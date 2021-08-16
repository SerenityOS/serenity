/*
 * MUSCLE SmartCard Development ( https://pcsclite.apdu.fr/ )
 *
 * Copyright (C) 1999
 *  David Corcoran <corcoran@musclecard.com>
 * Copyright (C) 2002-2011
 *  Ludovic Rousseau <ludovic.rousseau@free.fr>
 *
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @file
 * @brief This keeps a list of Windows(R) types.
 */

#ifndef __wintypes_h__
#define __wintypes_h__

#ifdef __cplusplus
extern "C"
{
#endif

#ifdef __APPLE__

#include <stdint.h>

#ifndef BYTE
    typedef uint8_t BYTE;
#endif
    typedef uint8_t UCHAR;
    typedef UCHAR *PUCHAR;
    typedef uint16_t USHORT;

#ifndef __COREFOUNDATION_CFPLUGINCOM__
    typedef uint32_t ULONG;
    typedef void *LPVOID;
    typedef int16_t BOOL;
#endif

    typedef ULONG *PULONG;
    typedef const void *LPCVOID;
    typedef uint32_t DWORD;
    typedef DWORD *PDWORD;
    typedef uint16_t WORD;
    typedef int32_t LONG;
    typedef const char *LPCSTR;
    typedef const BYTE *LPCBYTE;
    typedef BYTE *LPBYTE;
    typedef DWORD *LPDWORD;
    typedef char *LPSTR;

#else

#ifndef BYTE
    typedef unsigned char BYTE;
#endif
    typedef unsigned char UCHAR;
    typedef UCHAR *PUCHAR;
    typedef unsigned short USHORT;

#ifndef __COREFOUNDATION_CFPLUGINCOM__
    typedef unsigned long ULONG;
    typedef void *LPVOID;
#endif

    typedef const void *LPCVOID;
    typedef unsigned long DWORD;
    typedef DWORD *PDWORD;
    typedef long LONG;
    typedef const char *LPCSTR;
    typedef const BYTE *LPCBYTE;
    typedef BYTE *LPBYTE;
    typedef DWORD *LPDWORD;
    typedef char *LPSTR;

    /* these types were deprecated but still used by old drivers and
     * applications. So just declare and use them. */
    typedef LPSTR LPTSTR;
    typedef LPCSTR LPCTSTR;

    /* types unused by pcsc-lite */
    typedef short BOOL;
    typedef unsigned short WORD;
    typedef ULONG *PULONG;

#endif

#ifdef __cplusplus
}
#endif

#endif
