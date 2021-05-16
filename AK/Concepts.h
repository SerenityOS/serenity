/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
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

template<typename T, typename U>
concept SameAs = IsSame<T, U>;

template<typename Func, typename... Args>
concept VoidFunction = requires(Func func, Args... args)
{
    {
        func(args...)
    }
    ->SameAs<void>;
};

template<typename Func, typename... Args>
concept IteratorFunction = requires(Func func, Args... args)
{
    {
        func(args...)
    }
    ->SameAs<IterationDecision>;
};

#endif

}

#if defined(__cpp_concepts) && !defined(__COVERITY__)

using AK::Concepts::Arithmetic;
using AK::Concepts::FloatingPoint;
using AK::Concepts::Integral;
using AK::Concepts::IteratorFunction;
using AK::Concepts::Signed;
using AK::Concepts::Unsigned;
using AK::Concepts::VoidFunction;

#endif
