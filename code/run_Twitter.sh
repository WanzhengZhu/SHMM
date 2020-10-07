#!/bin/bash
data=la  # Tweets from la or ny
dp=$(dirname "$0")/../data/tf-$data  # data path
cp=$(dirname "$0")/../code/Twitter/preprocess  # code path


function raw_to_train {
### Preprocess raw tweets. INPUT: raw tweets & background tweets; OUTPUT: processed training tweets for word embedding learning.
gcc $cp/myread.cpp -o $cp/myread -lm -pthread -O2 -Wall -funroll-loops -Wno-unused-result -lstdc++
$cp/myread $dp/raw/raw_tweet.txt $dp/geo.txt $dp/tweet.txt
java -XX:ParallelGCThreads=4 -Xmx500m -jar $cp/ark-tweet-nlp-0.3.2.jar "$@" --output-format conll $dp/tweet.txt > $dp/tokenized_tweet.txt
gcc $cp/reconstruct.cpp -o $cp/reconstruct -lm -pthread -O2 -Wall -funroll-loops -Wno-unused-result -lstdc++
$cp/reconstruct $dp/tokenized_tweet.txt $dp/recon_tweet.txt
cat $dp/recon_tweet.txt $dp/raw/background_tweet.txt > $dp/train.txt
rm $dp/tweet.txt $dp/tokenized_tweet.txt
}

function sen_to_vec {
### Train sentence embeddings. INPUT: processed training tweets (recon_tweet.txt + geo.txt). OUTPUT: sentence embedding.
gcc $cp/word2vec.c -o $cp/word2vec -lm -pthread -O2 -Wall -funroll-loops -Wno-unused-result
$cp/word2vec -train $dp/train.txt -output $dp/WordEmb.txt -min-count $1 -size $2 -window $3 -sample $4 -hs $5 -negative $6 -iter $7 -sentence-vectors $8 -cbow $9 -threads ${10}
##grep 'UserID_Clean_*' ./$dp/WordEmb.txt | sort -n -k 1.14 > ./$dp/sentence_vectors.txt
python $cp/tfidf_word2vec.py $dp
gcc $cp/combine.cpp -o $cp/combine -lm -pthread -O2 -Wall -funroll-loops -Wno-unused-result  -lstdc++
$cp/combine $dp/geo.txt $dp/sentence_vectors.txt $dp/input/final.txt
rm $dp/geo.txt $dp/train.txt
mv $dp/sentence_vectors.txt $dp/recon_tweet.txt $dp/WordEmb.txt $dp/processed/
}

function preprocess {
raw_to_train
sen_to_vec 10 30 5 1e-4 1 5 5 0 1 20
}

function model_training {
(cd $cp/../../gmove/ && java -Xmx16g -cp ./out/production/gmove:./* demo.Demo "../run/tf-$data.yaml")
}

preprocess
model_training
