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

#include <sys/cdefs.h>
#include <sys/types.h>
#include <time.h>

__BEGIN_DECLS

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

struct timezone {
    int tz_minuteswest;
    int tz_dsttime;
};

int gettimeofday(struct timeval* __restrict__, void* __restrict__) __attribute__((nonnull(1)));
int settimeofday(struct timeval* __restrict__, void* __restrict__) __attribute__((nonnull(1)));

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

#define timeradd timeradd
#define timersub timersub
#define timerclear timerclear
#define timerisset timerisset
#define timercmp(tvp, uvp, cmp) \
    (((tvp)->tv_sec == (uvp)->tv_sec) ? ((tvp)->tv_usec cmp(uvp)->tv_usec) : ((tvp)->tv_sec cmp(uvp)->tv_sec))

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

#define timespecadd timespecadd
#define timespecsub timespecsub
#define timespecclear timespecclear
#define timespecisset timespecisset
#define timespeccmp(ts, us, cmp) \
    (((ts)->tv_sec == (us)->tv_sec) ? ((ts)->vf_nsec cmp(us)->tv_nsec) : ((ts)->tv_sec cmp(us)->tv_sec))

__END_DECLS
