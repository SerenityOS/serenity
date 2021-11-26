/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AK::Detail {

template<bool B, class T = void>
struct EnableIf {
};

template<class T>
struct EnableIf<true, T> {
    using Type = T;
};

template<class T, T v>
struct IntegralConstant {
    static constexpr T value = v;
    using ValueType = T;
    using Type = IntegralConstant;
    constexpr operator ValueType() const { return value; }
    constexpr ValueType operator()() const { return value; }
};

using FalseType = IntegralConstant<bool, false>;
using TrueType = IntegralConstant<bool, true>;

template<class T>
using AddConst = const T;

template<class T>
struct __RemoveConst {
    using Type = T;
};
template<class T>
struct __RemoveConst<const T> {
    using Type = T;
};
template<class T>
using RemoveConst = typename __RemoveConst<T>::Type;

template<class T>
struct __RemoveVolatile {
    using Type = T;
};

template<class T>
struct __RemoveVolatile<volatile T> {
    using Type = T;
};

template<typename T>
using RemoveVolatile = typename __RemoveVolatile<T>::Type;

template<class T>
using RemoveCV = RemoveVolatile<RemoveConst<T>>;

template<typename...>
using VoidType = void;

template<class T>
inline constexpr bool IsLvalueReference = false;

template<class T>
inline constexpr bool IsLvalueReference<T&> = true;

template<class T>
inline constexpr bool __IsPointerHelper = false;

template<class T>
inline constexpr bool __IsPointerHelper<T*> = true;

template<class T>
inline constexpr bool IsPointer = __IsPointerHelper<RemoveCV<T>>;

template<class>
inline constexpr bool IsFunction = false;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...)> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...)> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) volatile> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) volatile> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const volatile> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const volatile> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...)&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...)&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) volatile&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) volatile&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const volatile&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const volatile&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) &&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) &&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const&&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const&&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) volatile&&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) volatile&&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args...) const volatile&&> = true;
template<class Ret, class... Args>
inline constexpr bool IsFunction<Ret(Args..., ...) const volatile&&> = true;

template<class T>
inline constexpr bool IsRvalueReference = false;
template<class T>
inline constexpr bool IsRvalueReference<T&&> = true;

template<class T>
struct __RemovePointer {
    using Type = T;
};
template<class T>
struct __RemovePointer<T*> {
    using Type = T;
};
template<class T>
struct __RemovePointer<T* const> {
    using Type = T;
};
template<class T>
struct __RemovePointer<T* volatile> {
    using Type = T;
};
template<class T>
struct __RemovePointer<T* const volatile> {
    using Type = T;
};
template<typename T>
using RemovePointer = typename __RemovePointer<T>::Type;

template<typename T, typename U>
inline constexpr bool IsSame = false;

template<typename T>
inline constexpr bool IsSame<T, T> = true;

template<bool condition, class TrueType, class FalseType>
struct __Conditional {
    using Type = TrueType;
};

template<class TrueType, class FalseType>
struct __Conditional<false, TrueType, FalseType> {
    using Type = FalseType;
};

template<bool condition, class TrueType, class FalseType>
using Conditional = typename __Conditional<condition, TrueType, FalseType>::Type;

template<typename T>
inline constexpr bool IsNullPointer = IsSame<decltype(nullptr), RemoveCV<T>>;

template<typename T>
struct __RemoveReference {
    using Type = T;
};
template<class T>
struct __RemoveReference<T&> {
    using Type = T;
};
template<class T>
struct __RemoveReference<T&&> {
    using Type = T;
};

template<typename T>
using RemoveReference = typename __RemoveReference<T>::Type;

template<typename T>
using RemoveCVReference = RemoveCV<RemoveReference<T>>;

template<typename T>
struct __MakeUnsigned {
    using Type = void;
};
template<>
struct __MakeUnsigned<signed char> {
    using Type = unsigned char;
};
template<>
struct __MakeUnsigned<short> {
    using Type = unsigned short;
};
template<>
struct __MakeUnsigned<int> {
    using Type = unsigned int;
};
template<>
struct __MakeUnsigned<long> {
    using Type = unsigned long;
};
template<>
struct __MakeUnsigned<long long> {
    using Type = unsigned long long;
};
template<>
struct __MakeUnsigned<unsigned char> {
    using Type = unsigned char;
};
template<>
struct __MakeUnsigned<unsigned short> {
    using Type = unsigned short;
};
template<>
struct __MakeUnsigned<unsigned int> {
    using Type = unsigned int;
};
template<>
struct __MakeUnsigned<unsigned long> {
    using Type = unsigned long;
};
template<>
struct __MakeUnsigned<unsigned long long> {
    using Type = unsigned long long;
};
template<>
struct __MakeUnsigned<char> {
    using Type = unsigned char;
};
template<>
struct __MakeUnsigned<char8_t> {
    using Type = char8_t;
};
template<>
struct __MakeUnsigned<char16_t> {
    using Type = char16_t;
};
template<>
struct __MakeUnsigned<char32_t> {
    using Type = char32_t;
};
template<>
struct __MakeUnsigned<bool> {
    using Type = bool;
};

template<typename T>
using MakeUnsigned = typename __MakeUnsigned<T>::Type;

template<typename T>
struct __MakeSigned {
    using Type = void;
};
template<>
struct __MakeSigned<signed char> {
    using Type = signed char;
};
template<>
struct __MakeSigned<short> {
    using Type = short;
};
template<>
struct __MakeSigned<int> {
    using Type = int;
};
template<>
struct __MakeSigned<long> {
    using Type = long;
};
template<>
struct __MakeSigned<long long> {
    using Type = long long;
};
template<>
struct __MakeSigned<unsigned char> {
    using Type = char;
};
template<>
struct __MakeSigned<unsigned short> {
    using Type = short;
};
template<>
struct __MakeSigned<unsigned int> {
    using Type = int;
};
template<>
struct __MakeSigned<unsigned long> {
    using Type = long;
};
template<>
struct __MakeSigned<unsigned long long> {
    using Type = long long;
};
template<>
struct __MakeSigned<char> {
    using Type = char;
};

template<typename T>
using MakeSigned = typename __MakeSigned<T>::Type;

template<typename T>
auto declval() -> T;

template<typename...>
struct __CommonType;

template<typename T>
struct __CommonType<T> {
    using Type = T;
};

template<typename T1, typename T2>
struct __CommonType<T1, T2> {
    using Type = decltype(true ? declval<T1>() : declval<T2>());
};

template<typename T1, typename T2, typename... Ts>
struct __CommonType<T1, T2, Ts...> {
    using Type = typename __CommonType<typename __CommonType<T1, T2>::Type, Ts...>::Type;
};

template<typename... Ts>
using CommonType = typename __CommonType<Ts...>::Type;

template<class T>
inline constexpr bool IsVoid = IsSame<void, RemoveCV<T>>;

template<class T>
inline constexpr bool IsConst = false;

template<class T>
inline constexpr bool IsConst<const T> = true;

template<typename T>
inline constexpr bool IsEnum = __is_enum(T);

template<typename T>
inline constexpr bool IsUnion = __is_union(T);

template<typename T>
inline constexpr bool IsClass = __is_class(T);

template<typename Base, typename Derived>
inline constexpr bool IsBaseOf = __is_base_of(Base, Derived);

template<typename T>
inline constexpr bool __IsIntegral = false;

template<>
inline constexpr bool __IsIntegral<bool> = true;
template<>
inline constexpr bool __IsIntegral<unsigned char> = true;
template<>
inline constexpr bool __IsIntegral<char8_t> = true;
template<>
inline constexpr bool __IsIntegral<char16_t> = true;
template<>
inline constexpr bool __IsIntegral<char32_t> = true;
template<>
inline constexpr bool __IsIntegral<unsigned short> = true;
template<>
inline constexpr bool __IsIntegral<unsigned int> = true;
template<>
inline constexpr bool __IsIntegral<unsigned long> = true;
template<>
inline constexpr bool __IsIntegral<unsigned long long> = true;

template<typename T>
inline constexpr bool IsIntegral = __IsIntegral<MakeUnsigned<RemoveCV<T>>>;

template<typename T>
inline constexpr bool __IsFloatingPoint = false;
template<>
inline constexpr bool __IsFloatingPoint<float> = true;
template<>
inline constexpr bool __IsFloatingPoint<double> = true;
template<>
inline constexpr bool __IsFloatingPoint<long double> = true;

template<typename T>
inline constexpr bool IsFloatingPoint = __IsFloatingPoint<RemoveCV<T>>;

template<typename ReferenceType, typename T>
using CopyConst = Conditional<IsConst<ReferenceType>, AddConst<T>, RemoveConst<T>>;

template<typename... Ts>
using Void = void;

template<typename... _Ignored>
constexpr auto DependentFalse = false;

template<typename T>
inline constexpr bool IsSigned = IsSame<T, MakeSigned<T>>;

template<typename T>
inline constexpr bool IsUnsigned = IsSame<T, MakeUnsigned<T>>;

template<typename T>
inline constexpr bool IsArithmetic = IsIntegral<T> || IsFloatingPoint<T>;

template<typename T>
inline constexpr bool IsFundamental = IsArithmetic<T> || IsVoid<T> || IsNullPointer<T>;

template<typename T, T... Ts>
struct IntegerSequence {
    using Type = T;
    static constexpr unsigned size() noexcept { return sizeof...(Ts); };
};

template<unsigned... Indices>
using IndexSequence = IntegerSequence<unsigned, Indices...>;

template<typename T, T N, T... Ts>
auto make_integer_sequence_impl()
{
    if constexpr (N == 0)
        return IntegerSequence<T, Ts...> {};
    else
        return make_integer_sequence_impl<T, N - 1, N - 1, Ts...>();
}

template<typename T, T N>
using MakeIntegerSequence = decltype(make_integer_sequence_impl<T, N>());

template<unsigned N>
using MakeIndexSequence = MakeIntegerSequence<unsigned, N>;

template<typename T>
struct __IdentityType {
    using Type = T;
};

template<typename T>
using IdentityType = typename __IdentityType<T>::Type;

template<typename T, typename = void>
struct __AddReference {
    using LvalueType = T;
    using TvalueType = T;
};

template<typename T>
struct __AddReference<T, VoidType<T&>> {
    using LvalueType = T&;
    using RvalueType = T&&;
};

template<typename T>
using AddLvalueReference = typename __AddReference<T>::LvalueType;

template<typename T>
using AddRvalueReference = typename __AddReference<T>::RvalueType;

template<class T>
requires(IsEnum<T>) using UnderlyingType = __underlying_type(T);

template<typename T, unsigned ExpectedSize, unsigned ActualSize>
struct __AssertSize : TrueType {
    static_assert(ActualSize == ExpectedSize,
        "actual size does not match expected size");

    consteval explicit operator bool() const { return value; }
};

// Note: This type is useful, as the sizes will be visible in the
//       compiler error messages, as they will be part of the
//       template parameters. This is not possible with a
//       static_assert on the sizeof a type.
template<typename T, unsigned ExpectedSize>
using AssertSize = __AssertSize<T, ExpectedSize, sizeof(T)>;

template<typename T>
inline constexpr bool IsPOD = __is_pod(T);

template<typename T>
inline constexpr bool IsTrivial = __is_trivial(T);

template<typename T>
inline constexpr bool IsTriviallyCopyable = __is_trivially_copyable(T);

template<typename T, typename... Args>
inline constexpr bool IsCallableWithArguments = requires(T t) { t(declval<Args>()...); };

template<typename T, typename... Args>
inline constexpr bool IsConstructible = requires { ::new T(declval<Args>()...); };

template<typename T, typename... Args>
inline constexpr bool IsTriviallyConstructible = __is_trivially_constructible(T, Args...);

template<typename From, typename To>
inline constexpr bool IsConvertible = requires { declval<void (*)(To)>()(declval<From>()); };

template<typename T, typename U>
inline constexpr bool IsAssignable = requires { declval<T>() = declval<U>(); };

template<typename T, typename U>
inline constexpr bool IsTriviallyAssignable = __is_trivially_assignable(T, U);

template<typename T>
inline constexpr bool IsDestructible = requires { declval<T>().~T(); };

template<typename T>
#if defined(__clang__)
inline constexpr bool IsTriviallyDestructible = __is_trivially_destructible(T);
#else
inline constexpr bool IsTriviallyDestructible = __has_trivial_destructor(T) && IsDestructible<T>;
#endif

template<typename T>
inline constexpr bool IsCopyConstructible = IsConstructible<T, AddLvalueReference<AddConst<T>>>;

template<typename T>
inline constexpr bool IsTriviallyCopyConstructible = IsTriviallyConstructible<T, AddLvalueReference<AddConst<T>>>;

template<typename T>
inline constexpr bool IsCopyAssignable = IsAssignable<AddLvalueReference<T>, AddLvalueReference<AddConst<T>>>;

template<typename T>
inline constexpr bool IsTriviallyCopyAssignable = IsTriviallyAssignable<AddLvalueReference<T>, AddLvalueReference<AddConst<T>>>;

template<typename T>
inline constexpr bool IsMoveConstructible = IsConstructible<T, AddRvalueReference<T>>;

template<typename T>
inline constexpr bool IsTriviallyMoveConstructible = IsTriviallyConstructible<T, AddRvalueReference<T>>;

template<typename T>
inline constexpr bool IsMoveAssignable = IsAssignable<AddLvalueReference<T>, AddRvalueReference<T>>;

template<typename T>
inline constexpr bool IsTriviallyMoveAssignable = IsTriviallyAssignable<AddLvalueReference<T>, AddRvalueReference<T>>;

template<typename T, template<typename...> typename U>
inline constexpr bool IsSpecializationOf = false;

template<template<typename...> typename U, typename... Us>
inline constexpr bool IsSpecializationOf<U<Us...>, U> = true;

}
using AK::Detail::AddConst;
using AK::Detail::AddLvalueReference;
using AK::Detail::AddRvalueReference;
using AK::Detail::AssertSize;
using AK::Detail::CommonType;
using AK::Detail::Conditional;
using AK::Detail::CopyConst;
using AK::Detail::declval;
using AK::Detail::DependentFalse;
using AK::Detail::EnableIf;
using AK::Detail::FalseType;
using AK::Detail::IdentityType;
using AK::Detail::IndexSequence;
using AK::Detail::IntegerSequence;
using AK::Detail::IsArithmetic;
using AK::Detail::IsAssignable;
using AK::Detail::IsBaseOf;
using AK::Detail::IsCallableWithArguments;
using AK::Detail::IsClass;
using AK::Detail::IsConst;
using AK::Detail::IsConstructible;
using AK::Detail::IsConvertible;
using AK::Detail::IsCopyAssignable;
using AK::Detail::IsCopyConstructible;
using AK::Detail::IsDestructible;
using AK::Detail::IsEnum;
using AK::Detail::IsFloatingPoint;
using AK::Detail::IsFunction;
using AK::Detail::IsFundamental;
using AK::Detail::IsIntegral;
using AK::Detail::IsLvalueReference;
using AK::Detail::IsMoveAssignable;
using AK::Detail::IsMoveConstructible;
using AK::Detail::IsNullPointer;
using AK::Detail::IsPOD;
using AK::Detail::IsPointer;
using AK::Detail::IsRvalueReference;
using AK::Detail::IsSame;
using AK::Detail::IsSigned;
using AK::Detail::IsSpecializationOf;
using AK::Detail::IsTrivial;
using AK::Detail::IsTriviallyAssignable;
using AK::Detail::IsTriviallyConstructible;
using AK::Detail::IsTriviallyCopyable;
using AK::Detail::IsTriviallyCopyAssignable;
using AK::Detail::IsTriviallyCopyConstructible;
using AK::Detail::IsTriviallyDestructible;
using AK::Detail::IsTriviallyMoveAssignable;
using AK::Detail::IsTriviallyMoveConstructible;
using AK::Detail::IsUnion;
using AK::Detail::IsUnsigned;
using AK::Detail::IsVoid;
using AK::Detail::MakeIndexSequence;
using AK::Detail::MakeIntegerSequence;
using AK::Detail::MakeSigned;
using AK::Detail::MakeUnsigned;
using AK::Detail::RemoveConst;
using AK::Detail::RemoveCV;
using AK::Detail::RemoveCVReference;
using AK::Detail::RemovePointer;
using AK::Detail::RemoveReference;
using AK::Detail::RemoveVolatile;
using AK::Detail::TrueType;
using AK::Detail::UnderlyingType;
using AK::Detail::Void;
