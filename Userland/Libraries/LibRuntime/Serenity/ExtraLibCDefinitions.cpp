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
#include <LibRuntime/Mutex.h>
#include <LibRuntime/Serenity/PosixThreadSupport.h>
#include <LibRuntime/System.h>
#include <LibSystem/syscall.h>
#include <bits/stdio_file_implementation.h>
#include <errno.h>
#include <math.h>
#include <sched.h>
#include <stdlib.h>
#include <sys/internals.h>
#include <wchar.h>

// LibC initialization routines
[[gnu::weak]] char** environ = nullptr; // Populated by DynamicLinker in shared executables.
bool __environ_is_malloced = false;
bool __stdio_is_initialized = false;
void* __auxiliary_vector = nullptr;

[[gnu::constructor]] void __libc_init()
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
[[gnu::weak]] void (*__call_fini_functions)() = nullptr; // Populated by DynamicLinker in shared executables.

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

// FIXME: We should instead provide a wrapper for SC_getrandom in System.h.
void arc4random_buf(void* buffer, size_t buffer_size)
{
    static constinit Runtime::Mutex s_randomness_mutex;
    static u8* s_randomness_buffer = nullptr;
    static size_t s_randomness_index = 0;

    Runtime::MutexLocker lock(s_randomness_mutex);

    size_t bytes_needed = buffer_size;
    auto* ptr = static_cast<u8*>(buffer);

    while (bytes_needed > 0) {
        if (!s_randomness_buffer || s_randomness_index >= PAGE_SIZE) {
            if (!s_randomness_buffer) {
                auto flags = Runtime::MMap::Anonymous | Runtime::MMap::Private | Runtime::MMap::Randomized;
                s_randomness_buffer = static_cast<u8*>(MUST(Runtime::mmap(nullptr, PAGE_SIZE, Runtime::RegionAccess::ReadWrite, flags, ""sv, -1, 0, 0)));
                Runtime::register_pthread_callback(Runtime::CallbackType::ForkChild,
                    [] {
                        MUST(Runtime::munmap(s_randomness_buffer, PAGE_SIZE));
                        s_randomness_buffer = nullptr;
                        s_randomness_index = 0;
                    });
            }
            syscall(SC_getrandom, s_randomness_buffer, PAGE_SIZE);
            s_randomness_index = 0;
        }

        size_t available_bytes = PAGE_SIZE - s_randomness_index;
        size_t bytes_to_copy = min(bytes_needed, available_bytes);

        memcpy(ptr, s_randomness_buffer + s_randomness_index, bytes_to_copy);

        s_randomness_index += bytes_to_copy;
        bytes_needed -= bytes_to_copy;
        ptr += bytes_to_copy;
    }
}
