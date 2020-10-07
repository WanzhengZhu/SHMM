from tweet_handler import Tweet, POI
import paras
from random import *
from time import time, ctime
from collections import defaultdict
import numpy as np
from sys import platform
import multiprocessing
from multiprocessing import *
from functools import partial
import cPickle as pickle

class IO:
    def __init__(self):
        self.root_dir = '/Users/keyangzhang/Documents/UIUC/Research/Embedding/embedding/data/'+paras.pd['dataset']+'/'\
        if platform == 'darwin' else '/shared/data/kzhang53/embedding/'+paras.pd['dataset']+'/'
        self.input_dir = self.root_dir+'input/'
        self.output_dir = self.root_dir+'output/'
        self.models_dir = self.root_dir+'models/'
        self.tweet_file = self.input_dir+'tweets'+str(paras.pd['data_size'])+'.txt'
        self.poi_file = self.input_dir+'pois.txt'
        self.case_study_dir = self.output_dir+'case_study/'

    def get_voca(self, tweets, voca_min=0, voca_max=20000):
        word2freq = defaultdict(int)
        for tweet in tweets:
            for word in tweet.words:
                word2freq[word] += 1
        word_and_freq = word2freq.items()
        word_and_freq.sort(reverse=True, key=lambda tup:tup[1])
        # print 'get_voca', len(tweets), len(word2freq)
        voca = set(zip(*word_and_freq[voca_min:voca_max])[0])
        if '' in voca:
            voca.remove('')
        return voca

    def read_pois(self):
        pois = []
        for line in open(self.poi_file):
            fields = line.strip().split(',')
            if len(fields)<5:
                continue
            poi_id, lat, lng, cat, name = fields[0], float(fields[1]), float(fields[2]), fields[3], ','.join(fields[4:]).lower() 
            pois.append(POI(poi_id, lat, lng, cat, name))
        return pois

    def read_tweets(self, file_path=None):
        tweets = [] 
        if paras.pd['dataset']=='4sq':
            for line in open('/shared/data/czhang82/clean/ny_checkins/checkins.txt'):
                tweet = Tweet()
                tweet.load_checkin(line.strip())
                tweets.append(tweet)
                if len(tweets)==paras.pd['data_size']:
                    break
        elif paras.pd['dataset']=='old_la':
            tweets = pickle.load(open(self.models_dir+'act_tweets_'+str(paras.pd['data_size'])+'.model','r'))
            for tweet in tweets:
                tweet.category = ''
        elif paras.pd['dataset']=='old_ny':
            for line in open("/shared/data/czhang82/clean/ny_tweets/tweets.txt"):
                tweet = Tweet()
                tweet.load_old_ny(line.strip())
                tweets.append(tweet)
        elif paras.pd['dataset'] in ['ny','la']:
            for line in open(file_path if file_path else self.tweet_file):
                tweet = Tweet()
                tweet.load_tweet(line.strip())
                tweets.append(tweet)
        else:
            print 'no such dataset!'
            exit(0)
        return tweets

if __name__ == '__main__':
    # IO().merge_phrases_into_tweets_fields_adhoc()
    IO().read_tweets()
