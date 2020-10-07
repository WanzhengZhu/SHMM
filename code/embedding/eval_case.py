import cPickle as pickle
from io_utils import IO
from embed import *
import paras
import bisect
import embed
import os
import folium
from collections import defaultdict
import sys
from sys import platform
import multiprocessing
from multiprocessing import *
from functools import partial
import ast, urllib, urllib2, time, chardet, random
import numpy as np
from dateutil import parser
import datetime

io = IO()

class QualitativeEvaluator:
	def __init__(self, predictor, predictor_name):
		self.predictor = predictor
		self.predictor_name = predictor_name
		self.pois = np.random.choice(io.read_pois(), 10000)

	def plot_locations_on_google_map(self, locations, output_path):
		request ='https://maps.googleapis.com/maps/api/staticmap?zoom=10&size=600x600&maptype=roadmap&'
		for lat, lng in locations:
			request += 'markers=color:red%7C' + '%f,%f&' % (lat, lng)
		if platform == 'darwin':
			proxy = urllib2.ProxyHandler({'https': '127.0.0.1:1087'}) # VPN via shadowsocks
		else:
			proxy = urllib2.ProxyHandler({})
		opener = urllib2.build_opener(proxy)
		response = opener.open(request).read()
		with open(output_path, 'wb') as f:
			f.write(response)
			f.close()
		time.sleep(3)

	def scribe(self, directory, ls, ps, ts, ws, show_ls):
		if not os.path.isdir(directory):
			os.makedirs(directory)
		for nbs, file_name in [(ps, 'pois.txt'), (ts, 'times.txt'), (ws, 'words.txt')]:
			output_file = open(directory+file_name,'w')
			for nb in nbs:
				output_file.write(str(nb)+'\n')
		if show_ls:
			self.plot_locations_on_google_map(ls[:10], directory+'locations.png')
		else:
			self.plot_locations_on_google_map(ls[:1], directory+'queried_location.png')

	def getNbs1(self, query):
		if type(query)==str and query not in paras.pd['category_list'] and query.lower() not in self.predictor.nt2vecs['w']:
			print query, 'not in voca'
			return
		directory = io.case_study_dir+str(query)+'/'+self.predictor_name+'/'
		ls, ps, ts, ws = [self.predictor.get_nbs1(self.pois, query, nt) for nt in ['l', 'p', 't', 'w']]
		self.scribe(directory, ls, ps, ts, ws, type(query)!=list)

	def getNbs2(self, query1, query2, func=lambda a, b:a+b):
		if type(query1)==str and query1 not in paras.pd['category_list'] and query1.lower() not in self.predictor.nt2vecs['w']:
			return
		directory = io.case_study_dir+str(query1)+'-'+str(query2)+'/'+self.predictor_name+'/'
		ls, ps, ts, ws = [self.predictor.get_nbs2(self.pois, query1, query2, func, nt)[0] for nt in ['l', 'p', 't', 'w']]
		self.scribe(directory, ls, ps, ts, ws, type(query1)!=list)


def count_local_month2word2freq(query):
	directory = io.case_study_dir+str(query)+'/'
	if not os.path.isdir(directory):
		os.makedirs(directory)
	output_file = open(directory+'month2word2freq.txt','w')
	lat, lng = query
	tweets = io.read_tweets()
	month2word2freq = defaultdict(lambda : defaultdict(int))
	for tweet in tweets:
		dist = np.linalg.norm(np.array([lat, lng])-np.array([tweet.lat, tweet.lng]))
		if dist<0.04:
			month = tweet.datetime.split()[1]
			for word in tweet.words:
				month2word2freq[month][word] += 1
	for month in ['Aug', 'Sep', 'Oct', 'Nov']:
		word2freq = month2word2freq[month]
		output_file.write(month+'\n')
		total_freq = sum(word2freq.values())
		for word in sorted(word2freq, key=word2freq.get, reverse=True)[:10]:
			output_file.write(word+'\t'+str(word2freq[word])+'\t'+str(word2freq[word]/float(total_freq))+'\n')
		output_file.write('\n')


if __name__ == '__main__':
	if len(sys.argv)==2 and sys.argv[1]=='month2word2freq':
		count_local_month2word2freq([34.043021,-118.2690243])
		count_local_month2word2freq([34.073851,-118.242147])
	if len(sys.argv)==2 and sys.argv[1]=='date2count':
		tweets = io.read_tweets()
		date2count = defaultdict(int)
		for tweet in tweets:
			date = parser.parse(tweet.datetime).date()
			date2count[date] += 1
		for date in sorted(date2count):
			print date, date2count[date]
	if len(sys.argv)==1 or len(sys.argv)==2 and sys.argv[1]=='periods':
		pd = dict(paras.pd)
		tweets = io.read_tweets()
		voca = io.get_voca(tweets)
		periods = [(datetime.datetime(2014, 8, 25), datetime.datetime(2014, 8, 30))]
		periods += [(datetime.datetime(2014, 9, 29), datetime.datetime(2014, 10, 3))]
		periods += [(datetime.datetime(2014, 11, 25), datetime.datetime(2014, 11, 30))]
		for start_date, end_date in periods:
			tweets_train = [tweet for tweet in tweets if start_date<=parser.parse(tweet.datetime).replace(tzinfo=None)<=end_date]
			print start_date, end_date, 'tweets num:', len(tweets_train)
			predictor = EmbedPredictor(pd)
			predictor.fit(tweets_train, voca)
			evaluator = QualitativeEvaluator(predictor, str(start_date.date())+' - '+str(end_date.date()))
			# for category in pd['category_list']:
			# 	evaluator.getNbs1(category)
			for word in ['food', 'restaurant', 'beach', 'weather', 'clothes', 'nba', 'basketball', 'thanksgiving', 'outdoor', 'staple', 'dodgers', 'stadium']:
				evaluator.getNbs1(word)
			# for location in [[34.043021,-118.2690243], [33.9424, -118.4137], [34.008, -118.4961], [34.0711, -118.4434], [34.1017, -118.3270], [34.073851,-118.242147], [34.1381168,-118.3555723]]:
			# 	evaluator.getNbs1(location)
			for location in [[40.6824952,-73.9772289], [40.7505045,-73.9956327], [40.8075355,-73.9647667], [40.8296426,-73.9283685], [40.8128397,-74.0764031]]:
				evaluator.getNbs1(location)
			# evaluator.getNbs2('outdoor', 'friend')
			# evaluator.getNbs2('outdoor', 'weekend')
			# evaluator.getNbs2('Outdoors & Recreation', 'friend')
			# evaluator.getNbs2('Outdoors & Recreation', 'weekend')

			