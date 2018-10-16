#pragma once

#ifdef SERENITY_KERNEL
#include <Kernel/StdLib.h>
#else
#include <cstring>
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


template<typename T>
static inline T ceilDiv(T a, T b)
{
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
struct identity {
    typedef T type;
};
template<class T>
constexpr T&& forward(typename identity<T>::type& param)
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

}

using AK::min;
using AK::max;
using AK::move;
using AK::forward;
using AK::exchange;
using AK::swap;
using AK::ceilDiv;

