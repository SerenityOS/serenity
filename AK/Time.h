#pragma once

namespace AK {

template<typename TimevalType>
inline void timeval_sub(const TimevalType* a, const TimevalType* b, TimevalType* result)
{
    result->tv_sec = a->tv_sec - b->tv_sec;
    result->tv_usec = a->tv_usec - b->tv_usec;
    if (result->tv_usec < 0) {
        --result->tv_sec;
        result->tv_usec += 1000000;
    }
}

template<typename TimevalType>
inline void timeval_add(const TimevalType* a, const TimevalType* b, TimevalType* result)
{
    result->tv_sec = a->tv_sec + b->tv_sec;
    result->tv_usec = a->tv_usec + b->tv_usec;
    if (result->tv_usec > 1000000) {
        ++result->tv_sec;
        result->tv_usec -= 1000000;
    }
}

}
