/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
#include <AK/StdLibExtras.h>

namespace AK::Concepts {

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

template<typename T>
concept Enum = IsEnum<T>;

template<typename T, typename U>
concept SameAs = IsSame<T, U>;

// FIXME: remove once Clang formats these properly.
// clang-format off
template<typename Func, typename... Args>
concept VoidFunction = requires(Func func, Args... args)
{
    {
        func(args...)
    }
    -> SameAs<void>;
};

template<typename Func, typename... Args>
concept IteratorFunction = requires(Func func, Args... args)
{
    {
        func(args...)
    }
    -> SameAs<IterationDecision>;
};
// clang-format on
}

using AK::Concepts::Arithmetic;
using AK::Concepts::Enum;
using AK::Concepts::FloatingPoint;
using AK::Concepts::Integral;
using AK::Concepts::IteratorFunction;
using AK::Concepts::Signed;
using AK::Concepts::Unsigned;
using AK::Concepts::VoidFunction;
