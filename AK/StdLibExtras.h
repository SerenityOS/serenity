/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>

#include <AK/Assertions.h>

template<typename T, typename U>
constexpr auto round_up_to_power_of_two(T value, U power_of_two) requires(IsIntegral<T>&& IsIntegral<U>)
{
    return ((value - 1) & ~(power_of_two - 1)) + power_of_two;
}

namespace std {

// NOTE: This is in the "std" namespace since some compiler features rely on it.

template<typename T>
constexpr T&& move(T& arg)
{
    return static_cast<T&&>(arg);
}

}

using std::move;

namespace AK::Detail {
template<typename T>
struct _RawPtr {
    using Type = T*;
};
}

namespace AK {

template<class T>
constexpr T&& forward(RemoveReference<T>& param)
{
    return static_cast<T&&>(param);
}

template<class T>
constexpr T&& forward(RemoveReference<T>&& param) noexcept
{
    static_assert(!IsLvalueReference<T>, "Can't forward an rvalue as an lvalue.");
    return static_cast<T&&>(param);
}

template<typename T, typename SizeType = decltype(sizeof(T)), SizeType N>
constexpr SizeType array_size(T (&)[N])
{
    return N;
}

template<typename T>
constexpr T min(const T& a, const T& b)
{
    return b < a ? b : a;
}

template<typename T>
constexpr T max(const T& a, const T& b)
{
    return a < b ? b : a;
}

template<typename T>
constexpr T clamp(const T& value, const T& min, const T& max)
{
    VERIFY(max >= min);
    if (value > max)
        return max;
    if (value < min)
        return min;
    return value;
}

template<typename T, typename U>
constexpr T ceil_div(T a, U b)
{
    static_assert(sizeof(T) == sizeof(U));
    T result = a / b;
    if ((a % b) != 0)
        ++result;
    return result;
}

template<typename T, typename U>
inline void swap(T& a, U& b)
{
    U tmp = move((U&)a);
    a = (T &&) move(b);
    b = move(tmp);
}

template<typename T, typename U = T>
constexpr T exchange(T& slot, U&& value)
{
    T old_value = move(slot);
    slot = forward<U>(value);
    return old_value;
}

template<typename T>
using RawPtr = typename Detail::_RawPtr<T>::Type;

}

using AK::array_size;
using AK::ceil_div;
using AK::clamp;
using AK::exchange;
using AK::forward;
using AK::max;
using AK::min;
using AK::RawPtr;
using AK::swap;
