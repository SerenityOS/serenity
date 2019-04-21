#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

struct timezone {
    int tz_minuteswest;
    int tz_dsttime;
};

int gettimeofday(struct timeval* __restrict__, void* __restrict__)  __attribute__((nonnull(1)));

__END_DECLS
