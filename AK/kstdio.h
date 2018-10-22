#pragma once

#ifdef SERENITY
#include <Kernel/kstdio.h>
#else
#include <cstdio>
#define kprintf printf
#define ksprintf sprintf
#endif
