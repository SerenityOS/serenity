/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>

namespace AK::Concepts {

#if defined(__cpp_concepts) && !defined(__COVERITY__)

template<typename T>
concept Integral = IsIntegral<T>;

template<typename T>
concept FloatingPoint = IsFloatingPoint<T>;

template<typename T>
concept Arithmetic = IsArithmetic<T>;

template<typename T>
concept Signed = IsSigned<T>;

template<typename T>
concept Unsigned = IsUnsigned<T>;

#endif

}

#if defined(__cpp_concepts) && !defined(__COVERITY__)

using AK::Concepts::Arithmetic;
using AK::Concepts::FloatingPoint;
using AK::Concepts::Integral;
using AK::Concepts::Signed;
using AK::Concepts::Unsigned;

#endif
