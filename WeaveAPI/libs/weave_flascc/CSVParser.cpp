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
	removeBlankLines = true;
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
		annotate("as3sig:public function parseCSV2(myString:String, _parseTokens:Boolean, output:Array):Array"),
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

	// initialize first row, empty
	inline_as3(
		"output.length = 0;"
		"var outputRow:Array = [];"
		"var outputCol:int = 0;"
		"output[0] = outputRow;"
	);

	CSVParser parser;
	parser.parseCSV(str,parseTokens);
	free(str);

	AS3_ReturnAS3Var(output);
}

// moves to the next row in the output
inline void CSVParser::newRow()
{
	// if skipping blank rows, don't create a new row if current row is blank
	inline_nonreentrant_as3(
		"if (!(%0 && outputCol == 1 && outputRow[0] == ''))"
		"    output.push(outputRow = new Array(outputRow.length));"
		"outputCol = 0;"
		: : "r"(removeBlankLines)
	);
}

// push new token onto output array
inline void CSVParser::saveToken(char* start, char* end, bool parse){
	//tracef("saveToken : %u %u", start, end - start);
	inline_as3(
		"ram_init.position = %0;"
		"var token:String = ram_init.readUTFBytes(%1);"
		: : "r"(start), "r"(end - start)
	);

	// if there are at least two characters and the token is surrounded with quotes, parse the token
	if (parse && end - start >= 2 && *start == quote && *(end - 1) == quote)
		inline_nonreentrant_as3("token = token.substr(1, token.length - 2).split('\"\"').join('\"');");

	// TODO: handle odd case for token like "Hel"lo where excel would change it into Hello

	inline_nonreentrant_as3("outputRow[outputCol++] = token;");
}

//TODO: option for removing blank lines (rows with one item, "")
inline void CSVParser::parseCSV(char* csvInput, bool parseTokens)
{
	// special case -- if csv is empty, do nothing
	if (*csvInput)
	{
		bool escaped = false;

		char* start = csvInput;

		char chr;
		char *next = csvInput;
		while (chr = *next++)
		{
			if (escaped)
			{
				if (chr == quote)
				{
					if (*next == quote) // escaped quote
					{
						// skip second quote mark
						next++;
					}
					else // end of escaped text
					{
						escaped = false;
					}
				}
			}
			else
			{
				if (chr == delimiter)
				{
					// end of current token
					saveToken(start, next - 1, parseTokens);
					start = next; // new token begins after delimiter
				}
				else if (chr == quote && csvInput == start) // quote at beginning of token
				{
					// beginning of escaped token
					escaped = true;
				}
				else if (chr == LF)
				{
					// end of current token and current row
					saveToken(start, next - 1, parseTokens);
					start = next; // new token begins after LF
					newRow();
				}
				else if (chr == CR)
				{
					// end of current token and current row
					saveToken(start, next - 1, parseTokens);

					// handle CRLF
					if (*next == LF)
						start = ++next; // new token begins after CRLF
					else
						start = next; // new token begins after CR
					newRow();
				}
			}
		}

		saveToken(start, next - 1, parseTokens);
	}

	// remove last line if it's blank (trailing newline character)
	inline_nonreentrant_as3("if (outputCol == 1 && outputRow[0] == '') output.pop();");
}

CSVParser::~CSVParser() {
	// TODO Auto-generated destructor stub
}

