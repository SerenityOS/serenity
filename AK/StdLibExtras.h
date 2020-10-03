/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#define UNUSED_PARAM(x) (void)x

inline constexpr unsigned round_up_to_power_of_two(unsigned value, unsigned power_of_two)
{
    return ((value - 1) & ~(power_of_two - 1)) + power_of_two;
}

namespace AK {

template<typename T>
auto declval() -> T;

template<typename T, typename SizeType = decltype(sizeof(T)), SizeType N>
constexpr SizeType array_size(T (&)[N])
{
    return N;
}

template<typename T>
inline constexpr T min(const T& a, const T& b)
{
    return b < a ? b : a;
}

template<typename T>
inline constexpr T max(const T& a, const T& b)
{
    return a < b ? b : a;
}

template<typename T>
inline constexpr T clamp(const T& value, const T& min, const T& max)
{
    ASSERT(max >= min);
    if (value > max)
        return max;
    if (value < min)
        return min;
    return value;
}

template<typename T, typename U>
inline constexpr T ceil_div(T a, U b)
{
    static_assert(sizeof(T) == sizeof(U));
    T result = a / b;
    if ((a % b) != 0)
        ++result;
    return result;
}

#ifdef __clang__
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wconsumed"
#endif
template<typename T>
inline T&& move(T& arg)
{
    return static_cast<T&&>(arg);
}
#ifdef __clang__
#    pragma clang diagnostic pop
#endif

template<typename T, typename U>
inline void swap(T& a, U& b)
{
    U tmp = move((U&)a);
    a = (T &&) move(b);
    b = move(tmp);
}

template<bool B, class T = void>
struct EnableIf {
};

template<class T>
struct EnableIf<true, T> {
    typedef T Type;
};

template<class T>
struct AddConst {
    typedef const T Type;
};

template<class T>
struct RemoveConst {
    typedef T Type;
};

template<class T>
struct RemoveConst<const T> {
    typedef T Type;
};

template<class T>
struct RemoveVolatile {
    typedef T Type;
};
template<class T>
struct RemoveVolatile<volatile T> {
    typedef T Type;
};
template<class T>
struct RemoveCV {
    typedef typename RemoveVolatile<typename RemoveConst<T>::Type>::Type Type;
};

template<class T, T v>
struct IntegralConstant {
    static constexpr T value = v;
    typedef T ValueType;
    typedef IntegralConstant Type;
    constexpr operator ValueType() const { return value; }
    constexpr ValueType operator()() const { return value; }
};

typedef IntegralConstant<bool, false> FalseType;
typedef IntegralConstant<bool, true> TrueType;

template<class T>
struct IsLvalueReference : FalseType {
};

template<class T>
struct IsLvalueReference<T&> : TrueType {
};

template<class T>
struct __IsPointerHelper : FalseType {
};

template<class T>
struct __IsPointerHelper<T*> : TrueType {
};

template<class T>
struct IsPointer : __IsPointerHelper<typename RemoveCV<T>::Type> {
};

template<class>
struct IsFunction : FalseType {
};

template<class Ret, class... Args>
struct IsFunction<Ret(Args...)> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...)> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) volatile> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) volatile> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const volatile> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const volatile> : TrueType {
};

template<class Ret, class... Args>
struct IsFunction<Ret(Args...)&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...)&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) volatile&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) volatile&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const volatile&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const volatile&> : TrueType {
};

template<class Ret, class... Args>
struct IsFunction<Ret(Args...) &&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) &&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const&&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const&&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) volatile&&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) volatile&&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args...) const volatile&&> : TrueType {
};
template<class Ret, class... Args>
struct IsFunction<Ret(Args..., ...) const volatile&&> : TrueType {
};

template<class T>
struct IsRvalueReference : FalseType {
};
template<class T>
struct IsRvalueReference<T&&> : TrueType {
};

template<class T>
struct RemovePointer {
    typedef T Type;
};
template<class T>
struct RemovePointer<T*> {
    typedef T Type;
};
template<class T>
struct RemovePointer<T* const> {
    typedef T Type;
};
template<class T>
struct RemovePointer<T* volatile> {
    typedef T Type;
};
template<class T>
struct RemovePointer<T* const volatile> {
    typedef T Type;
};

template<typename T, typename U>
struct IsSame {
    enum {
        value = 0
    };
};

template<typename T>
struct IsSame<T, T> {
    enum {
        value = 1
    };
};

template<bool condition, class TrueType, class FalseType>
struct Conditional {
    typedef TrueType Type;
};

template<class TrueType, class FalseType>
struct Conditional<false, TrueType, FalseType> {
    typedef FalseType Type;
};

template<typename T>
struct RemoveReference {
    typedef T Type;
};
template<class T>
struct RemoveReference<T&> {
    typedef T Type;
};
template<class T>
struct RemoveReference<T&&> {
    typedef T Type;
};

template<class T>
inline constexpr T&& forward(typename RemoveReference<T>::Type& param)
{
    return static_cast<T&&>(param);
}

template<class T>
inline constexpr T&& forward(typename RemoveReference<T>::Type&& param) noexcept
{
    static_assert(!IsLvalueReference<T>::value, "Can't forward an rvalue as an lvalue.");
    return static_cast<T&&>(param);
}

template<typename T>
struct MakeUnsigned {
};
template<>
struct MakeUnsigned<signed char> {
    typedef unsigned char Type;
};
template<>
struct MakeUnsigned<short> {
    typedef unsigned short Type;
};
template<>
struct MakeUnsigned<int> {
    typedef unsigned Type;
};
template<>
struct MakeUnsigned<long> {
    typedef unsigned long Type;
};
template<>
struct MakeUnsigned<long long> {
    typedef unsigned long long Type;
};
template<>
struct MakeUnsigned<unsigned char> {
    typedef unsigned char Type;
};
template<>
struct MakeUnsigned<unsigned short> {
    typedef unsigned short Type;
};
template<>
struct MakeUnsigned<unsigned int> {
    typedef unsigned Type;
};
template<>
struct MakeUnsigned<unsigned long> {
    typedef unsigned long Type;
};
template<>
struct MakeUnsigned<unsigned long long> {
    typedef unsigned long long Type;
};
template<>
struct MakeUnsigned<char> {
    typedef unsigned char Type;
};

template<typename T>
struct MakeSigned {
};
template<>
struct MakeSigned<signed char> {
    typedef signed char Type;
};
template<>
struct MakeSigned<short> {
    typedef short Type;
};
template<>
struct MakeSigned<int> {
    typedef int Type;
};
template<>
struct MakeSigned<long> {
    typedef long Type;
};
template<>
struct MakeSigned<long long> {
    typedef long long Type;
};
template<>
struct MakeSigned<unsigned char> {
    typedef char Type;
};
template<>
struct MakeSigned<unsigned short> {
    typedef short Type;
};
template<>
struct MakeSigned<unsigned int> {
    typedef int Type;
};
template<>
struct MakeSigned<unsigned long> {
    typedef long Type;
};
template<>
struct MakeSigned<unsigned long long> {
    typedef long long Type;
};
template<>
struct MakeSigned<char> {
    typedef signed char Type;
};

template<class T>
struct IsVoid : IsSame<void, typename RemoveCV<T>::Type> {
};

template<class T>
struct IsConst : FalseType {
};

template<class T>
struct IsConst<const T> : TrueType {
};

template<typename T, typename U = T>
inline constexpr T exchange(T& slot, U&& value)
{
    T old_value = move(slot);
    slot = forward<U>(value);
    return old_value;
}

template<typename T>
struct IsUnion : public IntegralConstant<bool, __is_union(T)> {
};

template<typename T>
struct IsClass : public IntegralConstant<bool, __is_class(T)> {
};

template<typename Base, typename Derived>
struct IsBaseOf : public IntegralConstant<bool, __is_base_of(Base, Derived)> {
};

template<typename T>
constexpr bool is_trivial()
{
    return __is_trivial(T);
}

template<typename T>
constexpr bool is_trivially_copyable()
{
    return __is_trivially_copyable(T);
}

template<typename T>
struct __IsIntegral : FalseType {
};
template<>
struct __IsIntegral<unsigned char> : TrueType {
};
template<>
struct __IsIntegral<unsigned short> : TrueType {
};
template<>
struct __IsIntegral<unsigned int> : TrueType {
};
template<>
struct __IsIntegral<unsigned long> : TrueType {
};
template<>
struct __IsIntegral<unsigned long long> : TrueType {
};
template<typename T>
using IsIntegral = __IsIntegral<typename MakeUnsigned<typename RemoveCV<T>::Type>::Type>;

template<typename T>
struct __IsFloatingPoint : FalseType {
};
template<>
struct __IsFloatingPoint<float> : TrueType {
};
template<>
struct __IsFloatingPoint<double> : TrueType {
};
template<typename T>
using IsFloatingPoint = __IsFloatingPoint<typename RemoveCV<T>::Type>;

template<typename ReferenceType, typename T>
using CopyConst =
    typename Conditional<IsConst<ReferenceType>::value, typename AddConst<T>::Type, typename RemoveConst<T>::Type>::Type;

template<typename... Ts>
using Void = void;

template<typename... _Ignored>
inline constexpr auto DependentFalse = false;

}

using AK::AddConst;
using AK::array_size;
using AK::ceil_div;
using AK::clamp;
using AK::Conditional;
using AK::declval;
using AK::DependentFalse;
using AK::exchange;
using AK::forward;
using AK::is_trivial;
using AK::is_trivially_copyable;
using AK::IsBaseOf;
using AK::IsClass;
using AK::IsConst;
using AK::IsSame;
using AK::IsUnion;
using AK::IsVoid;
using AK::MakeSigned;
using AK::MakeUnsigned;
using AK::max;
using AK::min;
using AK::move;
using AK::RemoveConst;
using AK::swap;
using AK::Void;
