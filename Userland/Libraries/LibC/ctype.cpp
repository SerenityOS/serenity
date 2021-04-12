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

#include <ctype.h>

extern "C" {

const char _ctype_[256] = {
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
    return ((unsigned)c <= 127);
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
