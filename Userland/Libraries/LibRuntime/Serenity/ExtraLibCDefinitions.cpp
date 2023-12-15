/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/BuiltinWrappers.h>
#include <AK/FloatingPoint.h>
#include <Kernel/API/POSIX/signal.h>
#include <LibSystem/syscall.h>
#include <bits/stdio_file_implementation.h>
#include <errno.h>
#include <math.h>
#include <sched.h>
#include <sys/internals.h>
#include <wchar.h>

// LibC initialization routines
char** environ = nullptr;
bool __environ_is_malloced = false;
bool __stdio_is_initialized = false;
void* __auxiliary_vector = nullptr;

void __libc_init()
{
    VERIFY(environ);
    char** env;
    for (env = environ; *env; ++env)
        ;
    __auxiliary_vector = (void*)++env;

    __malloc_init();
    __stdio_init();
}

extern "C" void (*__call_fini_functions)();
void (*__call_fini_functions)() = nullptr;

void exit(int status)
{
    __cxa_finalize(nullptr);

    if (Runtime::secure_getenv("LIBC_DUMP_MALLOC_STATS"sv).has_value())
        serenity_dump_malloc_stats();

    __call_fini_functions();
    FILE::flush_open_streams();
    __pthread_key_destroy_for_current_thread();

    syscall(SC_exit, status);
    VERIFY_NOT_REACHED();
}

// Needed by strsignal
char const* sys_siglist[NSIG] = {
#define DESCRIPTION(name, description) description,
    __ENUMERATE_SIGNALS(DESCRIPTION)
#undef __ENUMERATE_SIGNAL
};

// compiler-rt doesn't know how to implement these for 80-bit long doubles.
// Instead of copying functions from math.cpp here, teach compiler-rt how to work with long doubles.
long double fmaxl(long double x, long double y)
{
    if (__builtin_isnan(x))
        return y;
    if (__builtin_isnan(y))
        return x;
    return x > y ? x : y;
}

long double logbl(long double x)
{
    if (x == 0)
        return FP_ILOGB0;
    if (__builtin_isnan(x))
        return FP_ILOGNAN;
    if (!__builtin_isfinite(x))
        return INT_MAX;

    FloatExtractor<long double> extractor { .d = x };
    return (int)extractor.exponent - extractor.exponent_bias;
}

long double scalbnl(long double x, int exponent)
{
    if (x == 0 || !__builtin_isfinite(x) || __builtin_isnan(x) || exponent == 0)
        return x;

    FloatExtractor<long double> extractor { .d = x };

    if (extractor.exponent != 0) {
        extractor.exponent = clamp((int)extractor.exponent + exponent, 0, (int)extractor.exponent_max);
        return extractor.d;
    }

    unsigned leading_mantissa_zeroes = extractor.mantissa == 0 ? 32 : count_leading_zeroes(extractor.mantissa);
    int shift = min((int)leading_mantissa_zeroes, exponent);
    exponent = max(exponent - shift, 0);

    extractor.exponent <<= shift;
    extractor.exponent = exponent + 1;

    return extractor.d;
}

// cxa_demangle relies on isxdigit for some reason.
#undef isxdigit
int isxdigit(int ch)
{
    return ('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'f') || ('A' <= ch && ch <= 'F');
}

// AK/PrintfImplementation.h uses this function.
// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wcslen.html
size_t wcslen(wchar_t const* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

// FIXME: sched_yield is the exact opposite of a reasonable API. Remove its use in AK/Singleton.h
//        and move definition back to libc.
// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_yield.html
int sched_yield()
{
    int rc = syscall(SC_yield);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
