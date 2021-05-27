/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#ifndef WEOF
#    define WEOF (0xffffffffu)
#endif

typedef __WINT_TYPE__ wint_t;
typedef unsigned long int wctype_t;

size_t wcslen(const wchar_t*);
wchar_t* wcscpy(wchar_t*, const wchar_t*);
wchar_t* wcsncpy(wchar_t*, const wchar_t*, size_t);
int wcscmp(const wchar_t*, const wchar_t*);
int wcsncmp(const wchar_t*, const wchar_t*, size_t);
wchar_t* wcschr(const wchar_t*, int);
const wchar_t* wcsrchr(const wchar_t*, wchar_t);
wchar_t* wcscat(wchar_t*, const wchar_t*);
wchar_t* wcsncat(wchar_t*, const wchar_t*, size_t);
wchar_t* wcstok(wchar_t*, const wchar_t*, wchar_t**);
long wcstol(const wchar_t*, wchar_t**, int);
long long wcstoll(const wchar_t*, wchar_t**, int);
wint_t btowc(int c);

__END_DECLS
