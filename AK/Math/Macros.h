/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Concepts.h>
#include <AK/StdLibExtras.h>

#define CALL_BUILTIN(function, args...)           \
    do {                                          \
        if constexpr (IsSame<T, long double>)     \
            return __builtin_##function##l(args); \
        if constexpr (IsSame<T, double>)          \
            return __builtin_##function(args);    \
        if constexpr (IsSame<T, float>)           \
            return __builtin_##function##f(args); \
    } while (0)

#define CONSTEXPR_STATE(function, args...) \
    if (is_constant_evaluated())           \
        CALL_BUILTIN(function, args);

#define AARCH64_INSTRUCTION(instruction, arg) \
    if constexpr (IsSame<T, long double>)     \
        TODO();                               \
    if constexpr (IsSame<T, double>) {        \
        double res;                           \
        asm(#instruction " %d0, %d1"          \
            : "=w"(res)                       \
            : "w"(arg));                      \
        return res;                           \
    }                                         \
    if constexpr (IsSame<T, float>) {         \
        float res;                            \
        asm(#instruction " %s0, %s1"          \
            : "=w"(res)                       \
            : "w"(arg));                      \
        return res;                           \
    }
