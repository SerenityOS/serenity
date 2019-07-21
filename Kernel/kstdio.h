#pragma once

#include <AK/Types.h>

extern "C" {
int dbgprintf(const char* fmt, ...);
int dbgputstr(const char*, int);
int kprintf(const char* fmt, ...);
int ksprintf(char* buf, const char* fmt, ...);
}

#ifndef USERLAND
#    define printf dbgprintf
#endif

#ifndef __serenity__
#define dbgprintf printf
#endif
