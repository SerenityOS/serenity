#pragma once

extern "C" {
int dbgprintf(const char* fmt, ...);
int kprintf(const char* fmt, ...);
int ksprintf(char* buf, const char* fmt, ...);
}

#ifndef USERLAND
#    define printf dbgprintf
#endif

#ifndef __serenity__
#define dbgprintf printf
#endif
