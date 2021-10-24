/*
 * Copyright (c) 2022, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>

namespace AK {

template<FloatingPoint T>
constexpr T NaN = __builtin_nan("");
template<FloatingPoint T>
constexpr T Pi = 3.141592653589793238462643383279502884L;
template<FloatingPoint T>
constexpr T E = 2.718281828459045235360287471352662498L;

}
