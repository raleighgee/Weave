/*
 * CSVParser.h
 *
 *  Created on: May 12, 2014
 *      Author: sanjay
 */

#ifndef CSVPARSER_H_
#define CSVPARSER_H_

#include<string>
#include<vector>
#include<sstream>
#include "AS3/AS3.h"
#include "tracef.h"

using namespace std;

class CSVParser {
	char quote;
	char delimiter;
	bool removeBlankLines;
	inline string* newToken(vector< vector<string*>* > &rows, bool newRow);
	void test();
private:
	inline void newRow();
	inline void saveToken(char* start, char* end, bool parse);
public:

	string parseCSVToken(string token);
	string* createCSVToken(string,bool);
	inline void parseCSV(char* , bool);


	CSVParser();
	~CSVParser();
};

#endif /* CSVPARSER_H_ */
