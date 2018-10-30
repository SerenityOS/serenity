#pragma once

#include <AK/Types.h>

#define DO_SYSCALL_A0(function) Syscall::invoke((dword)(function))
#define DO_SYSCALL_A1(function, arg1) Syscall::invoke((dword)(function), (dword)(arg1))
#define DO_SYSCALL_A2(function, arg1, arg2) Syscall::invoke((dword)(function), (dword)(arg1), (dword)(arg2))
#define DO_SYSCALL_A3(function, arg1, arg2, arg3) Syscall::invoke((dword)(function), (dword)(arg1), (dword)(arg2), (dword)arg3)

namespace Syscall {

enum Function {
    Spawn = 0x1981,
    Sleep = 0x1982,
    Yield = 0x1983,
    PutCharacter = 1984,
    PosixOpen = 0x1985,
    PosixClose = 0x1986,
    PosixRead = 0x1987,
    PosixSeek = 0x1988,
    PosixKill = 0x1989,
    PosixGetuid = 0x1990,
    PosixExit = 0x1991,
    PosixGetgid = 0x1992,
    PosixGetpid = 0x1993,
    PosixWaitpid = 0x1994,
    PosixMmap = 0x1995,
    PosixMunmap = 0x1996,
    GetDirEntries = 0x1997,
    PosixLstat = 0x1998,
    PosixGetcwd = 0x1999,
    PosixGettimeofday = 0x2000,
    PosixGethostname = 0x2001,
    GetArguments = 0x2002,
    PosixChdir = 0x2003,
    PosixUname = 0x2004,
    SetMmapName = 0x2005,
    PosixReadlink = 0x2006,
    PosixWrite = 0x2007,
};

void initialize();

inline dword invoke(dword function)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function));
    return result;
}

inline dword invoke(dword function, dword arg1)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1));
    return result;
}

inline dword invoke(dword function, dword arg1, dword arg2)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2));
    return result;
}

inline dword invoke(dword function, dword arg1, dword arg2, dword arg3)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"(arg1),"c"(arg2),"b"(arg3));
    return result;
}

}
