/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <dlfcn.h>
#include <pthread.h>
#include <regex.h>

static void* s_libregex;
static pthread_mutex_t s_libregex_lock;

static int (*s_regcomp)(regex_t*, char const*, int);
static int (*s_regexec)(regex_t const*, char const*, size_t, regmatch_t[], int);
static size_t (*s_regerror)(int, regex_t const*, char*, size_t);
static void (*s_regfree)(regex_t*);

static void ensure_libregex()
{
    pthread_mutex_lock(&s_libregex_lock);
    if (!s_libregex) {
        s_libregex = dlopen("libregex.so", RTLD_NOW);
        VERIFY(s_libregex);

        s_regcomp = reinterpret_cast<int (*)(regex_t*, char const*, int)>(dlsym(s_libregex, "regcomp"));
        VERIFY(s_regcomp);

        s_regexec = reinterpret_cast<int (*)(regex_t const*, char const*, size_t, regmatch_t[], int)>(dlsym(s_libregex, "regexec"));
        VERIFY(s_regexec);

        s_regerror = reinterpret_cast<size_t (*)(int, regex_t const*, char*, size_t)>(dlsym(s_libregex, "regerror"));
        VERIFY(s_regerror);

        s_regfree = reinterpret_cast<void (*)(regex_t*)>(dlsym(s_libregex, "regfree"));
        VERIFY(s_regerror);
    }
    pthread_mutex_unlock(&s_libregex_lock);
}

extern "C" {

int __attribute__((weak)) regcomp(regex_t* reg, char const* pattern, int cflags)
{
    ensure_libregex();
    return s_regcomp(reg, pattern, cflags);
}

int __attribute__((weak)) regexec(regex_t const* reg, char const* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    ensure_libregex();
    return s_regexec(reg, string, nmatch, pmatch, eflags);
}

size_t __attribute__((weak)) regerror(int errcode, regex_t const* reg, char* errbuf, size_t errbuf_size)
{
    ensure_libregex();
    return s_regerror(errcode, reg, errbuf, errbuf_size);
}

void __attribute__((weak)) regfree(regex_t* reg)
{
    ensure_libregex();
    return s_regfree(reg);
}
}
