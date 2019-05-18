#pragma once

namespace AK {

inline void timeval_sub(const struct timeval* a, const struct timeval* b, struct timeval* result)
{
    result->tv_sec = a->tv_sec - b->tv_sec;
    result->tv_usec = a->tv_usec - b->tv_usec;
    if (result->tv_usec < 0) {
        --result->tv_sec;
        result->tv_usec += 1000000;
    }
}

inline void timeval_add(const struct timeval* a, const struct timeval* b, struct timeval* result)
{
    result->tv_sec = a->tv_sec + b->tv_sec;
    result->tv_usec = a->tv_usec + b->tv_usec;
    if (result->tv_usec > 1000000) {
        ++result->tv_sec;
        result->tv_usec -= 1000000;
    }
}

}
