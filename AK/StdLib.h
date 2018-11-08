#pragma once

#ifdef SERENITY
#ifdef KERNEL
#include <Kernel/StdLib.h>
#else
#include <LibC/stdlib.h>
#include <LibC/string.h>
#endif
#else
#include <cstring>
#include <cstdlib>
#include <utility>
#endif

namespace AK {

template<typename T>
inline T min(const T& a, const T& b)
{
    return a < b ? a : b;
}

template<typename T>
inline T max(const T& a, const T& b)
{
    return a < b ? b : a;
}


template<typename T, typename U>
static inline T ceilDiv(T a, U b)
{
    static_assert(sizeof(T) == sizeof(U));
    T result = a / b;
    if ((a % b) != 0)
        ++result;
    return result;
}

template <typename T>
T&& move(T& arg)
{
    return static_cast<T&&>(arg);
}

template<typename T>
struct Identity {
    typedef T Type;
};

template<class T>
constexpr T&& forward(typename Identity<T>::Type& param)
{
    return static_cast<T&&>(param);
}

template<typename T, typename U>
T exchange(T& a, U&& b)
{
    T tmp = move(a);
    a = move(b);
    return tmp;
}

template<typename T, typename U>
void swap(T& a, U& b)
{
    U tmp = move((U&)a);
    a = (T&&)move(b);
    b = move(tmp);
}

template<bool B, class T = void>
struct EnableIf
{
};

template<class T>
struct EnableIf<true, T>
{
    typedef T Type;
};

template<class T> struct RemoveConst { typedef T Type; };
template<class T> struct RemoveConst<const T> { typedef T Type; };
template<class T> struct RemoveVolatile { typedef T Type; };
template<class T> struct RemoveVolatile<const T> { typedef T Type; };
template<class T> struct RemoveCV {
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
struct __IsPointerHelper : FalseType { };

template<class T>
struct __IsPointerHelper<T*> : TrueType { };

template<class T>
struct IsPointer : __IsPointerHelper<typename RemoveCV<T>::Type> { };

template<class> struct IsFunction : FalseType { };

template<class Ret, class... Args> struct IsFunction<Ret(Args...)> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......)> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) volatile> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) volatile> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const volatile> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const volatile> : TrueType { };

template<class Ret, class... Args> struct IsFunction<Ret(Args...) &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) volatile &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) volatile &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const volatile &> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const volatile &> : TrueType { };

template<class Ret, class... Args> struct IsFunction<Ret(Args...) &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) volatile &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) volatile &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args...) const volatile &&> : TrueType { };
template<class Ret, class... Args> struct IsFunction<Ret(Args......) const volatile &&> : TrueType { };

template<class T> struct IsRvalueReference : FalseType { };
template<class T> struct IsRvalueReference<T&&> : TrueType { };

template<class T> struct RemovePointer { typedef T Type; };
template<class T> struct RemovePointer<T*> { typedef T Type; };
template<class T> struct RemovePointer<T* const> { typedef T Type; };
template<class T> struct RemovePointer<T* volatile> { typedef T Type; };
template<class T> struct RemovePointer<T* const volatile> { typedef T Type; };

}

using AK::min;
using AK::max;
using AK::move;
using AK::forward;
using AK::exchange;
using AK::swap;
using AK::ceilDiv;

