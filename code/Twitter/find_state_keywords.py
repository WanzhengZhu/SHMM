''' Find keywords for each state in the HMM (Twitter dataset)
# In order to use this function, you need to:
# 1. delete the ../../Results/Twitter-LA/Results_LA.txt
# 2. uncomment line92 WritetoFile in HMM.java to save the necessary information. (Don't forget to re-compile)
# 3. Re-run the SHMM model-training code.
'''
import numpy as np
import os

def read_file(filename):
    lines = []
    with open(filename) as file:
        for line in file:
            # line = " ".join(line.split()[1:])  # Get rid of the sentence ID
            lines.append(line)
    return lines

def read_wordEmb(filename):
    words = []
    wordEmb = []
    with open(filename) as file:
        for line in file:
            words.append(line.split()[0])  # Vocabulary
            wordEmb.append(map(float, line.split()[1:]))
    return (words,wordEmb)

def Normalize(senvec):
    norm = 0
    for x in senvec:
        norm += x**2
    senvec = [x/(norm**0.5) for x in senvec]
    return senvec

def get_mean(lines, state_num):
    Mean = []
    for j in range(state_num):
        state = lines[j]
        # print state
        temp = []
        for i in state.split():
            temp.append(float(i))
        Mean.append(temp)
    return Mean

def find_keywords(word, wordEmb, Kappa_index, Mean):
    for i in Kappa_index:
        sim = np.array([])
        # test = test/np.linalg.norm(test)
        for j in range(word.shape[0]):
            word[j] = word[j] / np.linalg.norm(word[j])  # Normalize the word vector
            # print np.linalg.norm(word[j])
            sim = np.append(sim, sum(Mean[i] * word[j]))
            # sim = np.append(sim, sum(test*word[j]))
        # print sim
        # index = np.argmax(sim)
        index = sim.argsort()[-20:][::-1]
        # print index
        sim_word = []
        for h in range(10):
            # print sim[index[h]]
            sim_word.append(wordEmb[index[h]])
            # print wordEmb[index[h]]
        print str(i+1) + ': ' + str(sim_word)

print 'Reading files...'
data = 'la'  # la or ny
if data == 'la':
    path = os.path.dirname(os.path.realpath(__file__)) + '/../../data/tf-la/'
    filename = os.path.dirname(os.path.realpath(__file__)) + '/../../Results/Twitter-LA/Results_LA.txt'
elif data == 'ny':
    path = os.path.dirname(os.path.realpath(__file__)) + '/../../data/tf-ny/'
    filename = os.path.dirname(os.path.realpath(__file__)) + '/../../Results/Twitter-NY/Results_NY.txt'

lines = read_file(filename)
del lines[0:5]
state_num = len(lines[1].split())
Kappa = []
for i in lines[1].split():
    Kappa.append(float(i))
lines = lines[state_num+3:2*state_num+3]
wordEmb = read_wordEmb(path + 'processed/WordEmb.txt')
senEmb = read_wordEmb(path + 'processed/sentence_vectors.txt')
sentence = read_file(path + 'processed/recon_tweet.txt')

print 'Getting Mean...'
Mean = get_mean(lines, state_num)
Mean = np.array(Mean)
Kappa = np.array(Kappa)
word = np.array(wordEmb[1])
senvec = np.array(senEmb[1])

print 'Sort Kappa...'
Kappa_index = Kappa.argsort()[-state_num:][::-1]
# Kappa_index = range(0, len(Kappa))
print Kappa[Kappa_index]
print 'Finding the closest keywords...'
find_keywords(word, wordEmb[0], Kappa_index, Mean)
# print Mean[0]
# print np.linalg.norm(Mean[0])

# print 'Finding the closest sentence...'
# find_keywords(senvec, state_num, sentence)
#
# for i in range(state_num):
#     sim = np.array([])
#     # test = test/np.linalg.norm(test)
#     for j in range(senvec.shape[0]):
#         # senvec[j] = senvec[j] / np.linalg.norm(senvec[j])  # Normalize the sentence vector
#         # print np.linalg.norm(senvec[j])
#         sim = np.append(sim, sum(Mean[i] * senvec[j]))
#         # sim = np.append(sim, sum(test*senvec[j]))
#
#     # index = np.argmax(sim)
#     index = sim.argsort()[-10:][::-1]
#     print index
#     for h in range(5):
#         # print sim[index[h]]
#         print sentence[index[h]]

print 'All Done...'
