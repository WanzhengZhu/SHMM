from embed import *
from sys import platform

# pd stands for parameter dictionary
pd = dict()
debug = 1
if platform != 'darwin':
	debug = 0

pd['voca_min'] = 0
pd['voca_max'] = 20000

pd['dim'] = 10 if debug else 400
pd['negative'] = 1
pd['alpha'] = 0.02
pd['epoch'] = 1 if debug else 100

pd['nt_list'] = ['w','l','t']
pd['predict_type'] = 'w'
pd['job_id'] = '-1'
pd['rand_seed'] = 1
pd['dataset'] = 'la'
pd['data_size'] = 10000 if debug else 1100000 
pd['test_size'] = 1000 if debug else 10000

pd['grid_len'] = 0.002 # used only in LGridClus
pd['kernel_nb_num'] = 1 # used for efficiency reason (requested by fast k-nearest-neighbor search)
pd['bandwidth_l'] = 0.005 # used only in LGridClus, should be of similar magnitude as grid_len
pd['bandwidth_t'] = 1000.0 # used only in TGridClus
pd['kernel_bandwidth_l'] = 0.01
pd['kernel_bandwidth_t'] = 1000.0

pd['lClus'] = LGridClus
# pd['lClus'] = LMeanshiftClus
pd['embed_algo'] = GraphEmbedLine
# pd['embed_algo'] = GraphEmbedNative
pd['second_order'] = 1
pd['use_context_vec'] = 1
pd['category_list'] = ['Food', 'Shop & Service', 'Travel & Transport',\
					'College & University', 'Nightlife Spot', 'Residence', 'Outdoors & Recreation',\
					'Arts & Entertainment', 'Professional & Other Places']



