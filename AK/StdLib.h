#pragma once

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

}

using AK::min;
using AK::max;
using AK::ceilDiv;

