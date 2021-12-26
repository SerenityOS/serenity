#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define timerclear(tvp) (tvp)->tv_sec = (tvp)->tv_usec = 0
#define timerisset(tvp) ((tvp)->tv_sec || (tvp)->tv_usec)
#define timercmp(tvp, uvp, cmp) \
    (((tvp)->tv_sec == (uvp)->tv_sec) ? ((tvp)->tv_usec cmp(uvp)->tv_usec) : ((tvp)->tv_sec cmp(uvp)->tv_sec))
#define timeradd(a, b, out)                           \
    do {                                              \
        (out)->tv_sec = (a)->tv_sec + (b)->tv_sec;    \
        (out)->tv_usec = (a)->tv_usec + (b)->tv_usec; \
        if ((out)->tv_usec >= 1000000) {              \
            (out)->tv_sec++;                          \
            (out)->tv_usec -= 1000000;                \
        }                                             \
    } while (0)
#define timersub(a, b, out)                           \
    do {                                              \
        (out)->tv_sec = (a)->tv_sec - (b)->tv_sec;    \
        (out)->tv_usec = (a)->tv_usec - (b)->tv_usec; \
        if ((out)->tv_usec < 0) {                     \
            (out)->tv_sec--;                          \
            (out)->tv_usec += 1000000;                \
        }                                             \
    } while (0)

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

struct timezone {
    int tz_minuteswest;
    int tz_dsttime;
};

int gettimeofday(struct timeval* __restrict__, void* __restrict__) __attribute__((nonnull(1)));

__END_DECLS
