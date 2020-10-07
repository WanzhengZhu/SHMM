#!/bin/bash
path=$(dirname "$0")/../code/Synthetic-Data

for kappa in 80 # 500 400 300 200 100 70 50 30 10 5 1
do for dim in 30 # 300 200 150 100 70 50 30 10 5
do for K in 10 # 20 25 30
do for num in 10000
do
/Applications/MATLAB_R2016b.app/bin/matlab -nodesktop -nosplash -r "run('$path/generate_VMF_data($K, $dim, $kappa, $num)'); exit;"
echo "Finished matlab part (Synthetic data generation)..."
(cd $path/../gmove/ && java -Xmx16g -cp ./out/production/gmove:./* demo.Demo "../run/synthetic.yaml")
done done done done