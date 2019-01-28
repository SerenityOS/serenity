#pragma once

#include <AK/Types.h>

#define ENUMERATE_SYSCALLS \
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
    __ENUMERATE_SYSCALL(fork) \
    __ENUMERATE_SYSCALL(execve) \
    __ENUMERATE_SYSCALL(geteuid) \
    __ENUMERATE_SYSCALL(getegid) \
    __ENUMERATE_SYSCALL(isatty) \
    __ENUMERATE_SYSCALL(getdtablesize) \
    __ENUMERATE_SYSCALL(dup) \
    __ENUMERATE_SYSCALL(dup2) \
    __ENUMERATE_SYSCALL(sigaction) \
    __ENUMERATE_SYSCALL(getppid) \
    __ENUMERATE_SYSCALL(umask) \
    __ENUMERATE_SYSCALL(getgroups) \
    __ENUMERATE_SYSCALL(setgroups) \
    __ENUMERATE_SYSCALL(sigreturn) \
    __ENUMERATE_SYSCALL(sigprocmask) \
    __ENUMERATE_SYSCALL(sigpending) \
    __ENUMERATE_SYSCALL(pipe) \
    __ENUMERATE_SYSCALL(killpg) \
    __ENUMERATE_SYSCALL(setuid) \
    __ENUMERATE_SYSCALL(setgid) \
    __ENUMERATE_SYSCALL(alarm) \
    __ENUMERATE_SYSCALL(fstat) \
    __ENUMERATE_SYSCALL(access) \
    __ENUMERATE_SYSCALL(fcntl) \
    __ENUMERATE_SYSCALL(ioctl) \
    __ENUMERATE_SYSCALL(mkdir) \
    __ENUMERATE_SYSCALL(times) \
    __ENUMERATE_SYSCALL(utime) \
    __ENUMERATE_SYSCALL(sync) \
    __ENUMERATE_SYSCALL(ptsname_r) \
    __ENUMERATE_SYSCALL(select) \
    __ENUMERATE_SYSCALL(unlink) \
    __ENUMERATE_SYSCALL(poll) \
    __ENUMERATE_SYSCALL(read_tsc) \
    __ENUMERATE_SYSCALL(gui_create_window) \
    __ENUMERATE_SYSCALL(gui_destroy_window) \
    __ENUMERATE_SYSCALL(gui_get_window_backing_store) \
    __ENUMERATE_SYSCALL(gui_release_window_backing_store) \
    __ENUMERATE_SYSCALL(gui_invalidate_window) \
    __ENUMERATE_SYSCALL(gui_get_window_title) \
    __ENUMERATE_SYSCALL(gui_set_window_title) \
    __ENUMERATE_SYSCALL(gui_get_window_rect) \
    __ENUMERATE_SYSCALL(gui_set_window_rect) \
    __ENUMERATE_SYSCALL(gui_notify_paint_finished) \
    __ENUMERATE_SYSCALL(gui_set_global_cursor_tracking_enabled) \
    __ENUMERATE_SYSCALL(rmdir) \


#ifdef SERENITY
struct fd_set;
#endif

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
    return "Unknown";
}

#ifdef SERENITY
struct SC_mmap_params {
    uint32_t addr;
    uint32_t size;
    int32_t prot;
    int32_t flags;
    int32_t fd;
    int32_t offset; // FIXME: 64-bit off_t?
};

struct SC_select_params {
    int nfds;
    fd_set* readfds;
    fd_set* writefds;
    fd_set* exceptfds;
    struct timeval* timeout;
};

void initialize();
int sync();

inline dword invoke(Function function)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function):"memory");
    return result;
}

template<typename T1>
inline dword invoke(Function function, T1 arg1)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"((dword)arg1):"memory");
    return result;
}

template<typename T1, typename T2>
inline dword invoke(Function function, T1 arg1, T2 arg2)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"((dword)arg1),"c"((dword)arg2):"memory");
    return result;
}

template<typename T1, typename T2, typename T3>
inline dword invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
    dword result;
    asm volatile("int $0x80":"=a"(result):"a"(function),"d"((dword)arg1),"c"((dword)arg2),"b"((dword)arg3):"memory");
    return result;
}
#endif

}

#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) using Syscall::SC_ ##x;
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
#define syscall Syscall::invoke
