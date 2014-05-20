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

using namespace std;

class CSVParser {
	char quote;
	char delimiter;
	string* newToken(vector< vector<string*>* > &rows, bool newRow);
	void test();
public:

	string parseCSVToken(string token);
	string* createCSVToken(string,bool);
	vector<string> parseCSVRow(string, bool);
	vector< vector<string> > parseCSV(string, bool);

	CSVParser();
	~CSVParser();
};

#endif /* CSVPARSER_H_ */
