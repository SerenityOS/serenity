/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stddef.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__attribute__((warn_unused_result)) int __generate_unique_filename(char* pattern);

__BEGIN_DECLS

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1
#define MB_CUR_MAX 1

__attribute__((malloc)) __attribute__((alloc_size(1))) void* malloc(size_t);
__attribute__((malloc)) __attribute__((alloc_size(1, 2))) void* calloc(size_t nmemb, size_t);
size_t malloc_size(void*);
size_t malloc_good_size(size_t);
void serenity_dump_malloc_stats(void);
void free(void*);
__attribute__((alloc_size(2))) void* realloc(void* ptr, size_t);
char* getenv(char const* name);
char* secure_getenv(char const* name);
int putenv(char*);
int unsetenv(char const*);
int clearenv(void);
int setenv(char const* name, char const* value, int overwrite);
char const* getprogname();
void setprogname(char const*);
int atoi(char const*);
long atol(char const*);
long long atoll(char const*);
double strtod(char const*, char** endptr);
long double strtold(char const*, char** endptr);
float strtof(char const*, char** endptr);
long strtol(char const*, char** endptr, int base);
long long strtoll(char const*, char** endptr, int base);
unsigned long long strtoull(char const*, char** endptr, int base);
unsigned long strtoul(char const*, char** endptr, int base);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(void const*, void const*));
void qsort_r(void* base, size_t nmemb, size_t size, int (*compar)(void const*, void const*, void*), void* arg);
int atexit(void (*function)());
__attribute__((noreturn)) void exit(int status);
__attribute__((noreturn)) void abort();
char* ptsname(int fd);
int ptsname_r(int fd, char* buffer, size_t);
int abs(int);
long labs(long);
long long int llabs(long long int);
double atof(char const*);
int system(char const* command);
char* mktemp(char*);
int mkstemp(char*);
char* mkdtemp(char*);
void* bsearch(void const* key, void const* base, size_t nmemb, size_t size, int (*compar)(void const*, void const*));
int mblen(char const*, size_t);
size_t mbstowcs(wchar_t*, char const*, size_t);
int mbtowc(wchar_t*, char const*, size_t);
int wctomb(char*, wchar_t);
size_t wcstombs(char*, wchar_t const*, size_t);
char* realpath(char const* pathname, char* buffer);
__attribute__((noreturn)) void _Exit(int status);

#define RAND_MAX 32767
int rand();
void srand(unsigned seed);

long int random();
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

long getauxval(long type);

__END_DECLS
