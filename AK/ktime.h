#pragma once

#ifdef SERENITY
#include <Kernel/ktime.h>
#else
#include <time.h>
#define ktime time
#define klocaltime localtime
#endif

