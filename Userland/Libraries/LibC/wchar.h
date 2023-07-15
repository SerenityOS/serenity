/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/wchar.h.html
#include <ctype.h>
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include <bits/FILE.h>
#include <bits/wchar_size.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#ifndef WEOF
#    define WEOF (0xffffffffu)
#endif

#ifdef __cplusplus
#    define _LIBCPP_WCHAR_H_HAS_CONST_OVERLOADS
#endif

typedef __WINT_TYPE__ wint_t;
typedef unsigned long int wctype_t;

// A zero-initialized mbstate_t struct must be a valid initial state.
typedef struct {
    unsigned char bytes[4];
    unsigned int stored_bytes;
} mbstate_t;

struct tm;

size_t wcslen(wchar_t const*);
wchar_t* wcscpy(wchar_t*, wchar_t const*);
wchar_t* wcsdup(wchar_t const*);
wchar_t* wcsncpy(wchar_t*, wchar_t const*, size_t);
__attribute__((warn_unused_result)) size_t wcslcpy(wchar_t*, wchar_t const*, size_t);
int wcscmp(wchar_t const*, wchar_t const*);
int wcsncmp(wchar_t const*, wchar_t const*, size_t);
wchar_t* wcschr(wchar_t const*, int);
wchar_t* wcsrchr(wchar_t const*, wchar_t);
wchar_t* wcscat(wchar_t*, wchar_t const*);
wchar_t* wcsncat(wchar_t*, wchar_t const*, size_t);
wchar_t* wcstok(wchar_t*, wchar_t const*, wchar_t**);
long wcstol(wchar_t const*, wchar_t**, int);
long long wcstoll(wchar_t const*, wchar_t**, int);
wint_t btowc(int c);
size_t mbrtowc(wchar_t*, char const*, size_t, mbstate_t*);
size_t mbrlen(char const*, size_t, mbstate_t*);
size_t wcrtomb(char*, wchar_t, mbstate_t*);
int wcscoll(wchar_t const*, wchar_t const*);
size_t wcsxfrm(wchar_t*, wchar_t const*, size_t);
int wctob(wint_t);
int mbsinit(mbstate_t const*);
wchar_t* wcspbrk(wchar_t const*, wchar_t const*);
wchar_t* wcsstr(wchar_t const*, wchar_t const*);
wchar_t* wmemchr(wchar_t const*, wchar_t, size_t);
wchar_t* wmemcpy(wchar_t*, wchar_t const*, size_t);
wchar_t* wmemset(wchar_t*, wchar_t, size_t);
wchar_t* wmemmove(wchar_t*, wchar_t const*, size_t);
unsigned long wcstoul(wchar_t const*, wchar_t**, int);
unsigned long long wcstoull(wchar_t const*, wchar_t**, int);
float wcstof(wchar_t const*, wchar_t**);
double wcstod(wchar_t const*, wchar_t**);
long double wcstold(wchar_t const*, wchar_t**);
int wcwidth(wchar_t);
int wcswidth(wchar_t const*, size_t);
size_t wcsrtombs(char*, wchar_t const**, size_t, mbstate_t*);
size_t mbsrtowcs(wchar_t*, char const**, size_t, mbstate_t*);
int wmemcmp(wchar_t const*, wchar_t const*, size_t);
size_t wcsnrtombs(char*, wchar_t const**, size_t, size_t, mbstate_t*);
size_t mbsnrtowcs(wchar_t*, char const**, size_t, size_t, mbstate_t*);
size_t wcscspn(wchar_t const* wcs, wchar_t const* reject);
size_t wcsspn(wchar_t const* wcs, wchar_t const* accept);

wint_t fgetwc(FILE* stream);
wint_t getwc(FILE* stream);
wint_t getwchar(void);
wint_t fputwc(wchar_t wc, FILE* stream);
wint_t putwc(wchar_t wc, FILE* stream);
wint_t putwchar(wchar_t wc);
wchar_t* fgetws(wchar_t* __restrict ws, int n, FILE* __restrict stream);
int fputws(wchar_t const* __restrict ws, FILE* __restrict stream);
wint_t ungetwc(wint_t wc, FILE* stream);
int fwide(FILE* stream, int mode);

int wprintf(wchar_t const* __restrict format, ...);
int fwprintf(FILE* __restrict stream, wchar_t const* __restrict format, ...);
int swprintf(wchar_t* __restrict wcs, size_t maxlen, wchar_t const* __restrict format, ...);
int vwprintf(wchar_t const* __restrict format, va_list args);
int vfwprintf(FILE* __restrict stream, wchar_t const* __restrict format, va_list args);
int vswprintf(wchar_t* __restrict wcs, size_t maxlen, wchar_t const* __restrict format, va_list args);

int fwscanf(FILE* __restrict stream, wchar_t const* __restrict format, ...);
int swscanf(wchar_t const* __restrict ws, wchar_t const* __restrict format, ...);
int wscanf(wchar_t const* __restrict format, ...);
int vfwscanf(FILE* __restrict stream, wchar_t const* __restrict format, va_list arg);
int vswscanf(wchar_t const* __restrict ws, wchar_t const* __restrict format, va_list arg);
int vwscanf(wchar_t const* __restrict format, va_list arg);

size_t wcsftime(wchar_t* __restrict wcs, size_t maxsize, wchar_t const* __restrict format, const struct tm* __restrict timeptr);

__END_DECLS
