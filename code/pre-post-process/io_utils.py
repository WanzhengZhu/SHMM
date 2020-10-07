import pymongo as pm
import parameter

class IO:

    def __init__(self, para_file):
        self.init_para(para_file)
        self.init_db()
        self.init_files()

    def init_para(self, para_file):
        self.para = parameter.yaml_loader().load(para_file)
        # parameters for hubseek
        self.num_state_list = self.para['hmm']['numState']
        self.num_cluster_list = self.para['ehmm']['numCluster']
        self.init_method_list = self.para['ehmm']['initMethod']
        self.aug_thresh_list = self.para['augment']['threshold']
        self.aug_size_list = self.para['augment']['augmentedSize']
        self.aug_num_bin_list = self.para['augment']['numAxisBin']

        self.default_num_state = self.num_state_list[0]
        self.default_num_cluster = self.num_cluster_list[0]
        self.default_init_method = self.init_method_list[0]
        self.default_aug_thresh = self.aug_thresh_list[0]
        self.default_aug_size = self.aug_size_list[0]
        self.default_aug_num_bin =self.aug_num_bin_list[0]

        self.xmax = self.para['post']['box']['xmax']
        self.xmin = self.para['post']['box']['xmin']
        self.ymax = self.para['post']['box']['ymax']
        self.ymin = self.para['post']['box']['ymin']
        self.xcenter = self.para['post']['box']['xcenter']
        self.ycenter = self.para['post']['box']['ycenter']


    def init_db(self):
        conn = pm.MongoClient(self.para['mongo']['dns'], int(self.para['mongo']['port']))
        db = conn[self.para['mongo']['db']]
        self.raw_col = db[self.para['mongo']['raw']]
        self.seq_col = db[self.para['mongo']['sequences']]
        self.word_col = db[self.para['mongo']['words']]
        self.model_col = db[self.para['mongo']['models']]
        self.exp_col = db[self.para['mongo']['exps']]


    def init_files(self):
        # raw files
        self.raw_checkin_file = self.para['file']['raw']['checkins']
        self.raw_place_file = self.para['file']['raw']['places']
        self.stopword_file = self.para['file']['raw']['stopwords']
        # input files
        self.word_file = self.para['file']['input']['words']
        self.sequence_file = self.para['file']['input']['sequences']
        self.raw_tweet = self.para['file']['input']['raw_tweet']
        # description file
        self.hmm_description = self.para['post']['keyword']['hmm_description']
        # plot file, geo distributions
        self.hmm_geo = self.para['post']['plot']['hmm_geo']
        # plot file, state transitions
        self.hmm_state = self.para['post']['plot']['hmm_state']
        self.background = self.para['post']['plot']['background']
        # plot file for prediciton
        self.prediction_C = self.para['post']['plot']['C']
        self.prediction_K = self.para['post']['plot']['K']
        self.prediction_L = self.para['post']['plot']['L']
        self.prediction_delta = self.para['post']['plot']['delta']
        self.prediction_all = self.para['post']['plot']['all']
        self.time_C = self.para['post']['plot']['time_C']
        self.time_K = self.para['post']['plot']['time_K']


if __name__ == '__main__':
    io = IO('../run/la-tweet.yaml')
    print 'Number of seqs:', io.seq_col.count()
    print 'Number of words:', io.word_col.count()
    print 'Number of models:', io.model_col.count()
    print 'Number of exps:', io.exp_col.count()

    # print io.model_col.remove({'ehmm.K': 20,
    #                              'ehmm.C': 6,
    #                              'ehmm.Init': 'random',
    #                              'augment': True,
    #                              'augmentThreshold': 0.3,
    #                              'augmentSize': 20,
    #                              'numAxisBin': 10});

    # print io.model_col.find_one({'ehmm.K': 20,
    #                              'ehmm.C': 6,
    #                              'ehmm.Init': 'random',
    #                              'augment': True,
    #                              'augmentThreshold': 0.3,
    #                              'augmentSize': 20,
    #                              'numAxisBin': 10});

    print io.exp_col.find_one({'ehmm.K': 10,
                               'ehmm.Init': 'kmeans_k',
                               'K': 3,
                               'augment': True,
                               'augmentThreshold': 0.3,
                               'augmentSize': 100,
                               'numAxisBin': 10})['Accuracy'];


    print io.exp_col.find_one({'hmm.K': 10,
                               'K': 3,
                               'augment': True,
                               'augmentThreshold': 0.3,
                               'augmentSize': 100,
                               'numAxisBin': 10})['Accuracy'];

    print io.exp_col.find_one({'geohmm.K': 10,
                               'K': 3})['Accuracy'];

    # io.model_col.remove()
    # io.exp_col.remove()

    # io.model_col.remove()
    # io.exp_col.remove()
