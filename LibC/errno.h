#pragma once

#include <Kernel/errno.h>

#define __RETURN_WITH_ERRNO(rc, good_ret, bad_ret) \
    do { \
        if (rc < 0) { \
            errno = -rc; \
            return (bad_ret); \
        } else { \
            errno = 0; \
            return (good_ret); \
        } \
    } while(0)

extern "C" {

extern int errno;

};

