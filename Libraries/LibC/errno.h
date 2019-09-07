#pragma once

#include <errno_numbers.h>
#include <sys/cdefs.h>

#define __RETURN_WITH_ERRNO(rc, good_ret, bad_ret) \
    do {                                           \
        if (rc < 0) {                              \
            errno = -rc;                           \
            return (bad_ret);                      \
        }                                          \
        errno = 0;                                 \
        return (good_ret);                         \
    } while (0)

__BEGIN_DECLS

extern const char* sys_errlist[];
extern int sys_nerr;
extern __thread int errno;

__END_DECLS
