#pragma once

#include "types.h"

extern "C" {

inline dword syscall_a0(dword function)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function));
    return result;
}

inline dword syscall_a1(dword function, dword arg1)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1));
    return result;
}

inline dword syscall_a2(dword function, dword arg1, dword arg2)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2));
    return result;
}

inline dword syscall_a3(dword function, dword arg1, dword arg2, dword arg3)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2),"b"(arg3));
    return result;
}

}

