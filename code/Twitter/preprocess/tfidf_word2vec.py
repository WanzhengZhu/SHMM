import numpy as np
from sklearn.feature_extraction.text import *
import sys

'''
Generate sentence embeddings for each tweet. word2vec+tfidf.
'''

def read_file(filename):
    lines = []
    with open(filename) as file:
        for line in file:
            line = " ".join(line.split())  # Get rid of the sentence ID
            lines.append(line)
    return lines

def read_wordEmb(filename):
    words = []
    wordEmb = []
    with open(filename) as file:
        for line in file:
            words.append(line.split()[0])  # Vocabulary
            wordEmb.append(map(float, line.split()[1:])) # word vectors
    return (words,wordEmb)

def Normalize(senvec):
    norm = 0
    for x in senvec:
        norm += x**2
    senvec = [x/(norm**0.5) for x in senvec]
    return senvec

print 'Reading files...'
path = sys.argv[1]
filename = path + '/recon_tweet.txt'
lines = read_file(filename)
wordEmb = read_wordEmb(path + '/WordEmb.txt')

print 'Initializing Tfidf...'
vectorizer = TfidfVectorizer(min_df=1)
A = vectorizer.fit_transform(lines)
tfidf = A.toarray()

print 'Training sentence2vec vectors and Writing to file...'
f = open(path + '/sentence_vectors.txt', 'w')
for num in range(0, lines.__len__()):
    #print lines[num]
    senvec = [1e-50] * len(wordEmb[1][0])
    f.write("UserID_Clean_%d " % num)
    for word in lines[num].split():  # It should be unique values: 'for word in set(lines[num].split()):'
        if vectorizer.vocabulary_.get(word.lower()) is None:
            continue
        factor = tfidf[num][vectorizer.vocabulary_.get(word.lower())]
        try:
            l = wordEmb[1][wordEmb[0].index(word)]  # word2vec vectors
            l = [x * factor for x in l]  # weighted word2vec vectors
            senvec = map(sum, zip(l, senvec))  # sen2vec vectors
        except Exception:  # The word couldn't been found.
            #print num
            pass
    senvec = Normalize(senvec)
    for item in senvec:
        f.write("%s " % item)
    f.write("\n")
f.close()

print 'All Done...'
