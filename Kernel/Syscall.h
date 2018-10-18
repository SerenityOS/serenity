#pragma once

#define DO_SYSCALL_A0(function) Syscall::invoke((DWORD)(function))
#define DO_SYSCALL_A1(function, arg1) Syscall::invoke((DWORD)(function), (DWORD)(arg1))
#define DO_SYSCALL_A2(function, arg1, arg2) Syscall::invoke((DWORD)(function), (DWORD)(arg1), (DWORD)(arg2))
#define DO_SYSCALL_A3(function, arg1, arg2, arg3) Syscall::invoke((DWORD)(function), (DWORD)(arg1), (DWORD)(arg2), (DWORD)arg3)

namespace Syscall {

enum Function {
    Sleep = 0x1982,
    Yield = 0x1983,
    PosixOpen = 0x1985,
    PosixClose = 0x1986,
    PosixRead = 0x1987,
    PosixSeek = 0x1988,
    PosixKill = 0x1989,
    PosixGetuid = 0x1990,
};

void initialize();

inline DWORD invoke(DWORD function)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function));
    return result;
}

inline DWORD invoke(DWORD function, DWORD arg1)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function),"d"(arg1));
    return result;
}

inline DWORD invoke(DWORD function, DWORD arg1, DWORD arg2)
{
    DWORD result;
    asm("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2));
    return result;
}

inline DWORD invoke(DWORD function, DWORD arg1, DWORD arg2, DWORD arg3)
{
    DWORD result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2),"b"(arg3));
    return result;
}

}
