/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#ifndef EOF
#    define EOF (-1)
#endif

/* Do what newlib does to appease GCC's --with-newlib option. */
#define _U 01
#define _L 02
#define _N 04
#define _S 010
#define _P 020
#define _C 040
#define _X 0100
#define _B 0200

/**
 * newlib has a 257 byte _ctype_ array to enable compiler tricks to catch
 * people passing char instead of int. We don't engage in those tricks,
 * but still claim to be newlib to the toolchains
 */
extern char const _ctype_[1 + 256] __attribute__((visibility("default")));

extern inline __attribute__((gnu_inline)) int isalnum(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_U | _L | _N);
}

extern inline __attribute__((gnu_inline)) int isalpha(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_U | _L);
}

extern inline __attribute__((gnu_inline)) int isascii(int c)
{
    return (unsigned)c <= 127;
}

extern inline __attribute__((gnu_inline)) int iscntrl(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_C);
}

extern inline __attribute__((gnu_inline)) int isdigit(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_N);
}

extern inline __attribute__((gnu_inline)) int isxdigit(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_N | _X);
}

extern inline __attribute__((gnu_inline)) int isspace(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_S);
}

extern inline __attribute__((gnu_inline)) int ispunct(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_P);
}

extern inline __attribute__((gnu_inline)) int isprint(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_P | _U | _L | _N | _B);
}

extern inline __attribute__((gnu_inline)) int isgraph(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_P | _U | _L | _N);
}

extern inline __attribute__((gnu_inline)) int islower(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_L);
}

extern inline __attribute__((gnu_inline)) int isupper(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_U);
}

extern inline __attribute__((gnu_inline)) int isblank(int c)
{
    return _ctype_[(unsigned char)(c) + 1] & (_B) || (c == '\t');
}

extern inline __attribute__((gnu_inline)) int toascii(int c)
{
    return c & 127;
}

extern inline __attribute__((gnu_inline)) int tolower(int c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

extern inline __attribute__((gnu_inline)) int toupper(int c)
{
    if (c >= 'a' && c <= 'z')
        return c & ~0x20;
    return c;
}

__END_DECLS
