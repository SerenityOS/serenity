#pragma once

#ifdef __serenity__
#include <Kernel/kstdio.h>
#else
#include <stdio.h>
#define kprintf printf
#endif
