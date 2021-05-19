/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
struct statvfs;
typedef u32 socklen_t;
}

namespace Kernel {

#define ENUMERATE_SYSCALLS(S)     \
    S(yield)                      \
    S(open)                       \
    S(close)                      \
    S(read)                       \
    S(lseek)                      \
    S(kill)                       \
    S(getuid)                     \
    S(exit)                       \
    S(geteuid)                    \
    S(getegid)                    \
    S(getgid)                     \
    S(getpid)                     \
    S(getppid)                    \
    S(getresuid)                  \
    S(getresgid)                  \
    S(waitid)                     \
    S(mmap)                       \
    S(munmap)                     \
    S(get_dir_entries)            \
    S(getcwd)                     \
    S(gettimeofday)               \
    S(gethostname)                \
    S(sethostname)                \
    S(chdir)                      \
    S(uname)                      \
    S(set_mmap_name)              \
    S(readlink)                   \
    S(write)                      \
    S(ttyname)                    \
    S(stat)                       \
    S(getsid)                     \
    S(setsid)                     \
    S(getpgid)                    \
    S(setpgid)                    \
    S(getpgrp)                    \
    S(fork)                       \
    S(execve)                     \
    S(dup2)                       \
    S(sigaction)                  \
    S(umask)                      \
    S(getgroups)                  \
    S(setgroups)                  \
    S(sigreturn)                  \
    S(sigprocmask)                \
    S(sigpending)                 \
    S(pipe)                       \
    S(killpg)                     \
    S(seteuid)                    \
    S(setegid)                    \
    S(setuid)                     \
    S(setgid)                     \
    S(setreuid)                   \
    S(setresuid)                  \
    S(setresgid)                  \
    S(alarm)                      \
    S(fstat)                      \
    S(access)                     \
    S(fcntl)                      \
    S(ioctl)                      \
    S(mkdir)                      \
    S(times)                      \
    S(utime)                      \
    S(sync)                       \
    S(ptsname)                    \
    S(select)                     \
    S(unlink)                     \
    S(poll)                       \
    S(rmdir)                      \
    S(chmod)                      \
    S(socket)                     \
    S(bind)                       \
    S(accept4)                    \
    S(listen)                     \
    S(connect)                    \
    S(link)                       \
    S(chown)                      \
    S(fchmod)                     \
    S(symlink)                    \
    S(sendmsg)                    \
    S(recvmsg)                    \
    S(getsockopt)                 \
    S(setsockopt)                 \
    S(create_thread)              \
    S(gettid)                     \
    S(donate)                     \
    S(rename)                     \
    S(ftruncate)                  \
    S(exit_thread)                \
    S(mknod)                      \
    S(writev)                     \
    S(beep)                       \
    S(getsockname)                \
    S(getpeername)                \
    S(socketpair)                 \
    S(sched_setparam)             \
    S(sched_getparam)             \
    S(fchown)                     \
    S(halt)                       \
    S(reboot)                     \
    S(mount)                      \
    S(umount)                     \
    S(dump_backtrace)             \
    S(dbgputch)                   \
    S(dbgputstr)                  \
    S(create_inode_watcher)       \
    S(inode_watcher_add_watch)    \
    S(inode_watcher_remove_watch) \
    S(mprotect)                   \
    S(realpath)                   \
    S(get_process_name)           \
    S(fchdir)                     \
    S(getrandom)                  \
    S(getkeymap)                  \
    S(setkeymap)                  \
    S(clock_gettime)              \
    S(clock_settime)              \
    S(clock_nanosleep)            \
    S(join_thread)                \
    S(module_load)                \
    S(module_unload)              \
    S(detach_thread)              \
    S(set_thread_name)            \
    S(get_thread_name)            \
    S(madvise)                    \
    S(purge)                      \
    S(profiling_enable)           \
    S(profiling_disable)          \
    S(profiling_free_buffer)      \
    S(futex)                      \
    S(chroot)                     \
    S(pledge)                     \
    S(unveil)                     \
    S(perf_event)                 \
    S(shutdown)                   \
    S(get_stack_bounds)           \
    S(ptrace)                     \
    S(sendfd)                     \
    S(recvfd)                     \
    S(sysconf)                    \
    S(set_process_name)           \
    S(disown)                     \
    S(adjtime)                    \
    S(allocate_tls)               \
    S(prctl)                      \
    S(mremap)                     \
    S(set_coredump_metadata)      \
    S(anon_create)                \
    S(msyscall)                   \
    S(readv)                      \
    S(emuctl)                     \
    S(statvfs)                    \
    S(fstatvfs)

namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) SC_##x,
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
        __Count
};

constexpr const char* to_string(Function function)
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

struct StringListArgument {
    StringArgument* strings {};
    size_t length { 0 };
};

struct SC_mmap_params {
    uintptr_t addr;
    size_t size;
    size_t alignment;
    int32_t prot;
    int32_t flags;
    int32_t fd;
    int64_t offset;
    StringArgument name;
};

struct SC_mremap_params {
    uintptr_t old_address;
    size_t old_size;
    size_t new_size;
    int32_t flags;
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

struct SC_accept4_params {
    int sockfd;
    sockaddr* addr;
    socklen_t* addrlen;
    int flags;
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

struct SC_socketpair_params {
    int domain;
    int type;
    int protocol;
    int* sv;
};

struct SC_futex_params {
    u32* userspace_address;
    int futex_op;
    u32 val;
    union {
        const timespec* timeout;
        uintptr_t val2;
    };
    u32* userspace_address2;
    u32 val3;
};

struct SC_setkeymap_params {
    const u32* map;
    const u32* shift_map;
    const u32* alt_map;
    const u32* altgr_map;
    const u32* shift_altgr_map;
    StringArgument map_name;
};

struct SC_getkeymap_params {
    u32* map;
    u32* shift_map;
    u32* alt_map;
    u32* altgr_map;
    u32* shift_altgr_map;
    MutableBufferArgument<char, size_t> map_name;
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
    int dirfd;
    StringArgument path;
    struct stat* statbuf;
    int follow_symlinks;
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

struct SC_set_coredump_metadata_params {
    StringArgument key;
    StringArgument value;
};

struct SC_inode_watcher_add_watch_params {
    int fd;
    StringArgument user_path;
    u32 event_mask;
};

struct SC_statvfs_params {
    StringArgument path;
    struct statvfs* buf;
};

void initialize();
int sync();

inline uintptr_t invoke(Function function)
{
    uintptr_t result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function)
                 : "memory");
    return result;
}

template<typename T1>
inline uintptr_t invoke(Function function, T1 arg1)
{
    uintptr_t result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1)
                 : "memory");
    return result;
}

template<typename T1, typename T2>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2)
{
    uintptr_t result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "c"((uintptr_t)arg2)
                 : "memory");
    return result;
}

template<typename T1, typename T2, typename T3>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
    uintptr_t result;
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "c"((uintptr_t)arg2), "b"((uintptr_t)arg3)
                 : "memory");
    return result;
}
#endif

}

#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(x) using Syscall::SC_##x;
ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL

}

using namespace Kernel;
