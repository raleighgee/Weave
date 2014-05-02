#include "miniz.c"
#include <stdlib.h>
#include "AS3/AS3.h"

// https://obtw.wordpress.com/2013/04/03/making-bytearray-faster/

// http://bruce-lab.blogspot.com/2012/12/migrating-from-alchemy-to-flascc.html
// http://blog.debit.nl/2009/03/using-bytearrays-in-actionscript-and-alchemy/

// FlasCC AS3.h API Reference:  http://www.adobe.com/devnet-docs/flascc/docs/capidocs/as3.html

// http://www.adobe.com/devnet-docs/flascc/docs/apidocs/com/adobe/flascc/CModule.html
// http://www.adobe.com/devnet-docs/flascc/docs/Reference.html#section_swig
// http://stackoverflow.com/questions/14326828/how-to-pass-bytearray-to-c-code-using-flascc
// http://forums.adobe.com/message/4969630

// sample miniz code found here: https://github.com/drhelius/Gearboy/blob/master/src/Cartridge.cpp#L146

#define tracef(...) {\
	size_t __size = 256;\
	char __cstr[__size];\
	AS3_DeclareVar(__astr, String);\
	AS3_CopyCStringToVar(__astr, __cstr, snprintf(__cstr, __size, __VA_ARGS__));\
	AS3_Trace(__astr);\
}

/**
 * Returns a pointer to an mz_zip_archive structure.
 */
void openZip() __attribute__((used,
	annotate("as3sig:public function openZip(byteArray:ByteArray):uint"),
	annotate("as3package:weave.zip"),
	annotate("as3import:flash.utils.ByteArray")));
void openZip()
{
	char *byteArray_c;
	size_t byteArray_len;

	inline_as3("byteArray.position = 0;");
	inline_as3("%0 = byteArray.length;" : "=r"(byteArray_len));
	byteArray_c = (char *)malloc(byteArray_len);

	inline_as3("CModule.ram.position = %0;" : : "r"(byteArray_c));
	inline_as3("CModule.ram.writeBytes(byteArray);");

	mz_zip_archive *zip_archive = (mz_zip_archive*)malloc(sizeof(zip_archive));
	memset(zip_archive, 0, sizeof(mz_zip_archive));

	mz_bool status = mz_zip_reader_init_mem(zip_archive, (void*) byteArray_c, byteArray_len, 0);
	if (!status)
	{
		tracef("mz_zip_reader_init_mem() failed!");
		AS3_Return(0);
	}

	AS3_Return(zip_archive);
}

/**
 * Gets a list of files in a zip
 */
void listFiles() __attribute__((used,
	annotate("as3sig:public function listFiles(_zip_archive:uint):Array"),
	annotate("as3package:weave.zip")));
void listFiles()
{
	mz_zip_archive *zip_archive;
	AS3_GetScalarFromVar(zip_archive, _zip_archive);

	char str[MZ_ZIP_MAX_ARCHIVE_FILENAME_SIZE];
	AS3_DeclareVar(fileName, String);
	AS3_DeclareVar(result, Array);
	inline_as3("result = [];");
	int n = mz_zip_reader_get_num_files(zip_archive);
	for (unsigned int i = 0; i < n; i++)
	{
		AS3_CopyCStringToVar(fileName, str, mz_zip_reader_get_filename(zip_archive, i, str, MZ_ZIP_MAX_ARCHIVE_FILENAME_SIZE) - 1);
		inline_as3("result.push(fileName);");
	}

	AS3_ReturnAS3Var(result);
}

void readObject() __attribute__((used,
		annotate("as3sig:public function readObject(_zip_archive:uint, _fileName:String):Object"),
		annotate("as3package:weave.zip")));
void readObject()
{
	mz_zip_archive *zip_archive;
	AS3_GetScalarFromVar(zip_archive, _zip_archive);
	char *fileName;
	AS3_MallocString(fileName, _fileName);

	void* uncomp_file;
	size_t uncomp_size;

	uncomp_file = mz_zip_reader_extract_file_to_heap(zip_archive, fileName, &uncomp_size, MZ_ZIP_FLAG_CASE_SENSITIVE);
	AS3_DeclareVar(result, Object);
	inline_as3("CModule.ram.position = %0;" : : "r"(uncomp_file));
	inline_as3("try { result = CModule.ram.readObject(); } catch (e:Error) { }");
	free(uncomp_file);
	free(fileName);
	AS3_ReturnAS3Var(result);
}

void readFile() __attribute__((used,
	annotate("as3sig:public function readFile(_zip_archive:uint, _fileName:String):ByteArray"),
	annotate("as3package:weave.zip"),
	annotate("as3import:flash.utils.ByteArray")));
void readFile()
{
	mz_zip_archive *zip_archive;
	AS3_GetScalarFromVar(zip_archive, _zip_archive);
	char *fileName;
	AS3_MallocString(fileName, _fileName);

	void* uncomp_file;
	size_t uncomp_size;

	uncomp_file = mz_zip_reader_extract_file_to_heap(zip_archive, fileName, &uncomp_size, MZ_ZIP_FLAG_CASE_SENSITIVE);
	AS3_DeclareVar(byteArray, ByteArray);
	inline_as3("byteArray = new ByteArray();");
	inline_as3("CModule.ram.position = %0;" : : "r"(uncomp_file));
	inline_as3("CModule.ram.readBytes(byteArray, 0, %0);" : : "r"(uncomp_size));
	free(uncomp_file);
	free(fileName);
	AS3_ReturnAS3Var(byteArray);
}

void closeZip() __attribute__((used,
	annotate("as3sig:public function closeZip(_zip_archive:uint):Boolean"),
	annotate("as3package:weave.zip")));
void closeZip()
{
	mz_zip_archive *zip_archive;
	AS3_GetScalarFromVar(zip_archive, _zip_archive);

	free(zip_archive->m_pState->m_pMem);
	AS3_Return(mz_zip_reader_end(zip_archive));
}
