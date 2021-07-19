/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if (ARCH(I386) || ARCH(X86_64))
#    include <AK/bits/Math_x86.h>
#else
#    include <AK/bits/Math_lagom.h>
#endif

#ifdef __clang__
#    define MATH_CONSTEXPR ALWAYS_INLINE
#    define CONSTEXPR_STATE(function, args...)
#else
#    define MATH_CONSTEXPR constexpr
#endif

#undef MATH_CONSTEXPR

// This is mostely implements LibM in a more inline friendly manner (if available)
//
// Also this adds fast paths for rounding to integers,
// while fast_round[_to_int] is equivalent to rint from LibM and therefore equivalent
// to round in most cases, although it is way faster due to not setting the
// rounding-mode of the cpu before hand
