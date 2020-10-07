Run code: run_Synthetic.sh 
This will generate new synthetic data and run HMM+VMF on that. If you don’t want to generate new synthetic data, you can comment the ‘run Matlab part’ (Line 9) in run_synthetic.sh. 


* Need to change the synthetic.yaml file: info_option: [30,31]
// Synthetic: 30 is approximation; 31 is the exact solution by Newton's method.


* The algorithm VMF+HMM works well with Synthetic data for: low to high number of state (3-15), low to high HMM length (2-8), low to high dimension (3-100).
