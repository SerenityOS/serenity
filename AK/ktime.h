#pragma once

#ifdef SERENITY_KERNEL
inline time_t time(time_t* tloc)
{
    if (tloc)
        *tloc = 123;
    return 123;
}
#else
#include <time.h>
#define ktime time
#define klocaltime localtime
#endif

