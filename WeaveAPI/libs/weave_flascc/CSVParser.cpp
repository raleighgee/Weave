/*
 * CSVParser.cpp
 *
 *  Created on: May 12, 2014
 *      Author: sanjay
 */
#include <iostream>
#include "CSVParser.h"

static char CR = '\r';
static char LF  = '\n';



CSVParser::CSVParser() {
	quote = '"';
	delimiter = ',';

}
/**
	 * This function will decode a CSV token, removing quotes and un-escaping quotes where applicable.
	 * If the String begins with a " then the characters up until the matching " will be parsed, replacing "" with ".
	 * For example, the String "ab,c""xyz" becomes ab,c"xyz
	 * @param token The CSV-encoded token to parse.
	 * @return The result of parsing the token.
	 */
string CSVParser::parseCSVToken(string token){
	string parsedToken = "";
	int tokenLength = token.length();
	if (token[0] == quote)
	{
		bool escaped = true;
		for (int i = 1; i <= tokenLength; i++)
		{
			char thisChar = token[i];
			char nextChar = (i < tokenLength - 1) ? token[i + 1] : 0;
			if (thisChar == quote && nextChar == quote) //append escaped quote
			{
				i += 1;
				parsedToken += quote;
			}
			else if (thisChar == quote && escaped)
			{
				escaped = false;
			}
			else
			{
				parsedToken += thisChar;
			}
		}
	}
	else
	{
		parsedToken = token;
	}
	return parsedToken;
}

// need to add String trim checking
// ported from AS
/**
	 * This function encodes a String as a CSV token, adding quotes around it and replacing " with "" if necessary.
	 * For example, the String ab,c"xyz becomes "ab,c""xyz"
	 * @param str The String to encode as a CSV token.
	 * @return The String encoded as a CSV token.
	 */
string* CSVParser::createCSVToken(string str, bool quoteEmptyStrings)
{
	string* token = new string;
	// determine if quotes are necessary
	if ( str.length() > 0
		&& str.find(quote) < 0
		&& str.find(delimiter) < 0
		&& str.find(LF) < 0
		&& str.find(CR) < 0)
	{
		*token = str;
		return token;
	}

	*token = *token + quote;
	for (unsigned int i = 0; i < str.length(); i++)
	{
		char currentChar = str[i];
		if (currentChar == quote)
			*token += quote + quote;
		else
			*token += currentChar;
	}
	*token = *token + quote;
	return token;
}

inline string*  CSVParser::newToken(vector< vector<string*>* > &rows, bool newRow)
{
	//tracef("%u , %u",rows ,newRow);
	if (newRow){
		vector<string* >* stringVector = new vector<string* >;
		//cout << "Created Vector address(new Token: " << stringVector << endl;
		rows.push_back(stringVector);
	}
	vector<string* >* lastElement = rows.back() ;
	//cout << "lastElement Address(new Token: " << rows.back() << endl;
	string* token = new string;
	//cout << "inner" << endl;
	//cout << lastElement->size() << endl;
	lastElement->push_back(token);
	//cout << lastElement->size() << endl;

	return token;
}



void CSVParser::test(){

}

void parseCSV2() __attribute__((used,
		annotate("as3sig:public function parseCSV2(myString:String, _parseTokens:Boolean):Array"),
		annotate("as3package:weave.utils"),
		annotate("as3import:flash.utils.ByteArray")));

void parseCSV2()
{

	char *str = NULL;
	AS3_MallocString(str, myString);
	//tracef(" ParseCSV2 : %u %u",str, strlen(str));


	bool parseTokens;
	// for bool,int, numbers
	AS3_GetScalarFromVar(parseTokens,_parseTokens);

	CSVParser parser;
	parser.parseCSV(str,parseTokens);
	free(str);
	// to make the Flash array and return it
	//size_t
	inline_as3(
			"var resultArray:Array = new Array();"

		);
}

inline void saveToken(char* start, char* end){
	tracef("saveToken : %u %u", start, end - start);
}

void CSVParser::parseCSV(char* csvInput, bool parseTokens)
{
	//stringstream csvReader(csvInput);
	vector< vector<string* >* > rows;

	bool escaped = false;
	bool skipNext = false;
	//int next = csvReader.get();

	char* start = csvInput;
	char* end = csvInput;

	// special case -- if csv is empty, return an empty array (a set of zero rows)
	if (!*csvInput){
		//vector< vector<string> > empty(0);
		//return empty;
	}


	//string* token = newToken(rows, true); // new row
	char next = csvInput[0];
	//for (int charIndex = 0; charIndex < csvInput.length(); charIndex++){

	while (*csvInput++)
	{

		char chr = (char) next;
	  //  char chr =  next;
		next = *csvInput;

		if (skipNext)
		{
			skipNext = false;
			continue;
		}

		if (escaped)
		{
			if (chr == quote)
			{
				// append quote if not parsing tokens
				//if (!parseTokens)
					//*token += quote;
				if (next == quote) // escaped quote
				{
					// always append second quote
					//*token += quote;
					// skip second quote mark
					skipNext = true;
				}
				else // end of escaped text
				{
					escaped = false;
				}
			}
			//else
			//{
				//*token += chr;
			//}
		}
		else
		{
			if (chr == delimiter)
			{
				// start new token on same row
				//tracef(token->c_str());
				//token = newToken(rows, false);
			}
			else if (chr == quote && start == end)
			{
				// beginning of escaped token
				escaped = true;
				//if (!parseTokens)
					//*token += chr;
			}
			else if (chr == LF)
			{
				// start new token on new row
				//tracef(token->c_str());
				//token = newToken(rows, true);
			}
			else if (chr == CR)
			{
				// handle CRLF
				if (next == LF)
					skipNext = true; // skip line feed
				// start new token on new row
				//tracef(token->c_str());
				//token = newToken(rows, true);
			}
			//else //append single character to current token
				//*token += chr;
		}
	}

	// if there is more than one row and last row is empty,
	// remove last row assuming it is there because of a newline at the end of the file.
	/*while (rows.size() > 1)
	{
		vector<string* >* lastRow = rows.back();
		string*  lastElement = lastRow->back();

		if (lastRow->size() == 1 && lastElement->length() == 0)
			rows.pop_back(); // remove last row
		else
			break;
	}

	// find the maximum number of columns in a row
	int columnCount = 0;
	int size = rows.size();
	for (int rowIndex = 0 ; rowIndex < size; rowIndex++)
	{
		vector <string* >* row = rows.at(rowIndex);
		int rowSize = row->size();
		//cout << "rowSize" << endl;
		//cout << row->size() << endl;
		columnCount = max(columnCount, rowSize);
	}*/

	// create a 2D String array
/*	vector <vector<string> > result(size);
	for (int i = 0, j; i < size; i++)
	{
		vector<string* >* rowVector = rows.at(i);
		//rows[i] = ; // free the memory
		vector <string> rowArray(columnCount);
		// the row may not have all the columns
		int vectorSize =  rowVector->size();
		string* str;
		for (j = 0; j <vectorSize; j++)
		{
			str =  rowVector->at(j);
			//cout<< *str << endl;
			rowArray[j] =  *str;
		}
		//remove the allocated memory
		delete rowVector;
		delete str;
		// fill remaining columns with empty Strings
		for (; j < columnCount; j++)
			rowArray[j] = "";
		result[i] = rowArray;
	}

	return result;*/
}

CSVParser::~CSVParser() {
	// TODO Auto-generated destructor stub
}

