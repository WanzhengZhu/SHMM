#include <fstream> 
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <sstream>

using namespace std;

void reconstruct(char* input_file,char* output_file);

int main(int argc, char *argv[])
{
	reconstruct(argv[1],argv[2]);
//	reconstruct("../temp_results/tokenized_tweet.txt", "../temp_results/recon_tweet.txt");
//  reconstruct("../../../../tf-ny/input/temp_results/tokenized_tweet.txt", "../../../../tf-ny/input/temp_results/recon_tweet.txt");
    return 0;
}

void reconstruct(char* input_file,char* output_file)
{
	ifstream input;
	ofstream output;
	int counter = 0;
	input.open(input_file);
	output.open(output_file);
	while(!input.eof()){
		string twitt;
		string line;
		getline(input,line);
		while(!line.empty()){
			istringstream iss(line);
			string token;
			char type;
			float confidence;
			iss >> token >> type >> confidence;
//			if(type != 'U' && type != ','){
//				twitt.append(token);
// 				twitt.push_back(' ');
//			}
			if(type != 'U' && type != ',' && type != '!') { // URL
				twitt.append(token);
				twitt.push_back(' ');
			}

			getline(input,line);
		}
//		if(!twitt.empty()){
            output << twitt <<'\n';
//			output << "UserID_Clean_" << to_string(counter) << " " << twitt <<'\n';
//			output << "*" << to_string(counter) << " " << twitt <<'\n';
			//twitt = "UserID_" + to_string(counter) + " " + twitt;
			//output << twitt << '\n';
			counter++;
//		}
		
	}
	input.close();
	output.close();
}