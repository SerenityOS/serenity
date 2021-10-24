/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MathDetail/common.h>

#include <AK/Concepts.h>
#include <AK/Types.h>
#include <cmath>

namespace AK {

namespace Division {
template<FloatingPoint T>
constexpr T fmod(T x, T y) { return std::fmod(x, y); }
template<FloatingPoint T>
constexpr T remainder(T x, T y) { return std::remainder(x, y); }
}

template<FloatingPoint T>
constexpr T sqrt(T x) { return std::sqrt(x); }

template<FloatingPoint T>
constexpr T cbrt(T x) { return std::cbrt(x); }

template<FloatingPoint T>
constexpr T fabs(T x) { return std::fabs(x); }

namespace Trigonometry {
template<FloatingPoint T>
constexpr T hypot(T x, T y) { return std::hypot(x, y); }
template<FloatingPoint T>
constexpr T sin(T angle) { return std::sin(angle); }
template<FloatingPoint T>
constexpr T cos(T angle) { return std::cos(angle); }
template<FloatingPoint T>
constexpr T tan(T angle) { return std::tan(angle); }
template<FloatingPoint T>
constexpr T atan(T x) { return std::atan(x); }
template<FloatingPoint T>
constexpr T asin(T x) { return std::asin(x); }
template<FloatingPoint T>
constexpr T acos(T x) { return std::acos(x); }
template<FloatingPoint T>
constexpr T atan2(T y, T x) { return std::atan2(y, x); }
}

namespace Exponentials {
template<FloatingPoint T>
constexpr T log(T x) { return std::log(x); }
template<FloatingPoint T>
constexpr T log2(T x) { return std::log2(x); }
template<FloatingPoint T>
constexpr T log10(T x) { return std::log10(x); }
template<FloatingPoint T>
constexpr T exp(T exponent) { return std::exp(exponent); }
template<FloatingPoint T>
constexpr T exp2(T exponent) { return std::exp2(exponent); }
}

namespace Hyperbolic {
template<FloatingPoint T>
constexpr T sinh(T x) { return std::sinh(x); }
template<FloatingPoint T>
constexpr T cosh(T x) { return std::cosh(x); }
template<FloatingPoint T>
constexpr T tanh(T x) { return std::tanh(x); }
template<FloatingPoint T>
constexpr T asinh(T x) { return std::asinh(x); }
template<FloatingPoint T>
constexpr T acosh(T x) { return std::acosh(x); }
template<FloatingPoint T>
constexpr T atanh(T x) { return std::atanh(x); }
}

template<FloatingPoint T>
constexpr T pow(T x, T y) { return std::pow(x, y); };

}
