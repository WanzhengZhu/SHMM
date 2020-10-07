#include <fstream> 
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <sstream>

using namespace std;

void combine(char* input_geo, char* input_emb, char* output_file);

int main(int argc, char *argv[])
{
	combine(argv[1],argv[2],argv[3]);
//	combine("../temp_results/geo.txt","../temp_results/sentence_vectors.txt","../temp_results/final.txt");
	return 0;
}

void combine(char* input_geo, char* input_emb, char* output_file)
{
	ifstream ip_geo, ip_emb;
	ofstream op;
	ip_emb.open(input_emb);
	string line;
	string title = "UserID_Clean_";
	map<int,string> dict;
	while(getline(ip_emb,line)){
		istringstream iss(line);
		string userid, index;
		iss >> userid;
		index = userid.substr(title.size());
		line.pop_back();
		dict[stoi(index)] = line.substr(userid.size()+1);
	}
	ip_emb.close();

	ip_geo.open(input_geo);
	op.open(output_file);
	int counter = 0;
	while(getline(ip_geo,line)){
		op << line << dict[counter];
		if(counter % 2 == 0)  
			op << ',';
		else
			op << '\n';
		counter++;
	}
	ip_geo.close();
	op.close();
	/*ip_dict.open(input_dict);
	string line;
	map<int,string> dict;
	while(getline(ip_dict,line)){
		istringstream iss(line);
    	string index, word;
    	getline(iss,index,',');
    	getline(iss,word);
    	dict[stoi(index)] = word;
	}
	ip_dict.close();
	
	ip_file.open(input_file);
	op.open(output_file);
	int counter = 0;
	while(getline(ip_file,line)){
		istringstream iss(line);
		string index;
		op<< "UserID_Clean_" << to_string(counter) << " ";
		while(iss >> index){
			if(dict.find(stoi(index))==dict.end()){
				cout << "not found " << index << endl;
				continue;
			}
			op<< dict[stoi(index)] << " ";
		}
		op << '\n';
		counter++;
	}
	ip_file.close();
	op.close();*/
}