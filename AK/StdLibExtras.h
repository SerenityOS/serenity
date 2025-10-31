/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic ignored "-Wunqualified-std-cast-call"
#endif

#include <AK/StdLibExtraDetails.h>
#include <AK/StdShim.h>

#include <AK/Assertions.h>

namespace AK {

template<typename T, typename U>
constexpr auto round_up_to_power_of_two(T value, U power_of_two)
requires(AK::Detail::IsIntegral<T> && AK::Detail::IsIntegral<U>)
{
    return ((value - 1) & ~(power_of_two - 1)) + power_of_two;
}

template<typename T>
constexpr bool is_power_of_two(T value)
requires(AK::Detail::IsIntegral<T>)
{
    return value && !((value) & (value - 1));
}

template<typename... Args>
void compiletime_fail(Args...);

}

#if !USING_AK_GLOBALLY || defined(AK_DONT_REPLACE_STD)
#    define AK_REPLACED_STD_NAMESPACE AK::replaced_std
#else
#    define AK_REPLACED_STD_NAMESPACE std
#endif

namespace AK::Detail {
template<typename T>
struct _RawPtr {
    using Type = T*;
};
}

namespace AK {

struct Empty {
    constexpr bool operator==(Empty const&) const = default;
};

template<typename T, typename SizeType = decltype(sizeof(T)), SizeType N>
constexpr SizeType array_size(T (&)[N])
{
    return N;
}

template<typename T>
constexpr T min(T const& a, IdentityType<T> const& b)
{
    return b < a ? b : a;
}

template<typename T>
constexpr T max(T const& a, IdentityType<T> const& b)
{
    return a < b ? b : a;
}

template<typename T>
constexpr T clamp(T const& value, IdentityType<T> const& min, IdentityType<T> const& max)
{
    VERIFY(max >= min);
    if (value > max)
        return max;
    if (value < min)
        return min;
    return value;
}

template<typename T, typename U>
constexpr T mix(T const& v1, T const& v2, U const& interpolation) // aka lerp
{
    return v1 + (v2 - v1) * interpolation;
}

template<typename T>
constexpr T mod(T const& a, IdentityType<T> const& b)
{
    VERIFY(b != 0);
    return (a % b + b) % b;
}

template<typename T, typename U>
constexpr T ceil_div(T a, U b)
{
    T result = a / b;
    if ((a % b) != 0 && (a > 0) == (b > 0))
        ++result;
    return result;
}

template<typename T, typename U>
constexpr T floor_div(T a, U b)
{
    T result = a / b;
    if ((a % b) != 0 && (a > 0) != (b > 0))
        --result;
    return result;
}

template<typename T, typename U>
inline void swap(T& a, U& b)
{
    if (&a == &b)
        return;
    U tmp = move(static_cast<U&>(a));
    a = static_cast<T&&>(move(b));
    b = move(tmp);
}

template<typename T, typename U = T>
constexpr T exchange(T& slot, U&& value)
{
    T old_value = move(slot);
    slot = AK::forward<U>(value);
    return old_value;
}

template<typename T>
using RawPtr = typename Detail::_RawPtr<T>::Type;

template<typename V>
constexpr decltype(auto) to_underlying(V value)
requires(IsEnum<V>)
{
    return static_cast<UnderlyingType<V>>(value);
}

constexpr bool is_constant_evaluated()
{
#if __has_builtin(__builtin_is_constant_evaluated)
    return __builtin_is_constant_evaluated();
#else
    return false;
#endif
}

template<typename T>
ALWAYS_INLINE constexpr void taint_for_optimizer(T& value)
requires(IsIntegral<T>)
{
    if (!is_constant_evaluated()) {
        asm volatile(""
            : "+r"(value));
    }
}

template<typename T>
ALWAYS_INLINE constexpr void taint_for_optimizer(T& value)
requires(!IsIntegral<T>)
{
    if (!is_constant_evaluated()) {
        asm volatile(""
            :
            : "m"(value)
            : "memory");
    }
}

// These can't be exported into the global namespace as they would clash with the C standard library.

#define __DEFINE_GENERIC_ABS(type, zero, intrinsic) \
    constexpr type abs(type num)                    \
    {                                               \
        if (is_constant_evaluated())                \
            return num < (zero) ? -num : num;       \
        return __builtin_##intrinsic(num);          \
    }

__DEFINE_GENERIC_ABS(int, 0, abs);
__DEFINE_GENERIC_ABS(long, 0L, labs);
__DEFINE_GENERIC_ABS(long long, 0LL, llabs);
#ifndef KERNEL
__DEFINE_GENERIC_ABS(float, 0.0F, fabsf);
__DEFINE_GENERIC_ABS(double, 0.0, fabs);
__DEFINE_GENERIC_ABS(long double, 0.0L, fabsl);
#endif

#undef __DEFINE_GENERIC_ABS

}

#if USING_AK_GLOBALLY
using AK::array_size;
using AK::ceil_div;
using AK::clamp;
using AK::Empty;
using AK::exchange;
using AK::floor_div;
using AK::forward;
using AK::is_constant_evaluated;
using AK::is_power_of_two;
using AK::max;
using AK::min;
using AK::mix;
using AK::mod;
using AK::move;
using AK::RawPtr;
using AK::round_up_to_power_of_two;
using AK::swap;
using AK::to_underlying;
#endif
