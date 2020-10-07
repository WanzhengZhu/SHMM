#!/bin/zsh

# parameter file
para_file='./tf-la.yaml'
java_dir='../hmm-java/'
python_dir='../python-mobility/'
jar_file=$java_dir'hmm-java.jar'

# --------------------------------------------------------------------------------
# Step 1: preprocessing.
# --------------------------------------------------------------------------------

function pre {
  python $python_dir'preprocess.py' $para_file
}

# --------------------------------------------------------------------------------
# Step 2: run the algorithms.
# --------------------------------------------------------------------------------
function run {
  java -jar -Xmx10G $jar_file $para_file
}


# --------------------------------------------------------------------------------
# Step 3: post-processing
# --------------------------------------------------------------------------------

function post {
  python $python_dir'postprocess.py' $para_file
}

# pre
# run
post
