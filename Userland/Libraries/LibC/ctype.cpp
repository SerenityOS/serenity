/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ctype.h>

extern "C" {

char const _ctype_[1 + 256] = {
    0,
    _C, _C, _C, _C, _C, _C, _C, _C,
    _C, _C | _S, _C | _S, _C | _S, _C | _S, _C | _S, _C, _C,
    _C, _C, _C, _C, _C, _C, _C, _C,
    _C, _C, _C, _C, _C, _C, _C, _C,
    (char)(_S | _B), _P, _P, _P, _P, _P, _P, _P,
    _P, _P, _P, _P, _P, _P, _P, _P,
    _N, _N, _N, _N, _N, _N, _N, _N,
    _N, _N, _P, _P, _P, _P, _P, _P,
    _P, _U | _X, _U | _X, _U | _X, _U | _X, _U | _X, _U | _X, _U,
    _U, _U, _U, _U, _U, _U, _U, _U,
    _U, _U, _U, _U, _U, _U, _U, _U,
    _U, _U, _U, _P, _P, _P, _P, _P,
    _P, _L | _X, _L | _X, _L | _X, _L | _X, _L | _X, _L | _X, _L,
    _L, _L, _L, _L, _L, _L, _L, _L,
    _L, _L, _L, _L, _L, _L, _L, _L,
    _L, _L, _L, _P, _P, _P, _P, _C
};

#undef isalnum
int isalnum(int c)
{
    return __inline_isalnum(c);
}

#undef isalpha
int isalpha(int c)
{
    return __inline_isalpha(c);
}

#undef iscntrl
int iscntrl(int c)
{
    return __inline_iscntrl(c);
}

#undef isdigit
int isdigit(int c)
{
    return __inline_isdigit(c);
}

#undef isxdigit
int isxdigit(int c)
{
    return __inline_isxdigit(c);
}

#undef isspace
int isspace(int c)
{
    return __inline_isspace(c);
}

#undef ispunct
int ispunct(int c)
{
    return __inline_ispunct(c);
}

#undef isprint
int isprint(int c)
{
    return __inline_isprint(c);
}

#undef isgraph
int isgraph(int c)
{
    return __inline_isgraph(c);
}

#undef isupper
int isupper(int c)
{
    return __inline_isupper(c);
}

#undef islower
int islower(int c)
{
    return __inline_islower(c);
}

#undef isascii
int isascii(int c)
{
    return __inline_isascii(c);
}

#undef isblank
int isblank(int c)
{
    return __inline_isblank(c);
}

#undef toascii
int toascii(int c)
{
    return __inline_toascii(c);
}

#undef tolower
int tolower(int c)
{
    return __inline_tolower(c);
}

#undef toupper
int toupper(int c)
{
    return __inline_toupper(c);
}
}
