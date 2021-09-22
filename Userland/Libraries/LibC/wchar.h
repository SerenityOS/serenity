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

typedef struct {
    unsigned char bytes[4];
} mbstate_t;

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
size_t mbrtowc(wchar_t*, const char*, size_t, mbstate_t*);
size_t mbrlen(const char*, size_t, mbstate_t*);
size_t wcrtomb(char*, wchar_t, mbstate_t*);
int wcscoll(const wchar_t*, const wchar_t*);
int wctob(wint_t);
int mbsinit(const mbstate_t*);
wchar_t* wcspbrk(const wchar_t*, const wchar_t*);
wchar_t* wcsstr(const wchar_t*, const wchar_t*);
wchar_t* wmemchr(const wchar_t*, wchar_t, size_t);
wchar_t* wmemcpy(wchar_t*, const wchar_t*, size_t);
wchar_t* wmemset(wchar_t*, wchar_t, size_t);
wchar_t* wmemmove(wchar_t*, const wchar_t*, size_t);
unsigned long wcstoul(const wchar_t*, wchar_t**, int);
unsigned long long wcstoull(const wchar_t*, wchar_t**, int);
float wcstof(const wchar_t*, wchar_t**);

__END_DECLS
