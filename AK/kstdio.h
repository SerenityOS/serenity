#pragma once

#ifdef SERENITY_KERNEL
#include <Kernel/kstdio.h>
#else
#include <cstdio>
#define kprintf printf
#define ksprintf sprintf
#endif
