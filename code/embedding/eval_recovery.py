# import dill as pickle
import cPickle as pickle
from io_utils import IO
from embed import *
import time
import paras
import bisect
import os
import folium
from collections import defaultdict
import sys
import multiprocessing
from multiprocessing import *
from functools import partial

io = IO()

class QuantitativeEvaluator:
	def __init__(self, predict_type='w', fake_num=10):
		self.ranks = []
		self.predict_type = predict_type
		self.fake_num = fake_num
		if self.predict_type=='p':
			self.pois = io.read_pois()

	def get_ranks(self, tweets, predictor):
		noiseList = np.random.choice((self.pois if self.predict_type=='p' else tweets), self.fake_num*len(tweets)).tolist()
		for tweet in tweets:
			scores = []
			if self.predict_type=='p':
				score = predictor.predict(tweet.ts, tweet.poi_lat, tweet.poi_lng, tweet.words, tweet.category)
			else:
				score = predictor.predict(tweet.ts, tweet.lat, tweet.lng, tweet.words, tweet.category)
			scores.append(score)
			if self.predict_type=='c':
				for category in paras.pd['category_list']:
					if category!=tweet.category:
						noise_score = predictor.predict(tweet.ts, tweet.lat, tweet.lng, tweet.words, category)
						scores.append(noise_score)
			else:
				for i in range(self.fake_num):
					noise = noiseList.pop()
					if self.predict_type in ['l','p']:
						noise_score = predictor.predict(tweet.ts, noise.lat, noise.lng, tweet.words, tweet.category)
					elif self.predict_type=='t':
						noise_score = predictor.predict(noise.ts, tweet.lat, tweet.lng, tweet.words, tweet.category)
					elif self.predict_type=='w':
						noise_score = predictor.predict(tweet.ts, tweet.lat, tweet.lng, noise.words, tweet.category)
					scores.append(noise_score)
			scores.sort()
			# handle ties
			rank = len(scores)+1-(bisect.bisect_left(scores,score)+bisect.bisect_right(scores,score)+1)/2.0
			self.ranks.append(rank)

	def compute_mrr(self):
		ranks = self.ranks
		rranks = [1.0/rank for rank in ranks]
		mrr,mr = sum(rranks)/len(rranks),sum(ranks)/len(ranks)
		return round(mrr,4), round(mr,4)

def output_embed_vecs(predictor):
	directory = io.output_dir+'embed_vecs/'
	if not os.path.isdir(directory):
		os.makedirs(directory)
	for nt, vecs in predictor.nt2vecs.items():
		with open(directory+nt+'.txt', 'w') as f:
			for node, vec in vecs.items():
				if nt=='l':
					node = predictor.lClus.centroids[node]
				l = [str(e) for e in [node, list(vec)]]
				f.write('\x01'.join(l)+'\n')

def main(job_id=-1, params={}): # job_id is used for working with a bayesian optimization tool
	params = {k:v[0] if type(v)==list and len(v)==1 else v for k,v in params.items()}
	pd = dict(paras.pd)
	for para in params:
		pd[para] = params[para]
	pd['job_id'] = str(job_id)

	rand_seed = pd['rand_seed']
	np.random.seed(rand_seed)
	random.seed(rand_seed)
	
	start_time = time.time()
	tweets = io.read_tweets()
	random.shuffle(tweets)
	# tweets.sort(key=lambda tweet:tweet.ts)
	voca = io.get_voca(tweets, pd['voca_min'], pd['voca_max'])
	print 'reading done:', round(time.time()-start_time)
	predictor = EmbedPredictor(pd)
	predictor.fit(tweets[:-pd['test_size']], voca)
	evaluator = QuantitativeEvaluator(predict_type=pd['predict_type'])
	tweets_test = tweets[-pd['test_size']:]
	evaluator.get_ranks(tweets_test, predictor)

	mrr, mr = evaluator.compute_mrr()
	print 'time:', round(time.time()-start_time)
	print params 
	print 'test size:', len(evaluator.ranks)
	print 'predict:', evaluator.predict_type
	print 'mr:', mr
	print 'mrr:', mrr
	print 
	output_embed_vecs(predictor)
	return -mrr

def generate_ny_embedding(job_id=-1, params={}):
	pd = dict(paras.pd)
	for para in params:
		pd[para] = params[para]
	pd["job_id"] = str(job_id)

	rand_seed = pd["rand_seed"]
	np.random.seed(rand_seed)
	random.seed(rand_seed)
	
	start_time = time.time()
	tweets = io.read_tweets()
	import copy
	tweets_test = copy.deepcopy(tweets)
	random.shuffle(tweets)
	voca = io.get_voca(tweets, pd['voca_min'], pd['voca_max'])
	predictor = EmbedPredictor(pd)
	predictor.fit(tweets[:-pd['test_size']], voca)
	print "training time:", round(time.time()-start_time)
	with open('/shared/data/kzhang53/embedding/ny.txt', 'w') as f:
		for tweet in tweets_test:
			spatial_feature = predictor.gen_spatial_feature(tweet.lat, tweet.lng)
			temporal_feature = predictor.gen_temporal_feature(tweet.ts)
			textual_feature = predictor.gen_textual_feature(tweet.words)
			l = [str(e) for e in [tweet.id, list(spatial_feature), list(temporal_feature), list(textual_feature)]]
			f.write('\x01'.join(l)+'\n')


if __name__ == '__main__':
	sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)
	if len(sys.argv)==1:
		main(-1, {})
	if len(sys.argv)==2 and sys.argv[1]=='multiprocess':
		tasks = [{'epoch':epoch} for epoch in [50,100]]
		for i, task in enumerate(tasks):
			process = multiprocessing.Process(target=main,args=(i,task))
			process.start()
	if len(sys.argv)==2 and sys.argv[1]=='ny':
		generate_ny_embedding(-1, {})


