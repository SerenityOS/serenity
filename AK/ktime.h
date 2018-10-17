#pragma once

#ifdef SERENITY_KERNEL
#include <Kernel/ktime.h>
#else
#include <time.h>
#define ktime time
#define klocaltime localtime
#endif

