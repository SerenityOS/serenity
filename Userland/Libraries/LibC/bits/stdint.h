/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/wchar_size.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

typedef __UINT64_TYPE__ uint64_t;
typedef __UINT32_TYPE__ uint32_t;
typedef __UINT16_TYPE__ uint16_t;
typedef __UINT8_TYPE__ uint8_t;

typedef __INT64_TYPE__ int64_t;
typedef __INT32_TYPE__ int32_t;
typedef __INT16_TYPE__ int16_t;
typedef __INT8_TYPE__ int8_t;

typedef __UINT_FAST8_TYPE__ uint_fast8_t;
typedef __UINT_FAST16_TYPE__ uint_fast16_t;
typedef __UINT_FAST32_TYPE__ uint_fast32_t;
typedef __UINT_FAST64_TYPE__ uint_fast64_t;

typedef __INT_FAST8_TYPE__ int_fast8_t;
typedef __INT_FAST16_TYPE__ int_fast16_t;
typedef __INT_FAST32_TYPE__ int_fast32_t;
typedef __INT_FAST64_TYPE__ int_fast64_t;

typedef __UINT_LEAST8_TYPE__ uint_least8_t;
typedef __UINT_LEAST16_TYPE__ uint_least16_t;
typedef __UINT_LEAST32_TYPE__ uint_least32_t;
typedef __UINT_LEAST64_TYPE__ uint_least64_t;

typedef __INT_LEAST8_TYPE__ int_least8_t;
typedef __INT_LEAST16_TYPE__ int_least16_t;
typedef __INT_LEAST32_TYPE__ int_least32_t;
typedef __INT_LEAST64_TYPE__ int_least64_t;

#define __int8_t_defined 1
#define __uint8_t_defined 1
#define __int16_t_defined 1
#define __uint16_t_defined 1
#define __int32_t_defined 1
#define __uint32_t_defined 1
#define __int64_t_defined 1
#define __uint64_t_defined 1

#define INT8_C(x) x
#define UINT8_C(x) x

#define INT16_C(x) x
#define UINT16_C(x) x

#define INT32_C(x) x
#define UINT32_C(x) x##U

#ifdef __clang__
#    define __int_c_concat(a, b) a##b
#    define __int_c(var, suffix) __int_c_concat(var, suffix)

#    define INT64_C(x) __int_c(x, __INT64_C_SUFFIX__)
#    define UINT64_C(x) __int_c(x, __UINT64_C_SUFFIX__)

#    define INTMAX_C(x) __int_c(x, __INTMAX_C_SUFFIX__)
#    define UINTMAX_C(x) __int_c(x, __UINTMAX_C_SUFFIX__)
#else
#    define INT64_C(x) __INT64_C(x)
#    define UINT64_C(x) __UINT64_C(x)

#    define INTMAX_C(x) __INTMAX_C(x)
#    define UINTMAX_C(x) __UINTMAX_C(x)
#endif

typedef __UINTPTR_TYPE__ uintptr_t;
typedef __INTPTR_TYPE__ intptr_t;

typedef __UINTMAX_TYPE__ uintmax_t;
#define UINTMAX_MAX __UINTMAX_MAX__

typedef __INTMAX_TYPE__ intmax_t;
#define INTMAX_MAX __INTMAX_MAX__
#define INTMAX_MIN (-INTMAX_MAX - 1)

#define INT8_MIN (-128)
#define INT16_MIN (-32767 - 1)
#define INT32_MIN (-2147483647 - 1)
#define INT64_MIN (-INT64_C(9223372036854775807) - 1)
#define INT8_MAX (127)
#define INT16_MAX (32767)
#define INT32_MAX (2147483647)
#define INT64_MAX (INT64_C(9223372036854775807))
#define UINT8_MAX (255)
#define UINT16_MAX (65535)
#define UINT32_MAX (4294967295U)
#define UINT64_MAX (UINT64_C(18446744073709551615))

#define INTPTR_MAX __INTPTR_MAX__
#define INTPTR_MIN (-INTPTR_MAX - 1)
#define UINTPTR_MAX __UINTPTR_MAX__

#define INT_FAST8_MIN INT8_MIN
#define INT_FAST16_MIN INT16_MIN
#define INT_FAST32_MIN INT32_MIN
#define INT_FAST64_MIN INT64_MIN

#define INT_FAST8_MAX INT8_MAX
#define INT_FAST16_MAX INT16_MAX
#define INT_FAST32_MAX INT32_MAX
#define INT_FAST64_MAX INT64_MAX

#define UINT_FAST8_MAX UINT8_MAX
#define UINT_FAST16_MAX UINT16_MAX
#define UINT_FAST32_MAX UINT32_MAX
#define UINT_FAST64_MAX UINT64_MAX

#define INT_LEAST8_MIN INT8_MIN
#define INT_LEAST16_MIN INT16_MIN
#define INT_LEAST32_MIN INT32_MIN
#define INT_LEAST64_MIN INT64_MIN

#define INT_LEAST8_MAX INT8_MAX
#define INT_LEAST16_MAX INT16_MAX
#define INT_LEAST32_MAX INT32_MAX
#define INT_LEAST64_MAX INT64_MAX

#define UINT_LEAST8_MAX UINT8_MAX
#define UINT_LEAST16_MAX UINT16_MAX
#define UINT_LEAST32_MAX UINT32_MAX
#define UINT_LEAST64_MAX UINT64_MAX

#define SIZE_MAX __SIZE_MAX__

#define PTRDIFF_MAX __PTRDIFF_MAX__
#define PTRDIFF_MIN (-__PTRDIFF_MAX__ - 1)

__END_DECLS
