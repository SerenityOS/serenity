/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/wchar.h>
#include <stddef.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1

__attribute__((noreturn)) void _abort(void);

__attribute__((malloc)) __attribute__((alloc_size(1))) void* malloc(size_t);
__attribute__((malloc)) __attribute__((alloc_size(1, 2))) void* calloc(size_t nmemb, size_t);
size_t malloc_size(void const*);
size_t malloc_good_size(size_t);
void serenity_dump_malloc_stats(void);
void free(void*);
__attribute__((alloc_size(2))) void* realloc(void* ptr, size_t);
__attribute__((malloc, alloc_size(1), alloc_align(2))) void* _aligned_malloc(size_t size, size_t alignment);
void _aligned_free(void* memblock);
char* getenv(const char* name);
char* secure_getenv(const char* name);
int putenv(char*);
int unsetenv(const char*);
int clearenv(void);
int setenv(const char* name, const char* value, int overwrite);
const char* getprogname(void);
void setprogname(const char*);
int atoi(const char*);
long atol(const char*);
long long atoll(const char*);
double strtod(const char*, char** endptr);
long double strtold(const char*, char** endptr);
float strtof(const char*, char** endptr);
long strtol(const char*, char** endptr, int base);
long long strtoll(const char*, char** endptr, int base);
unsigned long long strtoull(const char*, char** endptr, int base);
unsigned long strtoul(const char*, char** endptr, int base);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*));
void qsort_r(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*, void*), void* arg);
int atexit(void (*function)(void));
__attribute__((noreturn)) void exit(int status);
__attribute__((noreturn)) void abort(void);
char* ptsname(int fd);
int ptsname_r(int fd, char* buffer, size_t);
int abs(int);
long labs(long);
long long int llabs(long long int);
double atof(const char*);
int system(const char* command);
char* mktemp(char*);
int mkstemp(char*);
char* mkdtemp(char*);
void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*));
int mblen(char const*, size_t);
size_t mbstowcs(wchar_t*, const char*, size_t);
int mbtowc(wchar_t*, const char*, size_t);
int wctomb(char*, wchar_t);
size_t wcstombs(char*, const wchar_t*, size_t);
char* realpath(const char* pathname, char* buffer);
__attribute__((noreturn)) void _Exit(int status);

#define RAND_MAX 32767
int rand(void);
void srand(unsigned seed);

long int random(void);
void srandom(unsigned seed);

uint32_t arc4random(void);
void arc4random_buf(void*, size_t);
uint32_t arc4random_uniform(uint32_t);

typedef struct {
    int quot;
    int rem;
} div_t;
div_t div(int, int);
typedef struct {
    long quot;
    long rem;
} ldiv_t;
ldiv_t ldiv(long, long);
typedef struct {
    long long quot;
    long long rem;
} lldiv_t;
lldiv_t lldiv(long long, long long);

int posix_openpt(int flags);
int grantpt(int fd);
int unlockpt(int fd);

__END_DECLS
