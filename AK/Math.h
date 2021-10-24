/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Concepts.h>
#include <AK/MathDetail/common.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace AK {

namespace Division {
template<FloatingPoint T>
constexpr T fmod(T x, T y);
template<FloatingPoint T>
constexpr T remainder(T x, T y);
}

using Division::fmod;
using Division::remainder;

template<FloatingPoint T>
constexpr T sqrt(T x);

template<FloatingPoint T>
constexpr T cbrt(T x);

template<FloatingPoint T>
constexpr T fabs(T x);

namespace Trigonometry {

template<FloatingPoint T>
constexpr T hypot(T x, T y);
template<FloatingPoint T>
constexpr T sin(T angle);

template<FloatingPoint T>
constexpr T cos(T angle);

template<FloatingPoint T>
constexpr T tan(T angle);

template<FloatingPoint T>
constexpr T atan(T value);
template<FloatingPoint T>
constexpr T asin(T x);

template<FloatingPoint T>
constexpr T acos(T value);

template<FloatingPoint T>
constexpr T atan2(T y, T x);

}

using Trigonometry::acos;
using Trigonometry::asin;
using Trigonometry::atan;
using Trigonometry::atan2;
using Trigonometry::cos;
using Trigonometry::hypot;
using Trigonometry::sin;
using Trigonometry::tan;

namespace Exponentials {

template<FloatingPoint T>
constexpr T log(T x);

template<FloatingPoint T>
constexpr T log2(T x);

template<FloatingPoint T>
constexpr T log10(T x);

template<FloatingPoint T>
constexpr T exp(T exponent);

template<FloatingPoint T>
constexpr T exp2(T exponent);

}

using Exponentials::exp;
using Exponentials::exp2;
using Exponentials::log;
using Exponentials::log10;
using Exponentials::log2;

namespace Hyperbolic {

template<FloatingPoint T>
constexpr T sinh(T x);
template<FloatingPoint T>
constexpr T cosh(T x);
template<FloatingPoint T>
constexpr T tanh(T x);
template<FloatingPoint T>
constexpr T asinh(T x);
template<FloatingPoint T>
constexpr T acosh(T x);
template<FloatingPoint T>
constexpr T atanh(T x);

}

using Hyperbolic::acosh;
using Hyperbolic::asinh;
using Hyperbolic::atanh;
using Hyperbolic::cosh;
using Hyperbolic::sinh;
using Hyperbolic::tanh;

template<FloatingPoint T>
constexpr T pow(T x, T y);
}

#if ARCH(I386) || ARCH(X86_64)
#    include <AK/MathDetail/x86.h>
#elif not defined(__serenity__)
#    include <AK/MathDetail/stl_fallback.h>
#endif
