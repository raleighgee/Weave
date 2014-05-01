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

// Mark the functions declaration with a GCC attribute specifying the
// AS3 signature we want it to have in the generated SWC.

void mallocTest_AS3() __attribute__((used,
	annotate("as3sig:public function mallocTest_AS3(_size:uint):uint"),
	annotate("as3package:weave.zip")));
void mallocTest_AS3()
{
	//copy the AS3 params to C variables
	unsigned int size; AS3_GetScalarFromVar(size, _size);

	int* result;

	//get the pointer of the texture buffer
	result = (int*)malloc( size );
	// return the result (using an AS3 return rather than a C/C++ return)
	AS3_Return(result);
}

#define tracef(...) {\
	unsigned int __size = 256;\
	char __cstr[__size];\
    AS3_DeclareVar(__astr, String);\
    AS3_CopyCStringToVar(__astr, __cstr, snprintf(__cstr, __size, __VA_ARGS__));\
    AS3_Trace(__astr);\
}

void readZip() __attribute__((used,
	annotate("as3sig:public function readZip(byteArray:ByteArray):Boolean"),
	annotate("as3package:weave.zip"),
	annotate("as3import:flash.utils.ByteArray")));
// sample code found here: https://github.com/drhelius/Gearboy/blob/master/src/Cartridge.cpp#L146
void readZip()
{
	char *byteArray_c;
	unsigned int byteArray_len;

	inline_as3("%0 = byteData.length;" : "=r"(byteArray_len));
	byteArray_c = (char *)malloc(byteArray_len);

	inline_as3("CModule.ram.position = %0;" : : "r"(byteArray_c));
	inline_as3("CModule.ram.writeBytes(byteData);");

	//using namespace std;

    mz_zip_archive zip_archive;
    mz_bool status;
    memset(&zip_archive, 0, sizeof (zip_archive));

    status = mz_zip_reader_init_mem(&zip_archive, (void*) byteArray_c, byteArray_len, 0);
    if (!status)
    {
    	tracef(
    		"debug %u,%u,%u,%u,%u,%u,%u",
    		byteArray_c,
    		byteArray_len,
    		zip_archive.m_archive_size,
    		zip_archive.m_central_directory_file_ofs,
    		zip_archive.m_total_files,
    		zip_archive.m_zip_mode,
    		zip_archive.m_file_offset_alignment
    	);

    	tracef("mz_zip_reader_init_mem() failed!");
        AS3_Return(false);
    }

    for (unsigned int i = 0; i < mz_zip_reader_get_num_files(&zip_archive); i++)
    {
        mz_zip_archive_file_stat file_stat;
        if (!mz_zip_reader_file_stat(&zip_archive, i, &file_stat))
        {
        	tracef("mz_zip_reader_file_stat() failed!");
            mz_zip_reader_end(&zip_archive);
            AS3_Return(false);
        }

        tracef("ZIP Content - Filename: \"%s\", Comment: \"%s\", Uncompressed size: %u, Compressed size: %u", file_stat.m_filename, file_stat.m_comment, (unsigned int) file_stat.m_uncomp_size, (unsigned int) file_stat.m_comp_size);

        const char* fn = file_stat.m_filename;

        void* uncomp_file;
		size_t uncomp_size;

		uncomp_file = mz_zip_reader_extract_file_to_heap(&zip_archive, file_stat.m_filename, &uncomp_size, 0);
		if (!uncomp_file)
		{
			tracef("mz_zip_reader_extract_file_to_heap() failed!");
			mz_zip_reader_end(&zip_archive);
	        AS3_Return(false);
		}

		free(uncomp_file);
    }

	mz_zip_reader_end(&zip_archive);
	tracef("success");

    AS3_Return(true);
}
