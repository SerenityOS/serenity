/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

/* Do what newlib does to appease GCC's --with-newlib option. */
#define _U 01
#define _L 02
#define _N 04
#define _S 010
#define _P 020
#define _C 040
#define _X 0100
#define _B 0200

extern const char _ctype_[256];

static inline int __inline_isalnum(int c)
{
    return _ctype_[(unsigned char)(c)] & (_U | _L | _N);
}

static inline int __inline_isalpha(int c)
{
    return _ctype_[(unsigned char)(c)] & (_U | _L);
}

static inline int __inline_isascii(int c)
{
    return (unsigned)c <= 127;
}

static inline int __inline_iscntrl(int c)
{
    return _ctype_[(unsigned char)(c)] & (_C);
}

static inline int __inline_isdigit(int c)
{
    return _ctype_[(unsigned char)(c)] & (_N);
}

static inline int __inline_isxdigit(int c)
{
    return _ctype_[(unsigned char)(c)] & (_N | _X);
}

static inline int __inline_isspace(int c)
{
    return _ctype_[(unsigned char)(c)] & (_S);
}

static inline int __inline_ispunct(int c)
{
    return _ctype_[(unsigned char)(c)] & (_P);
}

static inline int __inline_isprint(int c)
{
    return _ctype_[(unsigned char)(c)] & (_P | _U | _L | _N | _B);
}

static inline int __inline_isgraph(int c)
{
    return _ctype_[(unsigned char)(c)] & (_P | _U | _L | _N);
}

static inline int __inline_islower(int c)
{
    return _ctype_[(unsigned char)(c)] & (_L);
}

static inline int __inline_isupper(int c)
{
    return _ctype_[(unsigned char)(c)] & (_U);
}

static inline int __inline_toascii(int c)
{
    return c & 127;
}

static inline int __inline_tolower(int c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

static inline int __inline_toupper(int c)
{
    if (c >= 'a' && c <= 'z')
        return c & ~0x20;
    return c;
}

#ifdef __cplusplus
extern "C" {
#endif

int isalnum(int c);
int isalpha(int c);
int isascii(int c);
int iscntrl(int c);
int isdigit(int c);
int isxdigit(int c);
int isspace(int c);
int ispunct(int c);
int isprint(int c);
int isgraph(int c);
int islower(int c);
int isupper(int c);
int toascii(int c);
int tolower(int c);
int toupper(int c);

#ifdef __cplusplus
}
#endif
#define isalnum __inline_isalnum
#define isalpha __inline_isalpha
#define isascii __inline_isascii
#define iscntrl __inline_iscntrl
#define isdigit __inline_isdigit
#define isxdigit __inline_isxdigit
#define isspace __inline_isspace
#define ispunct __inline_ispunct
#define isprint __inline_isprint
#define isgraph __inline_isgraph
#define islower __inline_islower
#define isupper __inline_isupper
#define toascii __inline_toascii
#define tolower __inline_tolower
#define toupper __inline_toupper

__END_DECLS
