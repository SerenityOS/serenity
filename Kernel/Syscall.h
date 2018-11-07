#pragma once

#include <AK/Types.h>

#define ENUMERATE_SYSCALLS \
    __ENUMERATE_SYSCALL(spawn) \
    __ENUMERATE_SYSCALL(sleep) \
    __ENUMERATE_SYSCALL(yield) \
    __ENUMERATE_SYSCALL(putch) \
    __ENUMERATE_SYSCALL(open) \
    __ENUMERATE_SYSCALL(close) \
    __ENUMERATE_SYSCALL(read) \
    __ENUMERATE_SYSCALL(lseek) \
    __ENUMERATE_SYSCALL(kill) \
    __ENUMERATE_SYSCALL(getuid) \
    __ENUMERATE_SYSCALL(exit) \
    __ENUMERATE_SYSCALL(getgid) \
    __ENUMERATE_SYSCALL(getpid) \
    __ENUMERATE_SYSCALL(waitpid) \
    __ENUMERATE_SYSCALL(mmap) \
    __ENUMERATE_SYSCALL(munmap) \
    __ENUMERATE_SYSCALL(get_dir_entries) \
    __ENUMERATE_SYSCALL(lstat) \
    __ENUMERATE_SYSCALL(getcwd) \
    __ENUMERATE_SYSCALL(gettimeofday) \
    __ENUMERATE_SYSCALL(gethostname) \
    __ENUMERATE_SYSCALL(get_arguments) \
    __ENUMERATE_SYSCALL(chdir) \
    __ENUMERATE_SYSCALL(uname) \
    __ENUMERATE_SYSCALL(set_mmap_name) \
    __ENUMERATE_SYSCALL(readlink) \
    __ENUMERATE_SYSCALL(write) \
    __ENUMERATE_SYSCALL(ttyname_r) \
    __ENUMERATE_SYSCALL(stat) \
    __ENUMERATE_SYSCALL(get_environment) \
    __ENUMERATE_SYSCALL(getsid) \
    __ENUMERATE_SYSCALL(setsid) \
    __ENUMERATE_SYSCALL(getpgid) \
    __ENUMERATE_SYSCALL(setpgid) \
    __ENUMERATE_SYSCALL(getpgrp) \
    __ENUMERATE_SYSCALL(tcsetpgrp) \
    __ENUMERATE_SYSCALL(tcgetpgrp) \
    __ENUMERATE_SYSCALL(fork) \
    __ENUMERATE_SYSCALL(execve) \
    __ENUMERATE_SYSCALL(geteuid) \
    __ENUMERATE_SYSCALL(getegid) \
    __ENUMERATE_SYSCALL(signal) \
    __ENUMERATE_SYSCALL(isatty) \
    __ENUMERATE_SYSCALL(getdtablesize) \
    __ENUMERATE_SYSCALL(dup) \
    __ENUMERATE_SYSCALL(dup2) \
    __ENUMERATE_SYSCALL(sigaction) \
    __ENUMERATE_SYSCALL(getppid) \
    __ENUMERATE_SYSCALL(umask) \
    __ENUMERATE_SYSCALL(getgroups) \
    __ENUMERATE_SYSCALL(setgroups) \


#define DO_SYSCALL_A0(function) Syscall::invoke((dword)(function))
#define DO_SYSCALL_A1(function, arg1) Syscall::invoke((dword)(function), (dword)(arg1))
#define DO_SYSCALL_A2(function, arg1, arg2) Syscall::invoke((dword)(function), (dword)(arg1), (dword)(arg2))
#define DO_SYSCALL_A3(function, arg1, arg2, arg3) Syscall::invoke((dword)(function), (dword)(arg1), (dword)(arg2), (dword)arg3)

namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) SC_ ##x ,
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
};

inline constexpr const char* toString(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) case SC_ ##x: return #x;
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
    }
}

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
