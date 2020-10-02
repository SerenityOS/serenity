/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>
#include <AK/Userspace.h>

#ifdef __serenity__
#    include <LibC/fd_set.h>
#endif

constexpr int syscall_vector = 0x82;

extern "C" {
struct pollfd;
struct timeval;
struct timespec;
struct sockaddr;
struct siginfo;
struct stat;
typedef u32 socklen_t;
}

namespace Kernel {

#define ENUMERATE_SYSCALLS(S) \
    S(yield)                  \
    S(open)                   \
    S(close)                  \
    S(read)                   \
    S(lseek)                  \
    S(kill)                   \
    S(getuid)                 \
    S(exit)                   \
    S(geteuid)                \
    S(getegid)                \
    S(getgid)                 \
    S(getpid)                 \
    S(getppid)                \
    S(getresuid)              \
    S(getresgid)              \
    S(waitid)                 \
    S(mmap)                   \
    S(munmap)                 \
    S(get_dir_entries)        \
    S(getcwd)                 \
    S(gettimeofday)           \
    S(gethostname)            \
    S(sethostname)            \
    S(chdir)                  \
    S(uname)                  \
    S(set_mmap_name)          \
    S(readlink)               \
    S(write)                  \
    S(ttyname)                \
    S(stat)                   \
    S(getsid)                 \
    S(setsid)                 \
    S(getpgid)                \
    S(setpgid)                \
    S(getpgrp)                \
    S(fork)                   \
    S(execve)                 \
    S(dup2)                   \
    S(sigaction)              \
    S(umask)                  \
    S(getgroups)              \
    S(setgroups)              \
    S(sigreturn)              \
    S(sigprocmask)            \
    S(sigpending)             \
    S(pipe)                   \
    S(killpg)                 \
    S(seteuid)                \
    S(setegid)                \
    S(setuid)                 \
    S(setgid)                 \
    S(setresuid)              \
    S(setresgid)              \
    S(alarm)                  \
    S(fstat)                  \
    S(access)                 \
    S(fcntl)                  \
    S(ioctl)                  \
    S(mkdir)                  \
    S(times)                  \
    S(utime)                  \
    S(sync)                   \
    S(ptsname)                \
    S(select)                 \
    S(unlink)                 \
    S(poll)                   \
    S(rmdir)                  \
    S(chmod)                  \
    S(socket)                 \
    S(bind)                   \
    S(accept)                 \
    S(listen)                 \
    S(connect)                \
    S(shbuf_create)           \
    S(shbuf_allow_pid)        \
    S(shbuf_get)              \
    S(shbuf_release)          \
    S(link)                   \
    S(chown)                  \
    S(fchmod)                 \
    S(symlink)                \
    S(shbuf_seal)             \
    S(sendmsg)                \
    S(recvmsg)                \
    S(getsockopt)             \
    S(setsockopt)             \
    S(create_thread)          \
    S(gettid)                 \
    S(donate)                 \
    S(rename)                 \
    S(ftruncate)              \
    S(exit_thread)            \
    S(mknod)                  \
    S(writev)                 \
    S(beep)                   \
    S(getsockname)            \
    S(getpeername)            \
    S(sched_setparam)         \
    S(sched_getparam)         \
    S(fchown)                 \
    S(halt)                   \
    S(reboot)                 \
    S(mount)                  \
    S(umount)                 \
    S(dump_backtrace)         \
    S(dbgputch)               \
    S(dbgputstr)              \
    S(watch_file)             \
    S(shbuf_allow_all)        \
    S(set_process_icon)       \
    S(mprotect)               \
    S(realpath)               \
    S(get_process_name)       \
    S(fchdir)                 \
    S(getrandom)              \
    S(setkeymap)              \
    S(clock_gettime)          \
    S(clock_settime)          \
    S(clock_nanosleep)        \
    S(join_thread)            \
    S(module_load)            \
    S(module_unload)          \
    S(detach_thread)          \
    S(set_thread_name)        \
    S(get_thread_name)        \
    S(madvise)                \
    S(purge)                  \
    S(shbuf_set_volatile)     \
    S(profiling_enable)       \
    S(profiling_disable)      \
    S(futex)                  \
    S(set_thread_boost)       \
    S(set_process_boost)      \
    S(chroot)                 \
    S(pledge)                 \
    S(unveil)                 \
    S(perf_event)             \
    S(shutdown)               \
    S(get_stack_bounds)       \
    S(ptrace)                 \
    S(minherit)               \
    S(sendfd)                 \
    S(recvfd)                 \
    S(sysconf)                \
    S(set_process_name)       \
    S(disown)

namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) SC_##x,
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
        __Count
};

inline constexpr const char* to_string(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) \
    case SC_##x:               \
        return #x;
        ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
    default:
        break;
    }
    return "Unknown";
}

#ifdef __serenity__
struct StringArgument {
    const char* characters;
    size_t length { 0 };
};

template<typename DataType, typename SizeType>
struct MutableBufferArgument {
    DataType* data { nullptr };
    SizeType size { 0 };
};

template<typename DataType, typename SizeType>
struct ImmutableBufferArgument {
    const DataType* data { nullptr };
    SizeType size { 0 };
};

struct StringListArgument {
    StringArgument* strings {};
    size_t length { 0 };
};

struct SC_mmap_params {
    uint32_t addr;
    uint32_t size;
    uint32_t alignment;
    int32_t prot;
    int32_t flags;
    int32_t fd;
    int32_t offset; // FIXME: 64-bit off_t?
    StringArgument name;
};

struct SC_open_params {
    int dirfd;
    StringArgument path;
    int options;
    u16 mode;
};

struct SC_select_params {
    int nfds;
    fd_set* readfds;
    fd_set* writefds;
    fd_set* exceptfds;
    const struct timespec* timeout;
    const u32* sigmask;
};

struct SC_poll_params {
    struct pollfd* fds;
    unsigned nfds;
    const struct timespec* timeout;
    const u32* sigmask;
};

struct SC_clock_nanosleep_params {
    int clock_id;
    int flags;
    const struct timespec* requested_sleep;
    struct timespec* remaining_sleep;
};

struct SC_getsockopt_params {
    int sockfd;
    int level;
    int option;
    void* value;
    socklen_t* value_size;
};

struct SC_setsockopt_params {
    int sockfd;
    int level;
    int option;
    const void* value;
    socklen_t value_size;
};

struct SC_getsockname_params {
    int sockfd;
    sockaddr* addr;
    socklen_t* addrlen;
};

struct SC_getpeername_params {
    int sockfd;
    sockaddr* addr;
    socklen_t* addrlen;
};

struct SC_futex_params {
    const i32* userspace_address;
    int futex_op;
    i32 val;
    const timespec* timeout;
};

struct SC_setkeymap_params {
    const u32* map;
    const u32* shift_map;
    const u32* alt_map;
    const u32* altgr_map;
    StringArgument map_name;
};

struct SC_create_thread_params {
    unsigned int m_detach_state = 0; // JOINABLE or DETACHED
    int m_schedule_priority = 30;    // THREAD_PRIORITY_NORMAL
    // FIXME: Implement guard pages in create_thread (unreadable pages at "overflow" end of stack)
    // "If an implementation rounds up the value of guardsize to a multiple of {PAGESIZE},
    // a call to pthread_attr_getguardsize() specifying attr shall store in the guardsize
    // parameter the guard size specified by the previous pthread_attr_setguardsize() function call"
    // ... ok, if you say so posix. Guess we get to lie to people about guard page size
    unsigned int m_guard_page_size = 0;          // Rounded up to PAGE_SIZE
    unsigned int m_reported_guard_page_size = 0; // The lie we tell callers
    unsigned int m_stack_size = 4 * MiB;         // Default PTHREAD_STACK_MIN
    void* m_stack_location;                      // nullptr means any, o.w. process virtual address
};

struct SC_realpath_params {
    StringArgument path;
    MutableBufferArgument<char, size_t> buffer;
};

struct SC_set_mmap_name_params {
    void* addr;
    size_t size;
    StringArgument name;
};

struct SC_execve_params {
    StringArgument path;
    StringListArgument arguments;
    StringListArgument environment;
};

struct SC_readlink_params {
    StringArgument path;
    MutableBufferArgument<char, size_t> buffer;
};

struct SC_link_params {
    StringArgument old_path;
    StringArgument new_path;
};

struct SC_chown_params {
    StringArgument path;
    u32 uid;
    u32 gid;
};

struct SC_mknod_params {
    StringArgument path;
    u16 mode;
    u32 dev;
};

struct SC_symlink_params {
    StringArgument target;
    StringArgument linkpath;
};

struct SC_rename_params {
    StringArgument old_path;
    StringArgument new_path;
};

struct SC_mount_params {
    int source_fd;
    StringArgument target;
    StringArgument fs_type;
    int flags;
};

struct SC_pledge_params {
    StringArgument promises;
    StringArgument execpromises;
};

struct SC_unveil_params {
    StringArgument path;
    StringArgument permissions;
};

struct SC_waitid_params {
    int idtype;
    int id;
    struct siginfo* infop;
    int options;
};

struct SC_stat_params {
    StringArgument path;
    struct stat* statbuf;
    bool follow_symlinks;
};

struct SC_ptrace_params {
    int request;
    pid_t tid;
    u8* addr;
    int data;
};

struct SC_ptrace_peek_params {
    const u32* address;
    u32* out_data;
};

void initialize();
int sync();

inline u32 invoke(Function function)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function)
                 : "memory");
    return result;
}

template<typename T1>
inline u32 invoke(Function function, T1 arg1)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1)
                 : "memory");
    return result;
}

template<typename T1, typename T2>
inline u32 invoke(Function function, T1 arg1, T2 arg2)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1), "c"((u32)arg2)
                 : "memory");
    return result;
}

template<typename T1, typename T2, typename T3>
inline u32 invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
    u32 result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((u32)arg1), "c"((u32)arg2), "b"((u32)arg3)
                 : "memory");
    return result;
}
#endif

}

#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) using Syscall::SC_##x;
ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
#define syscall Syscall::invoke

}

using namespace Kernel;
