#pragma once

#include <AK/Types.h>

extern "C" {
int dbgprintf(const char* fmt, ...);
int dbgputstr(const char*, int);
int kprintf(const char* fmt, ...);
int sprintf(char* buf, const char* fmt, ...);
void set_serial_debug(bool on_or_off);
int get_serial_debug();
}

#ifdef KERNEL
#    define printf dbgprintf
#endif

#ifndef __serenity__
#define dbgprintf printf
#endif

#ifdef __cplusplus

template <size_t N>
inline int dbgputstr(const char (&array)[N])
{
    return ::dbgputstr(array, N);
}

#endif
