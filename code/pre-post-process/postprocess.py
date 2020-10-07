import sys
import numpy as np
import numpy.random as nprd
import matplotlib.pyplot as plt
import parameter
from io_utils import IO
from matplotlib.backends.backend_pdf import PdfPages
import seaborn as sns
import operator
import folium
from sklearn.preprocessing import normalize


class Postprocessor:

    def __init__(self, para_file):
        self.para = parameter.yaml_loader().load(para_file)
        self.io = IO(para_file)
        self.set_plot_properties()

    def set_plot_properties(self):
        sns.set(font_scale=1.75, \
                palette="deep", \
                style='ticks', \
                rc = {'font.family': 'sans-serif', 'font.serif':'Helvetica', \
                      'legend.fontsize':24,
                      'font.size':10, 'pdf.fonttype': 42})

    def plot_models(self):
        hmms = self.io.model_col.find_one({'ehmm.K': 20,
                                           'ehmm.C': 6,
                                           'ehmm.Init': 'random',
                                           'augment': True,
                                           'augmentThreshold': 0.3,
                                           'augmentSize': 20,
                                           'numAxisBin': 10})['ehmm']['hmms'];
        for i, hmm in enumerate(hmms):
            geo_file = self.io.background + str(i) + '.html'
            transition_file  = self.io.hmm_state + str(i) + '.pdf'
            self.plot_hmm(hmm, geo_file, transition_file)


    def plot_hmm(self, hmm, state_file, transition_file):
        # load the models from the database
        self.plot_states(hmm, state_file, True)
        self.plot_transition(np.array(hmm['A']), transition_file)


    # is_gmm is a bool to indicate whether is single gaussian or gmm
    def plot_states(self, model, out_file, is_gmm):
        descriptions = self.gen_model_description(model)
        centers = self.get_state_centers(model)
        m = folium.Map(location=[self.io.ycenter, self.io.xcenter], zoom_start=12, tiles='Stamen Toner')
        for c, d in zip(centers, descriptions):
            m.polygon_marker(location=(c[1], c[0]), fill_color='#132b5e', num_sides=4, radius=10, popup=d)
        m.create_map(path = out_file)  # self.io.background

        # geo_samples = self.gen_geo_samples(model, is_gmm)
        # pp = PdfPages(out_file)
        # for (g, t) in zip(geo_samples, descriptions):
        #     fig = self.plot_one_state(g, t)
        #     pp.savefig(fig)
        # pp.close()

    # get the geo-center for each state
    def get_state_centers(self, model):
        centers = []
        gmms = model['geoModel']
        for gmm in gmms:
            centers.append(gmm[0]['mean'])
        return centers

    # plot one state
    def plot_one_state(self, data, text):
        g = sns.JointGrid(data[0], data[1], xlim = [self.io.xmin, self.io.xmax], ylim = [self.io.ymin, self.io.ymax])
        g = g.plot_joint(sns.kdeplot)
        g.ax_marg_x.set_axis_off()
        g.ax_marg_y.set_axis_off()
        plt.text(self.io.xmin, self.io.ymin, text, fontsize = 10)
        return g.fig


    # plot the transitions for hmm and mixture
    def plot_transition(self, data, pdf_file):
        plt.figure()
        nb_state = data.shape[0]
        with sns.axes_style("white"):
            fig = plt.imshow(data, interpolation='nearest')
            fig.set_cmap('Accent')
            plt.xticks(np.linspace(0, nb_state - 1, nb_state, endpoint=True))
            plt.yticks(np.linspace(0, nb_state - 1, nb_state, endpoint=True))
            plt.colorbar()
            plt.savefig(pdf_file, bbox_inches='tight')


    '''
    generate text description for a model
    '''
    def gen_model_description(self, model):
        states = []
        pi = model['pi']
        text_model = model['textModel']
        temporal_model = model['temporalModel']
        for i in xrange(len(text_model)):
            one_state = 'State: %d  Prob: %.3f \nKeywords: \n' % (i, pi[i])
            one_state += self.gen_top_k_keywords(text_model[i])
            one_state += '\nTime:'
            one_state += str(temporal_model[i]['mean'][0] / 60) # + ' ' + str(temporal_model[i]['var'])
            states.append(one_state)
        return states


    def gen_top_k_keywords(self, text_model):
        # probabilities for the multinomial
        probs = text_model['prob']
        sort_list = []
        for i, p in enumerate(probs):
            sort_list.append((i, p))
        sort_list.sort( key = operator.itemgetter(1), reverse = True )
        topk = sort_list[:int(self.para['post']['keyword']['K'])]
        # convert word id to keywords using the database
        ret = []
        for id, prob in topk:
            word = self.io.word_col.find_one({'id':int(id)})['text']
            ret.append(word + ': %.3f' % prob)
        return '\n'.join(ret)



    '''
    generate the geographical sample points
    '''
    def gen_geo_samples(self, model, is_gmm):
        samples = []
        geo_model = model['geoModel']
        if not is_gmm:
            # generate sample points for each state
            for g in geo_model:
                s = nprd.multivariate_normal(g['mean'], g['var'], 100).T
                samples.append(s)
        else:
            c = model['c']
            for i in xrange(len(c)):
                s = self.sample_gmm(c[i], geo_model[i])
                samples.append(s)
        return samples


    # get samples from a gaussian mixture.
    def sample_gmm(self, weights, gmm):
        samples = []
        m = [g['mean'] for g in gmm]
        v = [g['var'] for g in gmm]
        for i in xrange(len(weights)):
            num = int(100 * weights[i])
            s = nprd.multivariate_normal(m[i], v[i], num).T
            samples.extend(s)
        return samples


    def plot_background_map(self):
        m = folium.Map(location=[self.io.ycenter, self.io.xcenter], zoom_start=12, tiles = 'Stamen Terrain')
        m.create_map(path = self.io.background)


    def plot_transition_selected(self):
        group_a_data = np.loadtxt(self.io.group_a_file)
        normed_matrix = normalize(group_a_data, axis=1, norm='l1')
        self.plot_transition(normed_matrix, self.io.group_a_pdf)
        group_b_data = np.loadtxt(self.io.group_b_file)
        normed_matrix = normalize(group_b_data, axis=1, norm='l1')
        self.plot_transition(normed_matrix, self.io.group_b_pdf)


if __name__ == '__main__':
    # p = Postprocessor(sys.argv[1])
    p = Postprocessor('../run/la-tweet.yaml')
    # p.plot_models()
    # p.plot_background_map()
    # p.plot_transition_selected()
