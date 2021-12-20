/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/FILE.h>
#include <stdarg.h>
#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

// Note: wint_t is unsigned on gcc, and signed on clang.
#ifndef WEOF
#    ifdef __clang__
#        define WEOF (-1)
#    else
#        define WEOF (0xffffffffu)
#    endif
#endif

#undef WCHAR_MAX
#undef WCHAR_MIN
#define WCHAR_MAX __WCHAR_MAX__
#ifdef __WCHAR_MIN__
#    define WCHAR_MIN __WCHAR_MIN__
#else
// Note: This assumes that wchar_t is a signed type.
#    define WCHAR_MIN (-WCHAR_MAX - 1)
#endif

typedef __WINT_TYPE__ wint_t;
typedef unsigned long int wctype_t;

// A zero-initialized mbstate_t struct must be a valid initial state.
typedef struct {
    unsigned char bytes[4];
    unsigned int stored_bytes;
} mbstate_t;

struct tm;

size_t wcslen(const wchar_t*);
wchar_t* wcscpy(wchar_t*, const wchar_t*);
wchar_t* wcsdup(const wchar_t*);
wchar_t* wcsncpy(wchar_t*, const wchar_t*, size_t);
__attribute__((warn_unused_result)) size_t wcslcpy(wchar_t*, const wchar_t*, size_t);
int wcscmp(const wchar_t*, const wchar_t*);
int wcsncmp(const wchar_t*, const wchar_t*, size_t);
wchar_t* wcschr(const wchar_t*, int);
wchar_t* wcsrchr(const wchar_t*, wchar_t);
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
size_t wcsxfrm(wchar_t*, const wchar_t*, size_t);
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
double wcstod(const wchar_t*, wchar_t**);
long double wcstold(const wchar_t*, wchar_t**);
int wcwidth(wchar_t);
size_t wcsrtombs(char*, const wchar_t**, size_t, mbstate_t*);
size_t mbsrtowcs(wchar_t*, const char**, size_t, mbstate_t*);
int wmemcmp(const wchar_t*, const wchar_t*, size_t);
size_t wcsnrtombs(char*, const wchar_t**, size_t, size_t, mbstate_t*);
size_t mbsnrtowcs(wchar_t*, const char**, size_t, size_t, mbstate_t*);
size_t wcscspn(const wchar_t* wcs, const wchar_t* reject);
size_t wcsspn(const wchar_t* wcs, const wchar_t* accept);

wint_t fgetwc(FILE* stream);
wint_t getwc(FILE* stream);
wint_t getwchar(void);
wint_t fputwc(wchar_t wc, FILE* stream);
wint_t putwc(wchar_t wc, FILE* stream);
wint_t putwchar(wchar_t wc);
wchar_t* fgetws(wchar_t* __restrict ws, int n, FILE* __restrict stream);
int fputws(const wchar_t* __restrict ws, FILE* __restrict stream);
wint_t ungetwc(wint_t wc, FILE* stream);
int fwide(FILE* stream, int mode);

int wprintf(const wchar_t* __restrict format, ...);
int fwprintf(FILE* __restrict stream, const wchar_t* __restrict format, ...);
int swprintf(wchar_t* __restrict wcs, size_t maxlen, const wchar_t* __restrict format, ...);
int vwprintf(const wchar_t* __restrict format, va_list args);
int vfwprintf(FILE* __restrict stream, const wchar_t* __restrict format, va_list args);
int vswprintf(wchar_t* __restrict wcs, size_t maxlen, const wchar_t* __restrict format, va_list args);

int fwscanf(FILE* __restrict stream, const wchar_t* __restrict format, ...);
int swscanf(const wchar_t* __restrict ws, const wchar_t* __restrict format, ...);
int wscanf(const wchar_t* __restrict format, ...);
int vfwscanf(FILE* __restrict stream, const wchar_t* __restrict format, va_list arg);
int vswscanf(const wchar_t* __restrict ws, const wchar_t* __restrict format, va_list arg);
int vwscanf(const wchar_t* __restrict format, va_list arg);

size_t wcsftime(wchar_t* __restrict wcs, size_t maxsize, const wchar_t* __restrict format, const struct tm* __restrict timeptr);

__END_DECLS
