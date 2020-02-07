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

#ifdef __serenity__
#    include <LibC/fd_set.h>
#endif

extern "C" {
struct timeval;
struct timespec;
struct sockaddr;
struct siginfo;
typedef u32 socklen_t;
}

#define ENUMERATE_SYSCALLS                          \
    __ENUMERATE_SYSCALL(sleep)                      \
    __ENUMERATE_SYSCALL(yield)                      \
    __ENUMERATE_SYSCALL(open)                       \
    __ENUMERATE_SYSCALL(close)                      \
    __ENUMERATE_SYSCALL(read)                       \
    __ENUMERATE_SYSCALL(lseek)                      \
    __ENUMERATE_SYSCALL(kill)                       \
    __ENUMERATE_SYSCALL(getuid)                     \
    __ENUMERATE_SYSCALL(exit)                       \
    __ENUMERATE_SYSCALL(getgid)                     \
    __ENUMERATE_SYSCALL(getpid)                     \
    __ENUMERATE_SYSCALL(waitid)                     \
    __ENUMERATE_SYSCALL(mmap)                       \
    __ENUMERATE_SYSCALL(munmap)                     \
    __ENUMERATE_SYSCALL(get_dir_entries)            \
    __ENUMERATE_SYSCALL(lstat)                      \
    __ENUMERATE_SYSCALL(getcwd)                     \
    __ENUMERATE_SYSCALL(gettimeofday)               \
    __ENUMERATE_SYSCALL(gethostname)                \
    __ENUMERATE_SYSCALL(chdir)                      \
    __ENUMERATE_SYSCALL(uname)                      \
    __ENUMERATE_SYSCALL(set_mmap_name)              \
    __ENUMERATE_SYSCALL(readlink)                   \
    __ENUMERATE_SYSCALL(write)                      \
    __ENUMERATE_SYSCALL(ttyname_r)                  \
    __ENUMERATE_SYSCALL(stat)                       \
    __ENUMERATE_SYSCALL(getsid)                     \
    __ENUMERATE_SYSCALL(setsid)                     \
    __ENUMERATE_SYSCALL(getpgid)                    \
    __ENUMERATE_SYSCALL(setpgid)                    \
    __ENUMERATE_SYSCALL(getpgrp)                    \
    __ENUMERATE_SYSCALL(fork)                       \
    __ENUMERATE_SYSCALL(execve)                     \
    __ENUMERATE_SYSCALL(geteuid)                    \
    __ENUMERATE_SYSCALL(getegid)                    \
    __ENUMERATE_SYSCALL(getdtablesize)              \
    __ENUMERATE_SYSCALL(dup)                        \
    __ENUMERATE_SYSCALL(dup2)                       \
    __ENUMERATE_SYSCALL(sigaction)                  \
    __ENUMERATE_SYSCALL(getppid)                    \
    __ENUMERATE_SYSCALL(umask)                      \
    __ENUMERATE_SYSCALL(getgroups)                  \
    __ENUMERATE_SYSCALL(setgroups)                  \
    __ENUMERATE_SYSCALL(sigreturn)                  \
    __ENUMERATE_SYSCALL(sigprocmask)                \
    __ENUMERATE_SYSCALL(sigpending)                 \
    __ENUMERATE_SYSCALL(pipe)                       \
    __ENUMERATE_SYSCALL(killpg)                     \
    __ENUMERATE_SYSCALL(setuid)                     \
    __ENUMERATE_SYSCALL(setgid)                     \
    __ENUMERATE_SYSCALL(alarm)                      \
    __ENUMERATE_SYSCALL(fstat)                      \
    __ENUMERATE_SYSCALL(access)                     \
    __ENUMERATE_SYSCALL(fcntl)                      \
    __ENUMERATE_SYSCALL(ioctl)                      \
    __ENUMERATE_SYSCALL(mkdir)                      \
    __ENUMERATE_SYSCALL(times)                      \
    __ENUMERATE_SYSCALL(utime)                      \
    __ENUMERATE_SYSCALL(sync)                       \
    __ENUMERATE_SYSCALL(ptsname_r)                  \
    __ENUMERATE_SYSCALL(select)                     \
    __ENUMERATE_SYSCALL(unlink)                     \
    __ENUMERATE_SYSCALL(poll)                       \
    __ENUMERATE_SYSCALL(rmdir)                      \
    __ENUMERATE_SYSCALL(chmod)                      \
    __ENUMERATE_SYSCALL(usleep)                     \
    __ENUMERATE_SYSCALL(socket)                     \
    __ENUMERATE_SYSCALL(bind)                       \
    __ENUMERATE_SYSCALL(accept)                     \
    __ENUMERATE_SYSCALL(listen)                     \
    __ENUMERATE_SYSCALL(connect)                    \
    __ENUMERATE_SYSCALL(create_shared_buffer)       \
    __ENUMERATE_SYSCALL(share_buffer_with)          \
    __ENUMERATE_SYSCALL(get_shared_buffer)          \
    __ENUMERATE_SYSCALL(release_shared_buffer)      \
    __ENUMERATE_SYSCALL(link)                       \
    __ENUMERATE_SYSCALL(chown)                      \
    __ENUMERATE_SYSCALL(fchmod)                     \
    __ENUMERATE_SYSCALL(symlink)                    \
    __ENUMERATE_SYSCALL(get_shared_buffer_size)     \
    __ENUMERATE_SYSCALL(seal_shared_buffer)         \
    __ENUMERATE_SYSCALL(sendto)                     \
    __ENUMERATE_SYSCALL(recvfrom)                   \
    __ENUMERATE_SYSCALL(getsockopt)                 \
    __ENUMERATE_SYSCALL(setsockopt)                 \
    __ENUMERATE_SYSCALL(create_thread)              \
    __ENUMERATE_SYSCALL(gettid)                     \
    __ENUMERATE_SYSCALL(donate)                     \
    __ENUMERATE_SYSCALL(rename)                     \
    __ENUMERATE_SYSCALL(ftruncate)                  \
    __ENUMERATE_SYSCALL(systrace)                   \
    __ENUMERATE_SYSCALL(exit_thread)                \
    __ENUMERATE_SYSCALL(mknod)                      \
    __ENUMERATE_SYSCALL(writev)                     \
    __ENUMERATE_SYSCALL(beep)                       \
    __ENUMERATE_SYSCALL(getsockname)                \
    __ENUMERATE_SYSCALL(getpeername)                \
    __ENUMERATE_SYSCALL(sched_setparam)             \
    __ENUMERATE_SYSCALL(sched_getparam)             \
    __ENUMERATE_SYSCALL(fchown)                     \
    __ENUMERATE_SYSCALL(halt)                       \
    __ENUMERATE_SYSCALL(reboot)                     \
    __ENUMERATE_SYSCALL(mount)                      \
    __ENUMERATE_SYSCALL(umount)                     \
    __ENUMERATE_SYSCALL(dump_backtrace)             \
    __ENUMERATE_SYSCALL(dbgputch)                   \
    __ENUMERATE_SYSCALL(dbgputstr)                  \
    __ENUMERATE_SYSCALL(watch_file)                 \
    __ENUMERATE_SYSCALL(share_buffer_globally)      \
    __ENUMERATE_SYSCALL(set_process_icon)           \
    __ENUMERATE_SYSCALL(mprotect)                   \
    __ENUMERATE_SYSCALL(realpath)                   \
    __ENUMERATE_SYSCALL(get_process_name)           \
    __ENUMERATE_SYSCALL(fchdir)                     \
    __ENUMERATE_SYSCALL(getrandom)                  \
    __ENUMERATE_SYSCALL(setkeymap)                  \
    __ENUMERATE_SYSCALL(clock_gettime)              \
    __ENUMERATE_SYSCALL(clock_nanosleep)            \
    __ENUMERATE_SYSCALL(join_thread)                \
    __ENUMERATE_SYSCALL(module_load)                \
    __ENUMERATE_SYSCALL(module_unload)              \
    __ENUMERATE_SYSCALL(detach_thread)              \
    __ENUMERATE_SYSCALL(set_thread_name)            \
    __ENUMERATE_SYSCALL(get_thread_name)            \
    __ENUMERATE_SYSCALL(madvise)                    \
    __ENUMERATE_SYSCALL(purge)                      \
    __ENUMERATE_SYSCALL(set_shared_buffer_volatile) \
    __ENUMERATE_SYSCALL(profiling_enable)           \
    __ENUMERATE_SYSCALL(profiling_disable)          \
    __ENUMERATE_SYSCALL(get_kernel_info_page)       \
    __ENUMERATE_SYSCALL(futex)                      \
    __ENUMERATE_SYSCALL(set_thread_boost)           \
    __ENUMERATE_SYSCALL(set_process_boost)          \
    __ENUMERATE_SYSCALL(chroot)                     \
    __ENUMERATE_SYSCALL(pledge)                     \
    __ENUMERATE_SYSCALL(unveil)                     \
    __ENUMERATE_SYSCALL(perf_event)                 \
    __ENUMERATE_SYSCALL(shutdown)

namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#undef __ENUMERATE_REMOVED_SYSCALL
#define __ENUMERATE_REMOVED_SYSCALL(x) SC_##x,
#define __ENUMERATE_SYSCALL(x) SC_##x,
    ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
#undef __ENUMERATE_REMOVED_SYSCALL
        __Count
};

inline constexpr const char* to_string(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#undef __ENUMERATE_REMOVED_SYSCALL
#define __ENUMERATE_REMOVED_SYSCALL(x) \
    case SC_##x:                       \
        return #x " (removed)";
#define __ENUMERATE_SYSCALL(x) \
    case SC_##x:               \
        return #x;
        ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
#undef __ENUMERATE_REMOVED_SYSCALL
    default:
        break;
    }
    return "Unknown";
}

#ifdef __serenity__
struct StringArgument {
    const char* characters { nullptr };
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
    StringArgument* strings { nullptr };
    size_t length { 0 };
};

struct SC_mmap_params {
    uint32_t addr;
    uint32_t size;
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
    struct timeval* timeout;
};

struct SC_clock_nanosleep_params {
    int clock_id;
    int flags;
    const struct timespec* requested_sleep;
    struct timespec* remaining_sleep;
};

struct SC_sendto_params {
    int sockfd;
    ImmutableBufferArgument<void, size_t> data;
    int flags;
    const sockaddr* addr;
    socklen_t addr_length;
};

struct SC_recvfrom_params {
    int sockfd;
    MutableBufferArgument<void, size_t> buffer;
    int flags;
    sockaddr* addr;
    socklen_t* addr_length;
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
    i32* userspace_address;
    int futex_op;
    i32 val;
    const timespec* timeout;
};

struct SC_setkeymap_params {
    const char* map;
    const char* shift_map;
    const char* alt_map;
    const char* altgr_map;
};

struct SC_create_thread_params {
    unsigned int m_detach_state = 0; // JOINABLE or DETACHED
    int m_schedule_priority = 30;    // THREAD_PRIORITY_NORMAL
    // FIXME: Implment guard pages in create_thread (unreadable pages at "overflow" end of stack)
    // "If an implementation rounds up the value of guardsize to a multiple of {PAGESIZE},
    // a call to pthread_attr_getguardsize() specifying attr shall store in the guardsize
    // parameter the guard size specified by the previous pthread_attr_setguardsize() function call"
    // ... ok, if you say so posix. Guess we get to lie to people about guard page size
    unsigned int m_guard_page_size = 0;          // Rounded up to PAGE_SIZE
    unsigned int m_reported_guard_page_size = 0; // The lie we tell callers
    unsigned int m_stack_size = 4 * MB;          // Default PTHREAD_STACK_MIN
    void* m_stack_location = nullptr;            // nullptr means any, o.w. process virtual address
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
    StringArgument source;
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
#define __ENUMERATE_REMOVED_SYSCALL(x)
ENUMERATE_SYSCALLS
#undef __ENUMERATE_SYSCALL
#undef __ENUMERATE_REMOVED_SYSCALL
#define syscall Syscall::invoke
