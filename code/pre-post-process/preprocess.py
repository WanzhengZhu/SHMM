import operator
import sys
from collections import Counter
import parameter, word, timestamp
from io_utils import IO
import re

class Preprocessor:

    def __init__(self, para_file):
        self.para = parameter.yaml_loader().load(para_file)
        self.io = IO(para_file)
        self.lat_pattern = re.compile(r',4\d\.\d+,')
        self.lng_pattern = re.compile(r',-7\d\.\d+,')
        self.parser = word.Word(stem=True, min_length=3)
        self.parser.load_stopwords(self.io.stopword_file)
        self.checkins = {}
        self.word_dict = {}
        self.sequences = []


    def process(self, filename):
        if self.para['preprocess']['source'] == 'db':
            self.load_checkins_db()
        else:
            # self.load_checkins_file()
            self.load_raw_file(filename)
        self.sort_checkins()
        self.extract_sequences()
        self.cnt_words()
        self.write_words()
        self.write_sequneces()
        self.write_raw()
        # self.write_background_tweet(filename)
        # self.write_words_to_db()
        # self.write_seqs_to_db()


    '''
    load checkins from the database
    '''
    def load_checkins_db(self):
        checkin_id = 0
        for tweet in self.io.raw_col.find():
            items = self.parse_checkin_db(tweet)
            if items is None:
                continue
            timestamp, user_id, lat, lng, message = items
            if user_id in self.checkins:
                self.checkins[user_id].append((checkin_id, timestamp, user_id, lat, lng, message))
            else:
                self.checkins[user_id] = [(checkin_id, timestamp, user_id, lat, lng, message)]
            checkin_id += 1
            if checkin_id % 10000 == 0:
                print '%d checkins' % checkin_id
        print 'Finished processing %d checkins' % len(self.checkins)


    def parse_checkin_db(self, tweet):
        t = timestamp.Timestamp(str(self.para['preprocess']['startTime']))
        ts = t.get_timestamp(tweet['created_at'], 'min')
        maxTime = t.get_timestamp(str(self.para['preprocess']['endTime']), 'min')
        if ts < 0 or ts > maxTime:
            return None
        user_id = tweet['user_id']
        # Note: for the ny40k data set, lng comes first.
        if type(tweet['location']) is list:
            lng, lat = tweet['location']
        else:
            lat, lng = [float(e) for e in tweet['location'].split(',')]
        message = self.parse_message(tweet['text'])
        if len(message) < 3:
            return None
        return ts, user_id, lat, lng, message


    '''
    load places from the raw data file
    '''
    def load_checkins_file(self):
        # load places
        self.places = {}
        self.load_places()
        # load checkins
        checkin_id = 0
        with open(self.io.raw_checkin_file, 'r') as fin:
            for line in fin:
                if self.has_valid_checkin(line) is False:
                    continue
                items = self.parse_checkin_file(line)
                if items is None:
                    continue
                timestamp, user_id, lat, lng, message = items
                if user_id in self.checkins:
                    self.checkins[user_id].append((checkin_id, timestamp, user_id, lat, lng, message))
                else:
                    self.checkins[user_id] = [(checkin_id, timestamp, user_id, lat, lng, message)]
                checkin_id += 1
                if checkin_id % 10000 == 0:
                    print '%d checkins' % checkin_id
        print 'Finished processing %d checkins' % len(self.checkins)


    def load_places(self):
        cnt = 0
        with open(self.io.raw_place_file, 'r') as fp_in:
            for line in fp_in:
                cnt += 1
                if cnt % 10000 == 0:
                    print '%d places' % cnt
                if self.has_valid_place(line) is False:
                    continue
                vid, lat, lng, name = self.parse_place(line)
                self.places[vid] = (lat, lng, name)
        print 'Finished loading %d places' % len(self.places)


    def has_valid_place(self, line):
        terms = line.strip().split(',')
        if len(terms) < 16:
            return False
        lat_obj = re.search(self.lat_pattern, line)
        lng_obj = re.search(self.lng_pattern, line)
        if lat_obj is None or lng_obj is None:
            return False
        return True


    def parse_place(self, line):
        vid = line.strip().split(',')[0].strip('"')
        lat = re.search(self.lat_pattern, line).group().strip(',')
        lng = re.search(self.lng_pattern, line).group().strip(',')
        name = set(self.parser.parse_words(line.strip().split(',')[2]))
        category = set(self.parser.parse_words(line.strip().split(',')[-7]))
        name.update(category)
        return vid, lat, lng, name


    #  To check whether this is a valid line that have time, uid, and vid
    def has_valid_checkin(self, line):
        terms = line.strip().split(',')
        if len(terms) < 8:
            return False
        time_pattern = re.compile(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}')
        uid_pattern = re.compile(r'[\d]+')
        vid_pattern = re.compile(r'[\d\w]{24}')
        time_obj = re.search(time_pattern, terms[1])
        uid_obj = re.search(uid_pattern, terms[-2])
        vid_obj = re.search(vid_pattern, terms[-1])
        vid = terms[-1].strip('"')  # venue id
        if time_obj and uid_obj and vid_obj and (vid in self.places):
            return True
        else:
            return False


    def parse_checkin_file(self, line):
        terms = line.strip().split(',')
        t = timestamp.Timestamp(str(self.para['preprocess']['startTime']))
        ts = t.get_timestamp(terms[1].strip('"'), 'min')
        max_ts =  t.get_timestamp(str(self.para['preprocess']['endTime']), 'min')
        if ts < 0 or ts > max_ts:
            return None
        user_id = terms[-2].strip('"')  # user id
        venue_id = terms[-1].strip('"')  # venue id
        lat, lng = self.places[venue_id][0:2]
        message = self.parse_message(terms[4].strip('"'))
        message.update(self.places[venue_id][-1])
        return ts, user_id, lat, lng, message


    def parse_message(self, s):
        strings = s.split('http')
        words = self.parser.parse_words(strings[0])
        # return set(words)
        return words

    # sort the checkins in the order of ascending time.
    def sort_checkins(self):
        for uid in self.checkins:
            self.checkins[uid].sort(key=operator.itemgetter(1))


    def load_raw_file(self, filename):
        checkin_id = 0
        t = timestamp.Timestamp(str(self.para['preprocess']['startTime']))
        max_ts = t.get_timestamp(str(self.para['preprocess']['endTime']), 'min')
        with open(filename, 'r') as fin:
            for line in fin:
                # items = self.parse_checkin_file(line)
                # if items is None:
                #     continue
                items = line.strip().split('\x01')
                time = (int(items[5])-460162801-6048000+864000)/60  # -460162801-6048000 means Oct 10, +864000 means Sep 30
                # time = t.get_timestamp(items[4].strip('"'), 'min')
                if time < 0 or time > max_ts:
                    continue
                user_id = int(items[1])
                lat = float(items[2])
                lng = float(items[3])
                message = items[6]
                raw_message = items[7]
                if user_id in self.checkins:
                    self.checkins[user_id].append((checkin_id, time, user_id, lat, lng, message, raw_message))
                else:
                    self.checkins[user_id] = [(checkin_id, time, user_id, lat, lng, message, raw_message)]
                checkin_id += 1
                if checkin_id % 10000 == 0:
                    print '%d checkins' % checkin_id
                    # break
        print 'Finished processing %d checkins' % len(self.checkins)

    '''
    Step 2: extract consecutive checkin sequences.
    '''

    def extract_sequences(self):
        min_gap = self.para['preprocess']['minGap']
        max_gap = self.para['preprocess']['maxGap']
        for uid in self.checkins:
            self.sequences.extend(self.extract_for_one_user(self.checkins[uid], min_gap, max_gap))


    # Process one user's checkins
    def extract_for_one_user(self, checkins, min_gap, max_gap):
        # print checkins
        sequences = []
        for i in xrange(len(checkins) - 1):
            gap = checkins[i+1][1] - checkins[i][1]
            if min_gap <= gap <= max_gap:
                sequences.append((checkins[i], checkins[i+1]))
        return sequences


    '''
    write words, and sequences
    '''

    # #  Load all the check-ins from noisy data file
    # def cnt_words(self):
    #     counter = Counter()
    #     for s in self.sequences:
    #         counter.update(set(s[0][-1]))  # add the message of the first checkin
    #         counter.update(set(s[1][-1]))  # add the message of the second checkin
    #     word_id = 0
    #     for w in counter:
    #         if counter[w] >= 15:
    #             self.word_dict[w] = word_id
    #             word_id += 1

    #  Load all the check-ins from noisy data file
    def cnt_words(self):
        counter = {}
        for s in self.sequences:
            for w in s[0][-2].split():
                if w not in counter:
                    counter[w] = set()
                counter[w].add(s[0][1])  # add user id for this word
            for w in s[1][-2].split():
                if w not in counter:
                    counter[w] = set()
                counter[w].add(s[1][1])  # add user id for this word
        word_id = 0
        for w in counter:
            if len(counter[w]) >= 5:
                self.word_dict[w] = word_id
                word_id += 1


    def write_words(self):
        with open(self.io.word_file, 'w') as fout:
            for w in self.word_dict:
                fout.write(str(self.word_dict[w]) + ',' + w +'\n')


    def write_sequneces(self):
        with open(self.io.sequence_file, 'w') as fout:
            for seq in self.sequences:
                ss = self.to_string(seq)
                if ss is not None:
                    fout.write(self.to_string(seq) + '\n')


    def to_string(self, seq):
        s = list(seq[0][:-2])
        words = [str(self.word_dict[w]) for w in seq[0][-2].split() if w in self.word_dict]
        if len(words) == 0:
            return None
        s.append(' '.join(words))
        s.extend(seq[1][:-2])
        # s.append(' '.join(seq[1][-1]))
        words = [str(self.word_dict[w]) for w in seq[1][-2].split() if w in self.word_dict]
        if len(words) == 0:
            return None
        s.append(' '.join(words))
        return ','.join([str(item) for item in s])


    def write_raw(self):
        with open(self.io.raw_tweet, 'w') as fout:
            for seq in self.sequences:
                # write raw message
                fout.write('\x01'.join([str(item) for item in seq[0][:-2]]))
                fout.write('\x01' + str(seq[0][-1]) + '\n')
                fout.write('\x01'.join([str(item) for item in seq[1][:-2]]))
                fout.write('\x01' + str(seq[1][-1]) + '\n')
                # write bag-of-word message
                # fout.write('\x01'.join([str(item) for item in seq[0][:-1]]) + '\n')
                # fout.write('\x01'.join([str(item) for item in seq[1][:-1]]) + '\n')


    def write_background_tweet(self, filename):
        if filename[-13:] == "la-tweets.txt":
            background_file = '../../data/tf-la/input/for_word2vec/temp_results/LA_background_tweet.txt'
        elif filename[-13:] == "ny-tweets.txt":
            background_file = '../../data/tf-ny/input/temp_results/NY_background_tweet.txt'

        with open(background_file, 'w') as fout:
            for uid in self.checkins:
                for i in self.checkins[uid]:
                    fout.write(i[6] + '\n')

    # '''
    # write words and sequences to mongo db.
    # '''
    #
    # def write_words_to_db(self):
    #     self.io.word_col.remove()
    #     insert_cnt, batch = 0, []
    #     for w in self.word_dict:
    #         word = {'id': self.word_dict[w], 'text': w}
    #         batch.append(word)
    #         insert_cnt += 1
    #         if insert_cnt % 1000 == 0:
    #             self.io.word_col.insert(batch)
    #             batch = []
    #             print 'Finished inserting', insert_cnt, 'word.'
    #     self.io.word_col.insert(batch)
    #     self.io.word_col.create_index("id")
    #
    #
    # def write_seqs_to_db(self):
    #     self.io.seq_col.remove()
    #     insert_cnt, batch = 0, []
    #     for s in self.sequences:
    #         sd = self.to_dict(s)
    #         if len(sd['start']['message']) == 0 or len(sd['end']['message']) == 0:
    #             continue
    #         batch.append(sd)
    #         insert_cnt += 1
    #         if insert_cnt % 1000 == 0:
    #             self.io.seq_col.insert(batch)
    #             batch = []
    #             print 'Finished inserting %d sequences.' % insert_cnt
    #     self.io.seq_col.insert(batch)
    #
    #
    # def to_dict(self, seq):
    #     return {'start': self.checkin_to_dict(seq[0]), 'end': self.checkin_to_dict(seq[1])}
    #
    #
    # def checkin_to_dict(self, c):
    #     r = {}
    #     r['id'] = c[0]
    #     r['timestamp'] = c[1]
    #     r['uid'] = c[2]
    #     r['lat'] = c[3]
    #     r['lng'] = c[4]
    #     r['message'] = [self.word_dict[w] for w in c[-1] if w in self.word_dict]
    #     return r


if __name__ == '__main__':
    p = Preprocessor(sys.argv[1])
    p.process(sys.argv[2])
    # p = Preprocessor('../run/tf-ny.yaml')
    # p.process('/Users/wanzheng/Downloads/data/ny-tweets.txt')