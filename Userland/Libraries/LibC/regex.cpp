/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibDl/dlfcn.h>
#include <LibDl/dlfcn_integration.h>
#include <pthread.h>
#include <regex.h>

static void* s_libregex;
static pthread_mutex_t s_libregex_lock;

static int (*s_regcomp)(regex_t*, const char*, int);
static int (*s_regexec)(const regex_t*, const char*, size_t, regmatch_t[], int);
static size_t (*s_regerror)(int, const regex_t*, char*, size_t);
static void (*s_regfree)(regex_t*);

static void ensure_libregex()
{
    pthread_mutex_lock(&s_libregex_lock);
    if (!s_libregex) {
        s_libregex = __dlopen("libregex.so", RTLD_NOW).value();

        s_regcomp = reinterpret_cast<int (*)(regex_t*, const char*, int)>(__dlsym(s_libregex, "regcomp").value());
        s_regexec = reinterpret_cast<int (*)(const regex_t*, const char*, size_t, regmatch_t[], int)>(__dlsym(s_libregex, "regexec").value());
        s_regerror = reinterpret_cast<size_t (*)(int, const regex_t*, char*, size_t)>(__dlsym(s_libregex, "regerror").value());
        s_regfree = reinterpret_cast<void (*)(regex_t*)>(__dlsym(s_libregex, "regfree").value());
    }
    pthread_mutex_unlock(&s_libregex_lock);
}

extern "C" {

int __attribute__((weak)) regcomp(regex_t* reg, const char* pattern, int cflags)
{
    ensure_libregex();
    return s_regcomp(reg, pattern, cflags);
}

int __attribute__((weak)) regexec(const regex_t* reg, const char* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    ensure_libregex();
    return s_regexec(reg, string, nmatch, pmatch, eflags);
}

size_t __attribute__((weak)) regerror(int errcode, const regex_t* reg, char* errbuf, size_t errbuf_size)
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
