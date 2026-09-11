/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ctype.h>

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


// "The following shall be declared as functions and may also be defined as macros."
// This means that these functions need to be declared with external linkage.
// For performance reasons, we want these functions to be inlinable so we provide
// the definition in the header file. This however causes compatibility issues
// between C and C++.
// In C++, the compiler generates a symbol for inline functions at will. In the end,
// multiple TUs might include one (in this case the linker takes care of only keeping
// one) or if the function is always inlined, no symbols are emitted at all, which
// conflicts with POSIX requirements.
// In C, the linker isn't required to de-duplicate symbols emitted from inline
// functions, so the compiler doesn't emit symbols for inline functions and it falls
// back on the user to provide one if needed.
// To solve this issue we use a dedicated C feature that anchors the function
// definition in this exact TU.

// This link provides useful information on the different meanings of inline:
// https://stackoverflow.com/questions/216510/what-does-extern-inline-do

__attribute__((gnu_inline)) inline int isalnum(int c);
__attribute__((gnu_inline)) inline int isalpha(int c);
__attribute__((gnu_inline)) inline int isascii(int c);
__attribute__((gnu_inline)) inline int iscntrl(int c);
__attribute__((gnu_inline)) inline int isdigit(int c);
__attribute__((gnu_inline)) inline int isxdigit(int c);
__attribute__((gnu_inline)) inline int isspace(int c);
__attribute__((gnu_inline)) inline int ispunct(int c);
__attribute__((gnu_inline)) inline int isprint(int c);
__attribute__((gnu_inline)) inline int isgraph(int c);
__attribute__((gnu_inline)) inline int islower(int c);
__attribute__((gnu_inline)) inline int isupper(int c);
__attribute__((gnu_inline)) inline int isblank(int c);
__attribute__((gnu_inline)) inline int toascii(int c);
__attribute__((gnu_inline)) inline int tolower(int c);
__attribute__((gnu_inline)) inline int toupper(int c);
