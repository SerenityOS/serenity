#pragma once

#include <AK/Types.h>
#include <LibC/fd_set.h>

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
    __ENUMERATE_SYSCALL(chdir) \
    __ENUMERATE_SYSCALL(uname) \
    __ENUMERATE_SYSCALL(set_mmap_name) \
    __ENUMERATE_SYSCALL(readlink) \
    __ENUMERATE_SYSCALL(write) \
    __ENUMERATE_SYSCALL(ttyname_r) \
    __ENUMERATE_SYSCALL(stat) \
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
    __ENUMERATE_SYSCALL(rmdir) \
    __ENUMERATE_SYSCALL(chmod) \
    __ENUMERATE_SYSCALL(usleep) \
    __ENUMERATE_SYSCALL(socket) \
    __ENUMERATE_SYSCALL(bind) \
    __ENUMERATE_SYSCALL(accept) \
    __ENUMERATE_SYSCALL(listen) \
    __ENUMERATE_SYSCALL(connect) \
    __ENUMERATE_SYSCALL(create_shared_buffer) \
    __ENUMERATE_SYSCALL(get_shared_buffer) \
    __ENUMERATE_SYSCALL(release_shared_buffer) \
    __ENUMERATE_SYSCALL(link) \
    __ENUMERATE_SYSCALL(chown) \
    __ENUMERATE_SYSCALL(fchmod) \
    __ENUMERATE_SYSCALL(symlink) \
    __ENUMERATE_SYSCALL(restore_signal_mask) \
    __ENUMERATE_SYSCALL(get_shared_buffer_size) \
    __ENUMERATE_SYSCALL(seal_shared_buffer) \
    __ENUMERATE_SYSCALL(sendto) \
    __ENUMERATE_SYSCALL(recvfrom) \
    __ENUMERATE_SYSCALL(getsockopt) \
    __ENUMERATE_SYSCALL(setsockopt) \
    __ENUMERATE_SYSCALL(create_thread) \
    __ENUMERATE_SYSCALL(gettid) \
    __ENUMERATE_SYSCALL(donate) \
    __ENUMERATE_SYSCALL(rename) \
    __ENUMERATE_SYSCALL(shm_open) \
    __ENUMERATE_SYSCALL(shm_close) \
    __ENUMERATE_SYSCALL(ftruncate) \
    __ENUMERATE_SYSCALL(systrace) \
    __ENUMERATE_SYSCALL(exit_thread) \
    __ENUMERATE_SYSCALL(mknod) \
    __ENUMERATE_SYSCALL(writev) \
    __ENUMERATE_SYSCALL(beep) \
    __ENUMERATE_SYSCALL(getsockname) \


namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) SC_ ##x ,
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
};

inline constexpr const char* to_string(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) case SC_ ##x: return #x;
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
    }
    return "Unknown";
}

#ifdef __serenity__
struct SC_mmap_params {
    uint32_t addr;
    uint32_t size;
    int32_t prot;
    int32_t flags;
    int32_t fd;
    int32_t offset; // FIXME: 64-bit off_t?
    const char* name { nullptr };
};

struct SC_select_params {
    int nfds;
    fd_set* readfds;
    fd_set* writefds;
    fd_set* exceptfds;
    struct timeval* timeout;
};

struct SC_sendto_params {
    int sockfd;
    const void* data;
    size_t data_length;
    int flags;
    const void* addr; // const sockaddr*
    size_t addr_length; // socklen_t
};

struct SC_recvfrom_params {
    int sockfd;
    void* buffer;
    size_t buffer_length;
    int flags;
    void* addr; // sockaddr*
    void* addr_length; // socklen_t*
};

struct SC_getsockopt_params {
    int sockfd;
    int level;
    int option;
    void* value;
    void* value_size; // socklen_t*
};

struct SC_setsockopt_params {
    int sockfd;
    int level;
    int option;
    const void* value;
    size_t value_size; // socklen_t
};

void initialize();
int sync();

inline dword invoke(Function function)
{
    dword result;
    asm volatile("int $0x82":"=a"(result):"a"(function):"memory");
    return result;
}

template<typename T1>
inline dword invoke(Function function, T1 arg1)
{
    dword result;
    asm volatile("int $0x82":"=a"(result):"a"(function),"d"((dword)arg1):"memory");
    return result;
}

template<typename T1, typename T2>
inline dword invoke(Function function, T1 arg1, T2 arg2)
{
    dword result;
    asm volatile("int $0x82":"=a"(result):"a"(function),"d"((dword)arg1),"c"((dword)arg2):"memory");
    return result;
}

template<typename T1, typename T2, typename T3>
inline dword invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
    dword result;
    asm volatile("int $0x82":"=a"(result):"a"(function),"d"((dword)arg1),"c"((dword)arg2),"b"((dword)arg3):"memory");
    return result;
}
#endif

}

#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) using Syscall::SC_ ##x;
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
#define syscall Syscall::invoke
