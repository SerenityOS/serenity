#pragma once

#include <sys/cdefs.h>
#include <stddef.h>

__BEGIN_DECLS

typedef void* iconv_t;

extern iconv_t iconv_open(const char* tocode, const char* fromcode);
extern size_t iconv(iconv_t, char** inbuf, size_t* inbytesleft, char** outbuf, size_t* outbytesleft);
extern int iconv_close(iconv_t);

__END_DECLS
