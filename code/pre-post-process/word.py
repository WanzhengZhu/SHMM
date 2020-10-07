import re
from nltk.stem import WordNetLemmatizer
from nltk.corpus import stopwords


class Word:

    def __init__(self, stem, min_length):
        self.stem = stem  # stem or not
        self.min_length = min_length  # minimum length
        self.more_stopwords = set()  # additional stopwords

    '''
    Allow a user to specify more stopwords through a file;
    Each line in the file is a stop word.
    '''

    def load_stopwords(self, stopwords_file):
        with open(stopwords_file, 'r') as fin:
            for line in fin:
                stopword = line.strip()
                self.more_stopwords.add(stopword)

    '''
    Parse a string into a list of words.
    Perform
    '''

    def parse_words(self, input_string):
        words = []
        tokens = re.findall(r'[a-zA-Z]+', input_string)
        wnl = WordNetLemmatizer()
        for token in tokens:
            if len(token) < self.min_length:
                continue
            word = wnl.lemmatize(token.lower()) if self.stem else token.lower()
            if self._is_valid(word):
                words.append(word)
        return words

    def _is_valid(self, word):
        #  Check whether the word is too short
        if(len(word) < self.min_length):
            return False
        #  Check whether the word is a stop word
        if(word in stopwords.words('english') or word in self.more_stopwords):
            return False
        return True


if __name__ == '__main__':
    word_processor = Word(True, 3)
    s = 'hello, This is@ went octopi just a test for 12you!. Try it http://'
    words = word_processor.parse_words(s)
    print words
