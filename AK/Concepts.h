/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {
// Some basic forward delcarations
// We need them so we can #include this file in Forward.h, so we can use concepts in types forward declared there.
class StringView;
template<typename T>
class Span;
}

namespace AK::Concepts {

template<typename T>
concept Integral = IsIntegral<T>;

template<typename T>
concept FloatingPoint = IsFloatingPoint<T>;

template<typename T>
concept Fundamental = IsFundamental<T>;

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

template<typename U, typename... Ts>
concept OneOf = IsOneOf<U, Ts...>;

template<typename U, typename... Ts>
concept OneOfIgnoringCV = IsOneOfIgnoringCV<U, Ts...>;

template<typename T, template<typename...> typename S>
concept SpecializationOf = IsSpecializationOf<T, S>;

template<typename T>
concept AnyString = Detail::IsConstructible<StringView, T>;

template<typename T, typename U>
concept HashCompatible = IsHashCompatible<Detail::Decay<T>, Detail::Decay<U>>;

template<typename T>
concept Pointer = IsPointer<T>;

template<typename T>
concept IntegralLike = Integral<T> || Enum<T> || Pointer<T>;

// FIXME: remove once Clang formats these properly.
// clang-format off

// Any indexable, sized, contiguous data structure.
template<typename ArrayT, typename ContainedT, typename SizeT = size_t>
concept ArrayLike = requires(ArrayT array, SizeT index)
{
    {
        array[index]
    }
    -> SameAs<RemoveReference<ContainedT>&>;

    {
        array.size()
    }
    -> SameAs<SizeT>;

    {
        array.span()
    }
    -> SameAs<Span<RemoveReference<ContainedT>>>;

    {
        array.data()
    }
    -> SameAs<RemoveReference<ContainedT>*>;
};

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

template<typename T, typename EndT>
concept IteratorPairWith = requires(T it, EndT end)
{
    *it;
    { it != end } -> SameAs<bool>;
    ++it;
};

template<typename T>
concept IterableContainer = requires
{
    { declval<T>().begin() } -> IteratorPairWith<decltype(declval<T>().end())>;
};

// clang-format on
}

using AK::Concepts::Arithmetic;
using AK::Concepts::ArrayLike;
using AK::Concepts::Enum;
using AK::Concepts::FloatingPoint;
using AK::Concepts::Fundamental;
using AK::Concepts::Integral;
using AK::Concepts::IntegralLike;
using AK::Concepts::IterableContainer;
using AK::Concepts::IteratorFunction;
using AK::Concepts::IteratorPairWith;
using AK::Concepts::OneOf;
using AK::Concepts::OneOfIgnoringCV;
using AK::Concepts::Pointer;
using AK::Concepts::SameAs;
using AK::Concepts::Signed;
using AK::Concepts::SpecializationOf;
using AK::Concepts::Unsigned;
using AK::Concepts::VoidFunction;
