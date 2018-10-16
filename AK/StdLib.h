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

template<typename T>
struct identity {
    typedef T type;
};
template<typename T>
T&& forward(typename identity<T>::type&& param)
{
    return static_cast<typename identity<T>::type&&>(param);
}

template<typename T, typename U>
T exchange(T& a, U&& b)
{
    T tmp = move(a);
    a = move(b);
    return tmp;
}


}

using AK::min;
using AK::max;
using AK::move;
using AK::forward;
using AK::exchange;
using AK::ceilDiv;

