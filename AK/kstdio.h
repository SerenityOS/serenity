#pragma once

#ifdef __serenity__
#include <Kernel/kstdio.h>
#else
#include <stdio.h>
#define kprintf printf
#define dbgprintf printf
#define dbgputstr(characters, length) fwrite(characters, 1, length, stdout)
#endif
