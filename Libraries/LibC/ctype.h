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

#include <string.h>
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

int tolower(int);
int toupper(int);

#define isalnum(c) (_ctype_[(int)(c)] & (_U | _L | _N))
#define isalpha(c) (_ctype_[(int)(c)] & (_U | _L))
#define iscntrl(c) (_ctype_[(int)(c)] & (_C))
#define isdigit(c) (_ctype_[(int)(c)] & (_N))
#define isxdigit(c) (_ctype_[(int)(c)] & (_N | _X))
#define isspace(c) (_ctype_[(int)(c)] & (_S))
#define ispunct(c) (_ctype_[(int)(c)] & (_P))
#define isprint(c) (_ctype_[(int)(c)] & (_P | _U | _L | _N | _B))
#define isgraph(c) (_ctype_[(int)(c)] & (_P | _U | _L | _N))
#define islower(c) ((_ctype_[(int)(c)] & (_U | _L)) == _L)
#define isupper(c) ((_ctype_[(int)(c)] & (_U | _L)) == _U)
#define isblank(c) ((_ctype_[(int)(c)] & (_B)))

#define isascii(c) ((unsigned)c <= 127)
#define toascii(c) ((c)&127)

__END_DECLS
