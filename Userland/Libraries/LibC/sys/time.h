/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/sys_time.h.html
#include <sys/select.h>

#include <Kernel/API/POSIX/sys/time.h>
#include <sys/cdefs.h>
#include <time.h>

__BEGIN_DECLS

struct timezone {
    int tz_minuteswest;
    int tz_dsttime;
};

int adjtime(const struct timeval* delta, struct timeval* old_delta);
int gettimeofday(struct timeval* __restrict__, void* __restrict__);
int settimeofday(struct timeval* __restrict__, void* __restrict__);
int utimes(char const* pathname, struct timeval const times[2]);
int lutimes(char const* pathname, struct timeval const times[2]);
int futimes(int fd, struct timeval const times[2]);

#define timeradd timeradd
#define timersub timersub
#define timerclear timerclear
#define timerisset timerisset
#define timercmp(tvp, uvp, cmp) \
    (((tvp)->tv_sec == (uvp)->tv_sec) ? ((tvp)->tv_usec cmp(uvp)->tv_usec) : ((tvp)->tv_sec cmp(uvp)->tv_sec))

static inline void timeradd(const struct timeval* a, const struct timeval* b, struct timeval* out)
{
    out->tv_sec = a->tv_sec + b->tv_sec;
    out->tv_usec = a->tv_usec + b->tv_usec;
    if (out->tv_usec >= 1000 * 1000) {
        out->tv_sec++;
        out->tv_usec -= 1000 * 1000;
    }
}

static inline void timersub(const struct timeval* a, const struct timeval* b, struct timeval* out)
{
    out->tv_sec = a->tv_sec - b->tv_sec;
    out->tv_usec = a->tv_usec - b->tv_usec;
    if (out->tv_usec < 0) {
        out->tv_sec--;
        out->tv_usec += 1000 * 1000;
    }
}

static inline void timerclear(struct timeval* out)
{
    out->tv_sec = out->tv_usec = 0;
}

static inline int timerisset(const struct timeval* tv)
{
    return tv->tv_sec || tv->tv_usec;
}

#define timespecadd timespecadd
#define timespecsub timespecsub
#define timespecclear timespecclear
#define timespecisset timespecisset
#define timespeccmp(ts, us, cmp) \
    (((ts)->tv_sec == (us)->tv_sec) ? ((ts)->tv_nsec cmp(us)->tv_nsec) : ((ts)->tv_sec cmp(us)->tv_sec))

static inline void timespecadd(const struct timespec* a, const struct timespec* b, struct timespec* out)
{
    out->tv_sec = a->tv_sec + b->tv_sec;
    out->tv_nsec = a->tv_nsec + b->tv_nsec;
    if (out->tv_nsec >= 1000 * 1000 * 1000) {
        out->tv_sec++;
        out->tv_nsec -= 1000 * 1000 * 1000;
    }
}

static inline void timespecsub(const struct timespec* a, const struct timespec* b, struct timespec* out)
{
    out->tv_sec = a->tv_sec - b->tv_sec;
    out->tv_nsec = a->tv_nsec - b->tv_nsec;
    if (out->tv_nsec < 0) {
        out->tv_sec--;
        out->tv_nsec += 1000 * 1000 * 1000;
    }
}

static inline void timespecclear(struct timespec* out)
{
    out->tv_sec = out->tv_nsec = 0;
}

static inline int timespecisset(const struct timespec* ts)
{
    return ts->tv_sec || ts->tv_nsec;
}

static inline void TIMEVAL_TO_TIMESPEC(const struct timeval* tv, struct timespec* ts)
{
    ts->tv_sec = tv->tv_sec;
    ts->tv_nsec = tv->tv_usec * 1000;
}

static inline void TIMESPEC_TO_TIMEVAL(struct timeval* tv, const struct timespec* ts)
{
    tv->tv_sec = ts->tv_sec;
    tv->tv_usec = ts->tv_nsec / 1000;
}

__END_DECLS
