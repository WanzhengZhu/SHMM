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
//	myread(argv[1],argv[2],argv[3]);
	myread("/Users/wanzheng/Downloads/data/tweets.txt","../temp_results/geo.txt","../temp_results/tweet.txt");
//    myread("../temp_results/raw_tweet.txt","../temp_results/geo.txt","../temp_results/tweet.txt");
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
	int linenum=1;
	while(getline(ip,line)){
		int counter = 1;
		istringstream iss(line);
    	string value;
		string checkin_ID;
		string user_ID;
		string lat;
		string lng;
		int time;

		while(getline(iss,value,'\x01')){
			if (counter == 1)
				checkin_ID = value;
			else if (counter == 2)
				user_ID = value;
			else if (counter == 3)
				lat = value;
			else if (counter == 4)
				lng = value;
			else if (counter == 6)
				time = atoi(value.c_str())-460162801;

//        	if((counter % 7 != 0) && (counter % 8 != 0))
//        		op_geo << value << ",";

        	if (counter % 8 == 0){
                if ((time <= 6048000-1) || (time >= 7862400-1))
                    continue;
                for(int i=0;i<value.size();++i){
					if(value[i]<0)  continue;
					// emoji has negative char number
					else if(value[i] == '@') op_text << " at ";
					else if(value[i] == '#') op_text << " tag "; 
					else  op_text << value[i];
				}
//                std::string s_time = std::to_string(time);
                op_geo << linenum << "," << time << "," << user_ID << "," << lat << "," << lng << ",";
        	}
        	counter++;
    	}
        linenum++;
        if ((time <= 6048000-1) || (time >= 7862400-1))
            continue;
    	op_geo << '\n';
    	op_text << '\n';
	}
	ip.close();
	op_geo.close();
	op_text.close();
}