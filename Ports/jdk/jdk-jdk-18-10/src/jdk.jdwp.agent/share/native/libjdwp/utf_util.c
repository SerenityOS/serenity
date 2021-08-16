/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <stdlib.h>
#include <ctype.h>

#include "jni.h"

#include "utf_util.h"


/* Error and assert macros */
#define UTF_ERROR(m) utfError(__FILE__, __LINE__,  m)
#define UTF_ASSERT(x) ( (x)==0 ? UTF_ERROR("ASSERT ERROR " #x) : (void)0 )

// Platform independed part

static void utfError(char *file, int line, char *message) {
    (void)fprintf(stderr, "UTF ERROR [\"%s\":%d]: %s\n", file, line, message);
    abort();
}

/* Determine length of this Standard UTF-8 in Modified UTF-8.
 *    Validation is done of the basic UTF encoding rules, returns
 *    length (no change) when errors are detected in the UTF encoding.
 *
 *    Note: Accepts Modified UTF-8 also, no verification on the
 *          correctness of Standard UTF-8 is done. e,g, 0xC080 input is ok.
 */
int JNICALL utf8sToUtf8mLength(jbyte *string, int length) {
  int newLength;
  int i;

  newLength = 0;
  for ( i = 0 ; i < length ; i++ ) {
    unsigned byte;

    byte = (unsigned char)string[i];
    if ( (byte & 0x80) == 0 ) { /* 1byte encoding */
      newLength++;
      if ( byte == 0 ) {
        newLength++; /* We gain one byte in length on NULL bytes */
      }
    } else if ( (byte & 0xE0) == 0xC0 ) { /* 2byte encoding */
      /* Check encoding of following bytes */
      if ( (i+1) >= length || (string[i+1] & 0xC0) != 0x80 ) {
        break; /* Error condition */
      }
      i++; /* Skip next byte */
      newLength += 2;
    } else if ( (byte & 0xF0) == 0xE0 ) { /* 3byte encoding */
      /* Check encoding of following bytes */
      if ( (i+2) >= length || (string[i+1] & 0xC0) != 0x80
        || (string[i+2] & 0xC0) != 0x80 ) {
        break; /* Error condition */
        }
        i += 2; /* Skip next two bytes */
        newLength += 3;
    } else if ( (byte & 0xF8) == 0xF0 ) { /* 4byte encoding */
      /* Check encoding of following bytes */
      if ( (i+3) >= length || (string[i+1] & 0xC0) != 0x80
        || (string[i+2] & 0xC0) != 0x80
        || (string[i+3] & 0xC0) != 0x80 ) {
        break; /* Error condition */
        }
        i += 3; /* Skip next 3 bytes */
        newLength += 6; /* 4byte encoding turns into 2 3byte ones */
    } else {
      break; /* Error condition */
    }
  }
  if ( i != length ) {
    /* Error in finding new length, return old length so no conversion */
    /* FIXUP: ERROR_MESSAGE? */
    return length;
  }
  return newLength;
}

/* Convert Standard UTF-8 to Modified UTF-8.
 *    Assumes the UTF-8 encoding was validated by utf8mLength() above.
 *
 *    Note: Accepts Modified UTF-8 also, no verification on the
 *          correctness of Standard UTF-8 is done. e,g, 0xC080 input is ok.
 */
void JNICALL utf8sToUtf8m(jbyte *string, int length, jbyte *newString, int newLength) {
    int i;
    int j;

    j = 0;
    for ( i = 0 ; i < length ; i++ ) {
        unsigned byte1;

        byte1 = (unsigned char)string[i];

        /* NULL bytes and bytes starting with 11110xxx are special */
        if ( (byte1 & 0x80) == 0 ) { /* 1byte encoding */
            if ( byte1 == 0 ) {
                /* Bits out: 11000000 10000000 */
                newString[j++] = (jbyte)0xC0;
                newString[j++] = (jbyte)0x80;
            } else {
                /* Single byte */
                newString[j++] = byte1;
            }
        } else if ( (byte1 & 0xE0) == 0xC0 ) { /* 2byte encoding */
            newString[j++] = byte1;
            newString[j++] = string[++i];
        } else if ( (byte1 & 0xF0) == 0xE0 ) { /* 3byte encoding */
            newString[j++] = byte1;
            newString[j++] = string[++i];
            newString[j++] = string[++i];
        } else if ( (byte1 & 0xF8) == 0xF0 ) { /* 4byte encoding */
            /* Beginning of 4byte encoding, turn into 2 3byte encodings */
            unsigned byte2, byte3, byte4, u21;

            /* Bits in: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx */
            byte2 = (unsigned char)string[++i];
            byte3 = (unsigned char)string[++i];
            byte4 = (unsigned char)string[++i];
            /* Reconstruct full 21bit value */
            u21  = (byte1 & 0x07) << 18;
            u21 += (byte2 & 0x3F) << 12;
            u21 += (byte3 & 0x3F) << 6;
            u21 += (byte4 & 0x3F);
            /* Bits out: 11101101 1010xxxx 10xxxxxx */
            newString[j++] = (jbyte)0xED;
            newString[j++] = (jbyte)(0xA0 + (((u21 >> 16) - 1) & 0x0F));
            newString[j++] = (jbyte)(0x80 + ((u21 >> 10) & 0x3F));
            /* Bits out: 11101101 1011xxxx 10xxxxxx */
            newString[j++] = (jbyte)0xED;
            newString[j++] = (jbyte)(0xB0 + ((u21 >>  6) & 0x0F));
            newString[j++] = byte4;
        }
    }
    UTF_ASSERT(i==length);
    UTF_ASSERT(j==newLength);
    newString[j] = (jbyte)0;
}

/* Given a Modified UTF-8 string, calculate the Standard UTF-8 length.
 *   Basic validation of the UTF encoding rules is done, and length is
 *   returned (no change) when errors are detected.
 *
 *   Note: No validation is made that this is indeed Modified UTF-8 coming in.
 *
 */
int JNICALL utf8mToUtf8sLength(jbyte *string, int length) {
    int newLength;
    int i;

    newLength = 0;
    for ( i = 0 ; i < length ; i++ ) {
        unsigned byte1, byte2, byte3, byte4, byte5, byte6;

        byte1 = (unsigned char)string[i];
        if ( (byte1 & 0x80) == 0 ) { /* 1byte encoding */
            newLength++;
        } else if ( (byte1 & 0xE0) == 0xC0 ) { /* 2byte encoding */
            /* Check encoding of following bytes */
            if ( (i+1) >= length || (string[i+1] & 0xC0) != 0x80 ) {
                break; /* Error condition */
            }
            byte2 = (unsigned char)string[++i];
            if ( byte1 != 0xC0 || byte2 != 0x80 ) {
                newLength += 2; /* Normal 2byte encoding, not 0xC080 */
            } else {
                newLength++;    /* We will turn 0xC080 into 0 */
            }
        } else if ( (byte1 & 0xF0) == 0xE0 ) { /* 3byte encoding */
            /* Check encoding of following bytes */
            if ( (i+2) >= length || (string[i+1] & 0xC0) != 0x80
                                 || (string[i+2] & 0xC0) != 0x80 ) {
                break; /* Error condition */
            }
            byte2 = (unsigned char)string[++i];
            byte3 = (unsigned char)string[++i];
            newLength += 3;
            /* Possible process a second 3byte encoding */
            if ( (i+3) < length && byte1 == 0xED && (byte2 & 0xF0) == 0xA0 ) {
                /* See if this is a pair of 3byte encodings */
                byte4 = (unsigned char)string[i+1];
                byte5 = (unsigned char)string[i+2];
                byte6 = (unsigned char)string[i+3];
                if ( byte4 == 0xED && (byte5 & 0xF0) == 0xB0 ) {
                    /* Check encoding of 3rd byte */
                    if ( (byte6 & 0xC0) != 0x80 ) {
                        break; /* Error condition */
                    }
                    newLength++; /* New string will have 4byte encoding */
                    i += 3;       /* Skip next 3 bytes */
                }
            }
        } else {
            break; /* Error condition */
        }
    }
    if ( i != length ) {
        /* Error in UTF encoding */
        /*  FIXUP: ERROR_MESSAGE()? */
        return length;
    }
    return newLength;
}

/* Convert a Modified UTF-8 string into a Standard UTF-8 string
 *   It is assumed that this string has been validated in terms of the
 *   basic UTF encoding rules by utf8Length() above.
 *
 *   Note: No validation is made that this is indeed Modified UTF-8 coming in.
 *
 */
void JNICALL utf8mToUtf8s(jbyte *string, int length, jbyte *newString, int newLength) {
    int i;
    int j;

    j = 0;
    for ( i = 0 ; i < length ; i++ ) {
        unsigned byte1, byte2, byte3, byte4, byte5, byte6;

        byte1 = (unsigned char)string[i];
        if ( (byte1 & 0x80) == 0 ) { /* 1byte encoding */
            /* Single byte */
            newString[j++] = byte1;
        } else if ( (byte1 & 0xE0) == 0xC0 ) { /* 2byte encoding */
            byte2 = (unsigned char)string[++i];
            if ( byte1 != 0xC0 || byte2 != 0x80 ) {
                newString[j++] = byte1;
                newString[j++] = byte2;
            } else {
                newString[j++] = 0;
            }
        } else if ( (byte1 & 0xF0) == 0xE0 ) { /* 3byte encoding */
            byte2 = (unsigned char)string[++i];
            byte3 = (unsigned char)string[++i];
            if ( i+3 < length && byte1 == 0xED && (byte2 & 0xF0) == 0xA0 ) {
                /* See if this is a pair of 3byte encodings */
                byte4 = (unsigned char)string[i+1];
                byte5 = (unsigned char)string[i+2];
                byte6 = (unsigned char)string[i+3];
                if ( byte4 == 0xED && (byte5 & 0xF0) == 0xB0 ) {
                    unsigned u21;

                    /* Bits in: 11101101 1010xxxx 10xxxxxx */
                    /* Bits in: 11101101 1011xxxx 10xxxxxx */
                    i += 3;

                    /* Reconstruct 21 bit code */
                    u21  = ((byte2 & 0x0F) + 1) << 16;
                    u21 += (byte3 & 0x3F) << 10;
                    u21 += (byte5 & 0x0F) << 6;
                    u21 += (byte6 & 0x3F);

                    /* Bits out: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx */

                    /* Convert to 4byte encoding */
                    newString[j++] = 0xF0 + ((u21 >> 18) & 0x07);
                    newString[j++] = 0x80 + ((u21 >> 12) & 0x3F);
                    newString[j++] = 0x80 + ((u21 >>  6) & 0x3F);
                    newString[j++] = 0x80 + (u21 & 0x3F);
                    continue;
                }
            }
            /* Normal 3byte encoding */
            newString[j++] = byte1;
            newString[j++] = byte2;
            newString[j++] = byte3;
        }
    }
    UTF_ASSERT(i==length);
    UTF_ASSERT(j==newLength);
    newString[j] = 0;
}

#ifdef _WIN32
// Microsoft Windows specific part

#include <windows.h>

static UINT getCodepage() {
    LANGID langID;
    LCID localeID;
    TCHAR strCodePage[7];       // ANSI code page id

    static UINT intCodePage = -1;

    if (intCodePage == -1) {
        // Firts call, get codepage from the os
        langID = LANGIDFROMLCID(GetUserDefaultLCID());
        localeID = MAKELCID(langID, SORT_DEFAULT);
        if (GetLocaleInfo(localeID, LOCALE_IDEFAULTANSICODEPAGE,
                         strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 ) {
            intCodePage = atoi(strCodePage);
        }
        else {
            intCodePage = GetACP();
        }
    }

    return intCodePage;
}

/*
 * Get wide string  (assumes len>0)
 */
static WCHAR* getWideString(UINT codePage, char* str, int len, int *pwlen) {
    int wlen;
    WCHAR* wstr;

    /* Convert the string to WIDE string */
    wlen = MultiByteToWideChar(codePage, 0, str, len, NULL, 0);
    *pwlen = wlen;
    if (wlen <= 0) {
        UTF_ERROR(("Can't get WIDE string length"));
        return NULL;
    }
    wstr = (WCHAR*)malloc(wlen * sizeof(WCHAR));
    if (wstr == NULL) {
        UTF_ERROR(("Can't malloc() any space"));
        return NULL;
    }
    if (MultiByteToWideChar(codePage, 0, str, len, wstr, wlen) == 0) {
        UTF_ERROR(("Can't get WIDE string"));
        return NULL;
    }
    return wstr;
}

/*
 * Convert UTF-8 to a platform string
 * NOTE: outputBufSize includes the space for the trailing 0.
 */
int JNICALL utf8ToPlatform(jbyte *utf8, int len, char* output, int outputBufSize) {
    int wlen;
    int plen;
    WCHAR* wstr;
    UINT codepage;
    int outputMaxLen;

    UTF_ASSERT(utf8);
    UTF_ASSERT(output);
    UTF_ASSERT(len >= 0);
    UTF_ASSERT(outputBufSize > len);
    outputMaxLen = outputBufSize - 1; // leave space for trailing 0

    /* Zero length is ok, but we don't need to do much */
    if ( len == 0 ) {
        output[0] = 0;
        return 0;
    }

    /* Get WIDE string version (assumes len>0) */
    wstr = getWideString(CP_UTF8, (char*)utf8, len, &wlen);
    if ( wstr == NULL ) {
        // Can't allocate WIDE string
        goto just_copy_bytes;
    }

    /* Convert WIDE string to MultiByte string */
    codepage = getCodepage();
    plen = WideCharToMultiByte(codepage, 0, wstr, wlen,
                               output, outputMaxLen, NULL, NULL);
    free(wstr);
    if (plen <= 0) {
        // Can't convert WIDE string to multi-byte
        goto just_copy_bytes;
    }
    output[plen] = '\0';
    return plen;

just_copy_bytes:
    (void)memcpy(output, utf8, len);
    output[len] = 0;
    return len;
}

/*
 * Convert Platform Encoding to UTF-8.
 * NOTE: outputBufSize includes the space for the trailing 0.
 */
int JNICALL utf8FromPlatform(char *str, int len, jbyte *output, int outputBufSize) {
    int wlen;
    int plen;
    WCHAR* wstr;
    UINT codepage;
    int outputMaxLen;

    UTF_ASSERT(str);
    UTF_ASSERT(output);
    UTF_ASSERT(len >= 0);
    UTF_ASSERT(outputBufSize > len);
    outputMaxLen = outputBufSize - 1; // leave space for trailing 0

    /* Zero length is ok, but we don't need to do much */
    if ( len == 0 ) {
        output[0] = 0;
        return 0;
    }

    /* Get WIDE string version (assumes len>0) */
    codepage = getCodepage();
    wstr = getWideString(codepage, str, len, &wlen);
    if ( wstr == NULL ) {
        goto just_copy_bytes;
    }

    /* Convert WIDE string to UTF-8 string */
    plen = WideCharToMultiByte(CP_UTF8, 0, wstr, wlen,
                               (char*)output, outputMaxLen, NULL, NULL);
    free(wstr);
    if (plen <= 0) {
        UTF_ERROR(("Can't convert WIDE string to multi-byte"));
        goto just_copy_bytes;
    }
    output[plen] = '\0';
    return plen;

just_copy_bytes:
    (void)memcpy(output, str, len);
    output[len] = 0;
    return len;
}


#else
// *NIX specific part

#include <iconv.h>
#include <locale.h>
#include <langinfo.h>
#include <string.h>

typedef enum {TO_UTF8, FROM_UTF8} conv_direction;

/*
 * Do iconv() conversion.
 *    Returns length or -1 if output overflows.
 * NOTE: outputBufSize includes the space for the trailing 0.
 */
static int iconvConvert(conv_direction drn, char *bytes, size_t len, char *output, size_t outputBufSize) {

    static char *codeset = 0;
    iconv_t func;
    size_t bytes_converted;
    size_t inLeft, outLeft;
    char *inbuf, *outbuf;
    int outputMaxLen;

    UTF_ASSERT(bytes);
    UTF_ASSERT(output);
    UTF_ASSERT(outputBufSize > len);
    outputMaxLen = outputBufSize - 1; // leave space for trailing 0

    /* Zero length is ok, but we don't need to do much */
    if ( len == 0 ) {
        output[0] = 0;
        return 0;
    }

    if (codeset == NULL && codeset != (char *) -1) {
        // locale is not initialized, do it now
        if (setlocale(LC_ALL, "") != NULL) {
            // nl_langinfo returns ANSI_X3.4-1968 by default
            codeset = (char*)nl_langinfo(CODESET);
        }

        if (codeset == NULL) {
           // Not able to intialize process locale from platform one.
           codeset = (char *) -1;
        }
    }

    if (codeset == (char *) -1) {
      // There was an error during initialization, so just bail out
      goto just_copy_bytes;
    }

    func = (drn == TO_UTF8) ? iconv_open(codeset, "UTF-8") : iconv_open("UTF-8", codeset);
    if (func == (iconv_t) -1) {
        // Requested charset combination is not supported, conversion couldn't be done.
        // make sure we will not try it again
        codeset = (char *) -1;
        goto just_copy_bytes;
    }

    // perform conversion
    inbuf = bytes;
    outbuf = output;
    inLeft = len;
    outLeft = outputMaxLen;

    bytes_converted = iconv(func, (void*)&inbuf, &inLeft, &outbuf, &outLeft);
    if (bytes_converted == (size_t) -1 || bytes_converted == 0 || inLeft != 0) {
        // Input string is invalid, not able to convert entire string
        // or some other iconv error happens.
        iconv_close(func);
        goto just_copy_bytes;
    }

    iconv_close(func);
    // Overwrite bytes_converted with value of actually stored bytes
    bytes_converted = outputMaxLen-outLeft;
    output[bytes_converted] = 0;
    return bytes_converted;


just_copy_bytes:
    (void)memcpy(output, bytes, len);
    output[len] = 0;
    return len;
 }

/*
 * Convert UTF-8 to Platform Encoding.
 *    Returns length or -1 if output overflows.
 * NOTE: outputBufSize includes the space for the trailing 0.
 */
int JNICALL utf8ToPlatform(jbyte *utf8, int len, char *output, int outputBufSize) {
    return iconvConvert(FROM_UTF8, (char*)utf8, len, output, outputBufSize);
}

/*
 * Convert Platform Encoding to UTF-8.
 *    Returns length or -1 if output overflows.
 * NOTE: outputBufSize includes the space for the trailing 0.
 */
int JNICALL utf8FromPlatform(char *str, int len, jbyte *output, int outputBufSize) {
    return iconvConvert(TO_UTF8, str, len, (char*) output, outputBufSize);
}

#endif
