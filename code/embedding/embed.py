import numpy as np
import random
from sklearn.cluster import MeanShift
from collections import defaultdict
import time, datetime
from time import time as cur_time
import itertools
import sys, os, ast
from subprocess import call, check_call
from scipy.special import expit
import math
from sklearn.neighbors import NearestNeighbors
from copy import deepcopy
from shutil import copyfile, rmtree
from sklearn.metrics.pairwise import cosine_similarity

def cosine(list1, list2):
	return cosine_similarity([list1],[list2])[0][0]

def convert_ts(ts):
	return (ts/3600)%24

class EmbedPredictor(object):
	def __init__(self, pd):
		self.pd = pd
		self.lClus = pd['lClus'](pd)
		self.nt2vecs = None
		self.start_time = cur_time()
		self.embed_algo = self.pd['embed_algo'](self.pd)

	def fit(self, tweets, voca):
		random.shuffle(tweets)
		locations = [[tweet.lat, tweet.lng] for tweet in tweets]
		self.lClus.fit(locations)
		nt2nodes, et2net = self.prepare_training_data(tweets, voca) # nt stands for "node type", and et stands for "edge type"
		self.nt2vecs = self.embed_algo.fit(nt2nodes, et2net)

	def prepare_training_data(self, tweets, voca):
		nt2nodes = {nt:set() for nt in self.pd['nt_list']}
		et2net = defaultdict(lambda : defaultdict(lambda : defaultdict(float)))
		for tweet in tweets:
			l = self.lClus.predict([tweet.lat, tweet.lng])
			t = convert_ts(tweet.ts)
			c = tweet.category
			words = [w for w in tweet.words if w in voca] # from text, only retain those words appearing in voca
			nts = self.pd['nt_list'][1:]
			if 'c' in nts and c not in self.pd['category_list']:
				nts.remove('c')
			for nt1 in nts:
				nt2nodes[nt1].add(eval(nt1))
				for nt2 in nts:
					if nt1!=nt2:
						et2net[nt1+nt2][eval(nt1)][eval(nt2)] += 1
			for w in words:
				nt1 = 'w'
				nt2nodes[nt1].add(eval(nt1))
				for nt2 in nts:
					et2net[nt1+nt2][eval(nt1)][eval(nt2)] += 1
					et2net[nt2+nt1][eval(nt2)][eval(nt1)] += 1
			for w1, w2 in itertools.combinations(words, r=2):
				if w1!=w2:
					et2net['ww'][w1][w2] += 1
					et2net['ww'][w2][w1] += 1
		for nt, clus in [('l',self.lClus)]:
		# for nt, clus in [('l',self.lClus), ('t',self.tClus)]:
			if type(clus) == MeanshiftClus:
				'''
				strangely, MeanshiftClus seems to produce some empty clusters, but we still have to 
				include them for encoding continuous proximity and for making predictions on test data
				'''
				for cluster in range(len(clus.centroids)):
					nt2nodes[nt].add(cluster)
			self.encode_continuous_proximity(nt, clus, et2net, nt2nodes)
		return nt2nodes, et2net

	def encode_continuous_proximity(self, nt, clus, et2net, nt2nodes):
		et = nt+nt
		if self.pd["kernel_nb_num"]>1:
			nodes = nt2nodes[nt]
			for n1 in nodes:
				center = clus.centroids[n1]
				for n2, proximity in clus.get_top_nbs(center):
					if n1!=n2:
						et2net[et][n1][n2] = proximity
						et2net[et][n2][n1] = proximity

	def gen_spatial_feature(self, lat, lng):
		nt2vecs = self.nt2vecs
		location = [lat, lng]
		if self.pd["kernel_nb_num"]>1: # do kernel smoothing
			l_vecs = [nt2vecs['l'][l]*weight for l, weight in self.lClus.get_top_nbs(location) if l in nt2vecs['l']]
			ls_vec = np.average(l_vecs, axis=0) if l_vecs else np.zeros(self.pd["dim"])
		else:
			l = self.lClus.predict(location)
			ls_vec = nt2vecs['l'][l] if l in nt2vecs['l'] else np.zeros(self.pd["dim"])
		return ls_vec

	def gen_temporal_feature(self, time):
		nt2vecs = self.nt2vecs
		t = convert_ts(time)
		ts_vec = nt2vecs['t'][t] if t in nt2vecs['t'] else np.zeros(self.pd['dim'])
		return ts_vec

	def gen_textual_feature(self, words):
		nt2vecs = self.nt2vecs
		w_vecs = [nt2vecs['w'][w] for w in words if w in nt2vecs['w']]
		ws_vec = np.average(w_vecs, axis=0) if w_vecs else np.zeros(self.pd['dim'])
		return ws_vec

	def gen_category_feature(self, c):
		nt2vecs = self.nt2vecs
		c_vec = nt2vecs['c'][c] if c in nt2vecs['c'] else np.zeros(self.pd['dim'])
		return c_vec

	def predict(self, time, lat, lng, words, category):
		# if 'c' not in self.pd['nt_list']:
		# 	words += category.lower().split()
		l_vec = self.gen_spatial_feature(lat, lng)
		t_vec = self.gen_temporal_feature(time)
		w_vec = self.gen_textual_feature(words)
		vecs = [l_vec, t_vec, w_vec]
		if 'c' in self.pd['nt_list']:
			c_vec = self.gen_category_feature(category)
			vecs.append(c_vec)
		score = sum([cosine(vec1, vec2) for vec1, vec2 in itertools.combinations(vecs, r=2)])
		return round(score, 6)

	def get_vec(self, query):
		nt2vecs = self.nt2vecs
		# use the "Python type" of the query to determine the "node type" of the query 
		if type(query)==str:
			if query in self.pd['category_list']:
				return nt2vecs['c'][query]
			else:
				return nt2vecs['w'][query.lower()]
		elif type(query)==list:
			return nt2vecs['l'][self.lClus.predict(query)]
		else:
			return nt2vecs['t'][query]

	def poi2vec(self, poi):
		l_vec = self.gen_spatial_feature(poi.lat, poi.lng)
		w_vec = self.gen_textual_feature(poi.name.lower())
		vecs = [l_vec, w_vec]
		if 'c' in self.pd['nt_list']:
			c_vec = self.gen_category_feature(poi.cat)
			vecs.append(c_vec)
		return np.average(vecs, axis=0)

	def get_nbs1(self, pois, query, nb_nt, neighbor_num=20):
		vec_query = self.get_vec(query)
		nb2vec = {poi:self.poi2vec(poi) for poi in pois} if nb_nt=='p' else self.nt2vecs[nb_nt]
		nbs = sorted(nb2vec, key=lambda nb:cosine(nb2vec[nb], vec_query), reverse=True)
		nbs = nbs[:neighbor_num]
		if nb_nt=='l':
			nbs = [self.lClus.centroids[nb] for nb in nbs]
		return nbs

	def get_nbs2(self, pois, query1, query2, func, nb_nt, neighbor_num=20):
		vec_query1 = self.get_vec(query1)
		vec_query2 = self.get_vec(query2)
		nb2vec = {poi:self.poi2vec(poi) for poi in pois} if nb_nt=='p' else self.nt2vecs[nb_nt]
		nbs = sorted(nb2vec, key=lambda nb:func(cosine(nb2vec[nb], vec_query1), cosine(nb2vec[nb], vec_query2)), reverse=True)
		nbs = nbs[:neighbor_num]
		if nb_nt=='l':
			nbs = [self.lClus.centroids[nb] for nb in nbs]
		return nbs

class Clus(object):
	def __init__(self, pd):
		self.nbrs = NearestNeighbors(n_neighbors=pd['kernel_nb_num'])
		self.centroids = None

	def fit(self, X):
		pass

	def predict(self, x):
		pass

	def get_top_nbs(self, x):
		[distances], [indices] = self.nbrs.kneighbors([x])
		return [(index, self.kernel(distance, self.kernel_bandwidth)) for index, distance in zip(indices, distances)]

	def kernel(self, u, h=1.0):
		u /= h
		return 0 if u>1 else math.e**(-u*u/2)


class LGridClus(Clus):
	def __init__(self, pd):
		super(LGridClus, self).__init__(pd)
		self.grid_len = pd['grid_len']
		self.kernel_bandwidth = pd['kernel_bandwidth_l']

	def fit(self, locations):
		centroids = []
		for lat, lng in locations:
			centroid_lat = round(lat - lat%self.grid_len + self.grid_len/2, 6)
			centroid_lng = round(lng - lng%self.grid_len + self.grid_len/2, 6)
			centroids.append((centroid_lat, centroid_lng))
		self.centroids = list(set(centroids))
		self.nbrs.fit(self.centroids)
		print 'location cluster num:', len(self.centroids)

	def predict(self, location):
		return self.nbrs.kneighbors([location])[1][0][0]


class LMeanshiftClus(object):
	def __new__(cls, pd):
		return MeanshiftClus(pd, pd["bandwidth_l"], pd["kernel_bandwidth_l"])

class TMeanshiftClus(object):
	def __new__(cls, pd):
		return MeanshiftClus(pd, pd["bandwidth_t"], pd["kernel_bandwidth_t"])

class MeanshiftClus(Clus):
	def __init__(self, pd, bandwidth, kernel_bandwidth):
		super(MeanshiftClus, self).__init__(pd)
		self.kernel_bandwidth = kernel_bandwidth
		self.ms = MeanShift(bandwidth=bandwidth, bin_seeding=True, n_jobs=5)

	def fit(self, X):
		X = np.array(X)
		self.ms.fit(X)
		self.centroids = self.ms.cluster_centers_
		self.nbrs.fit(self.centroids)
		print 'location cluster num:', len(self.centroids)

	def predict(self, x):
		return self.ms.predict([x])[0]


class GraphEmbedLine(object):
	def __init__(self, pd):
		self.pd = pd
		self.nt2vecs = dict()
		self.path_prefix = 'GraphEmbedLine/'
		self.path_suffix = '-'+self.pd['job_id']+'.txt'

	def fit(self, nt2nodes, et2net):
		self.write_line_input(nt2nodes, et2net)
		self.execute_line()
		self.read_line_output()
		return self.nt2vecs

	def write_line_input(self, nt2nodes, et2net):
		if 'c' not in nt2nodes: # add 'c' nodes (with no connected edges) to comply to Line's interface 
			nt2nodes['c'] = self.pd['category_list']
		for nt, nodes in nt2nodes.items():
			# print nt, len(nodes)
			node_file = open(self.path_prefix+'node-'+nt+self.path_suffix, 'w')
			for node in nodes:
				node_file.write(str(node)+'\n')
		all_et = [nt1+nt2 for nt1, nt2 in itertools.product(nt2nodes.keys(), repeat=2)]
		for et in all_et:
			edge_file = open(self.path_prefix+'edge-'+et+self.path_suffix, 'w')
			if et in et2net:
				for u, u_nb in et2net[et].items():
					for v, weight in u_nb.items():
						edge_file.write('\t'.join([str(u), str(v), str(weight), 'e'])+'\n')

	def execute_line(self):
		command = ['./hin2vec']
		command += ['-size', str(self.pd['dim'])]
		command += ['-negative', str(self.pd['negative'])]
		command += ['-alpha', str(self.pd['alpha'])]
		sample_num_in_million = max(1, self.pd['epoch']*self.pd['data_size']/1000000)
		command += ['-samples', str(sample_num_in_million)]
		command += ['-threads', str(10)]
		command += ['-second_order', str(self.pd['second_order'])]
		command += ['-job_id', str(self.pd['job_id'])]
		# call(command, cwd=self.path_prefix, stdout=open('stdout.txt','wb'))
		call(command, cwd=self.path_prefix)

	def read_line_output(self):
		for nt in self.pd['nt_list']:
			if nt!=self.pd['predict_type'] and self.pd['second_order'] and self.pd['use_context_vec']:
				vecs_path = self.path_prefix+'context-'+nt+self.path_suffix
			else:
				vecs_path = self.path_prefix+'output-'+nt+self.path_suffix
			vecs_file = open(vecs_path, 'r')
			vecs = dict()
			for line in vecs_file:
				node, vec_str = line.strip().split('\t')
				try:
					node = ast.literal_eval(node)
				except: # when nt is 'w', the type of node is string
					pass
				vecs[node] = np.array([float(i) for i in vec_str.split(' ')])
			self.nt2vecs[nt] = vecs
		for f in os.listdir(self.path_prefix): # clean up the tmp files created by this execution
		    if f.endswith(self.path_suffix):
		        os.remove(self.path_prefix+f)


class GraphEmbedNative(object):
	def __init__(self, pd):
		self.pd = pd
		self.nt2vecs = None
		self.nt2cvecs = None
		self.start_time = cur_time()

	def fit(self, nt2nodes, et2net):
		pd = self.pd
		sample_size = pd['data_size']*pd['epoch']
		# initialization not specified in the paper, got wrong at the beginning, corrected after reading the C source code
		self.nt2vecs = {nt:{node:(np.random.rand(pd['dim'])-0.5)/pd['dim'] for node in nt2nodes[nt]} for nt in nt2nodes}
		self.nt2cvecs = {nt:{node:(np.random.rand(pd['dim'])-0.5)/pd['dim'] for node in nt2nodes[nt]} for nt in nt2nodes}
		et2optimizer = {et:self.Optimizer(et2net[et], pd, sample_size) for et in et2net}
		alpha = pd['alpha']
		for i in range(sample_size):
			if i%1000==0:
				alpha = pd['alpha'] * (1 - float(i) / sample_size)
				if alpha < pd['alpha']*0.0001:
					alpha = pd['alpha']*0.0001
			for et in et2net:
				tu, tv = et[0], et[1]
				vecs_u, vecs_v = self.nt2vecs[tu], self.nt2vecs[tv]
				if pd['second_order']:
					vecs_v = self.nt2cvecs[tv]
				et2optimizer[et].sgd_one_step(vecs_u, vecs_v, alpha)
		nt2vecs = dict()
		for nt in nt2nodes:
			if nt!=pd["predict_type"] and pd["second_order"] and pd["use_context_vec"]:
				nt2vecs[nt] = self.nt2cvecs[nt]
			else:
				nt2vecs[nt] = self.nt2vecs[nt]
		return nt2vecs

	class Optimizer(object):
		def __init__(self, net, pd, sample_size):
			self.pd = pd
			u2d = defaultdict(float)
			v2d = defaultdict(float)
			for u in net:
				for v in net[u]:
					u2d[u] += net[u][v]
					v2d[v] += net[u][v]
			self.net = net
			self.u2samples = {u:iter(np.random.choice( net[u].keys(), 100, 
								p=self.normed(net[u].values()) )) for u in net}
			self.nega_samples = iter(np.random.choice( v2d.keys(), int(sample_size)*self.pd['negative'], 
								p=self.normed(np.power(v2d.values(), 0.75)) ))
			self.samples = iter(np.random.choice( u2d.keys(), sample_size, 
								p=self.normed(u2d.values()) ))
		
		def sgd_one_step(self, vecs_u, vecs_v, alpha):
			u = self.samples.next()
			try:
				v = self.u2samples[u].next()
			except StopIteration:
				self.u2samples[u] = iter(np.random.choice(self.net[u].keys(), 100, 
										p=self.normed(self.net[u].values()) ))
				v = self.u2samples[u].next()
			error_vec = np.zeros(self.pd['dim'])
			for j in range(self.pd['negative']+1):
				if j==0:
					target = v
					label = 1
				else:
					target = self.nega_samples.next()
					label = 0
				f = np.dot(vecs_u[u], vecs_v[target])
				g = (label - expit(f)) * alpha
				error_vec += g*vecs_v[target]
				vecs_v[target] += g*vecs_u[u]
			vecs_u[u] += error_vec
	
		def normed(self, x):
			return x/np.linalg.norm(x, ord=1)