#include <fstream> 
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <sstream>

using namespace std;

void myread(char* input_file, char* geo, char* text);

int main(int argc, char *argv[])
{
	myread(argv[1],argv[2],argv[3]);
//	myread("../temp_results/raw_tweet_test.txt","../temp_results/geo.txt","../temp_results/tweet.txt");
	return 0;
}

void myread(char* input_file, char* geo, char* text)
{
	ifstream ip;
	ofstream op_geo, op_text;
	ip.open(input_file);
	op_geo.open(geo);
	op_text.open(text);
	string line;
	while(getline(ip,line)){
		int counter = 1;
		istringstream iss(line);
    	string value;
		while(getline(iss,value,'\x01')){
        	if(counter % 6 != 0)
        		op_geo << value << ",";
        	else{
//				cout << value << '\n';
				std::transform(value.begin(), value.end(), value.begin(), ::tolower); // To lower case
//				cout << value << '\n';
				for(int i=0;i<value.size();++i){
					char word = value[i];
					if(word<0)  continue;
					// emoji has negative char number
//					else if(value[i] == '@') op_text << " at ";
//					else if(value[i] == '#') op_text << " tag ";
					else if(word == '@') continue;
					else if(word == '#')
						continue;
                    else if(word == '\r')
                        continue;
                    else  op_text << word;
				}
        	}
        	counter++;
    	}
    	op_geo << '\n';
    	op_text << '\n';
	}
	ip.close();
	op_geo.close();
	op_text.close();
}