#pragma once

#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/UnixTypes.h>
#else
#    include <sys/time.h>
#endif

struct KernelInfoPage {
    volatile u32 serial;
    volatile struct timeval now;
};
