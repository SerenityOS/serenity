/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
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

enum class NeedsBigProcessLock {
    Yes,
    No
};

// Declare all syscalls and associated  metadata.
//
// NOTE: When declaring a new syscall or modifying an existing, please
// ensure that the proper assert is present at the top of the syscall
// implementation to both verify and document to any readers if the
// syscall acquires the big process lock or not. The asserts are:
//   - VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
//   - VERIFY_NO_PROCESS_BIG_LOCK(this)
//
#define ENUMERATE_SYSCALLS(S)                               \
    S(accept4, NeedsBigProcessLock::Yes)                    \
    S(access, NeedsBigProcessLock::Yes)                     \
    S(adjtime, NeedsBigProcessLock::Yes)                    \
    S(alarm, NeedsBigProcessLock::Yes)                      \
    S(allocate_tls, NeedsBigProcessLock::Yes)               \
    S(anon_create, NeedsBigProcessLock::Yes)                \
    S(beep, NeedsBigProcessLock::No)                        \
    S(bind, NeedsBigProcessLock::Yes)                       \
    S(chdir, NeedsBigProcessLock::Yes)                      \
    S(chmod, NeedsBigProcessLock::Yes)                      \
    S(chown, NeedsBigProcessLock::Yes)                      \
    S(clock_gettime, NeedsBigProcessLock::No)               \
    S(clock_nanosleep, NeedsBigProcessLock::No)             \
    S(clock_settime, NeedsBigProcessLock::Yes)              \
    S(close, NeedsBigProcessLock::Yes)                      \
    S(connect, NeedsBigProcessLock::Yes)                    \
    S(create_inode_watcher, NeedsBigProcessLock::Yes)       \
    S(create_thread, NeedsBigProcessLock::Yes)              \
    S(dbgputstr, NeedsBigProcessLock::No)                   \
    S(detach_thread, NeedsBigProcessLock::Yes)              \
    S(disown, NeedsBigProcessLock::Yes)                     \
    S(dump_backtrace, NeedsBigProcessLock::No)              \
    S(dup2, NeedsBigProcessLock::Yes)                       \
    S(emuctl, NeedsBigProcessLock::Yes)                     \
    S(execve, NeedsBigProcessLock::Yes)                     \
    S(exit, NeedsBigProcessLock::Yes)                       \
    S(exit_thread, NeedsBigProcessLock::Yes)                \
    S(fchdir, NeedsBigProcessLock::Yes)                     \
    S(fchmod, NeedsBigProcessLock::Yes)                     \
    S(fchown, NeedsBigProcessLock::Yes)                     \
    S(fcntl, NeedsBigProcessLock::Yes)                      \
    S(fork, NeedsBigProcessLock::Yes)                       \
    S(fstat, NeedsBigProcessLock::Yes)                      \
    S(fstatvfs, NeedsBigProcessLock::Yes)                   \
    S(fsync, NeedsBigProcessLock::Yes)                      \
    S(ftruncate, NeedsBigProcessLock::Yes)                  \
    S(futex, NeedsBigProcessLock::Yes)                      \
    S(get_dir_entries, NeedsBigProcessLock::Yes)            \
    S(get_process_name, NeedsBigProcessLock::Yes)           \
    S(get_stack_bounds, NeedsBigProcessLock::No)            \
    S(get_thread_name, NeedsBigProcessLock::Yes)            \
    S(getcwd, NeedsBigProcessLock::Yes)                     \
    S(getegid, NeedsBigProcessLock::Yes)                    \
    S(geteuid, NeedsBigProcessLock::Yes)                    \
    S(getgid, NeedsBigProcessLock::Yes)                     \
    S(getgroups, NeedsBigProcessLock::Yes)                  \
    S(gethostname, NeedsBigProcessLock::No)                 \
    S(getkeymap, NeedsBigProcessLock::No)                   \
    S(getpeername, NeedsBigProcessLock::Yes)                \
    S(getpgid, NeedsBigProcessLock::Yes)                    \
    S(getpgrp, NeedsBigProcessLock::Yes)                    \
    S(getpid, NeedsBigProcessLock::No)                      \
    S(getppid, NeedsBigProcessLock::Yes)                    \
    S(getrandom, NeedsBigProcessLock::No)                   \
    S(getresgid, NeedsBigProcessLock::Yes)                  \
    S(getresuid, NeedsBigProcessLock::Yes)                  \
    S(getsid, NeedsBigProcessLock::Yes)                     \
    S(getsockname, NeedsBigProcessLock::Yes)                \
    S(getsockopt, NeedsBigProcessLock::Yes)                 \
    S(gettid, NeedsBigProcessLock::No)                      \
    S(getuid, NeedsBigProcessLock::Yes)                     \
    S(inode_watcher_add_watch, NeedsBigProcessLock::Yes)    \
    S(inode_watcher_remove_watch, NeedsBigProcessLock::Yes) \
    S(ioctl, NeedsBigProcessLock::Yes)                      \
    S(join_thread, NeedsBigProcessLock::Yes)                \
    S(kill, NeedsBigProcessLock::Yes)                       \
    S(kill_thread, NeedsBigProcessLock::Yes)                \
    S(killpg, NeedsBigProcessLock::Yes)                     \
    S(link, NeedsBigProcessLock::Yes)                       \
    S(listen, NeedsBigProcessLock::Yes)                     \
    S(lseek, NeedsBigProcessLock::Yes)                      \
    S(madvise, NeedsBigProcessLock::Yes)                    \
    S(map_time_page, NeedsBigProcessLock::Yes)              \
    S(mkdir, NeedsBigProcessLock::Yes)                      \
    S(mknod, NeedsBigProcessLock::Yes)                      \
    S(mmap, NeedsBigProcessLock::Yes)                       \
    S(mount, NeedsBigProcessLock::Yes)                      \
    S(mprotect, NeedsBigProcessLock::Yes)                   \
    S(mremap, NeedsBigProcessLock::Yes)                     \
    S(msync, NeedsBigProcessLock::Yes)                      \
    S(msyscall, NeedsBigProcessLock::Yes)                   \
    S(munmap, NeedsBigProcessLock::Yes)                     \
    S(open, NeedsBigProcessLock::Yes)                       \
    S(perf_event, NeedsBigProcessLock::Yes)                 \
    S(perf_register_string, NeedsBigProcessLock::Yes)       \
    S(pipe, NeedsBigProcessLock::Yes)                       \
    S(pledge, NeedsBigProcessLock::Yes)                     \
    S(poll, NeedsBigProcessLock::Yes)                       \
    S(prctl, NeedsBigProcessLock::Yes)                      \
    S(profiling_disable, NeedsBigProcessLock::Yes)          \
    S(profiling_enable, NeedsBigProcessLock::Yes)           \
    S(profiling_free_buffer, NeedsBigProcessLock::Yes)      \
    S(ptrace, NeedsBigProcessLock::Yes)                     \
    S(ptsname, NeedsBigProcessLock::Yes)                    \
    S(purge, NeedsBigProcessLock::Yes)                      \
    S(read, NeedsBigProcessLock::Yes)                       \
    S(pread, NeedsBigProcessLock::Yes)                      \
    S(readlink, NeedsBigProcessLock::Yes)                   \
    S(readv, NeedsBigProcessLock::Yes)                      \
    S(realpath, NeedsBigProcessLock::Yes)                   \
    S(recvfd, NeedsBigProcessLock::Yes)                     \
    S(recvmsg, NeedsBigProcessLock::Yes)                    \
    S(rename, NeedsBigProcessLock::Yes)                     \
    S(rmdir, NeedsBigProcessLock::Yes)                      \
    S(sched_getparam, NeedsBigProcessLock::Yes)             \
    S(sched_setparam, NeedsBigProcessLock::Yes)             \
    S(sendfd, NeedsBigProcessLock::Yes)                     \
    S(sendmsg, NeedsBigProcessLock::Yes)                    \
    S(set_coredump_metadata, NeedsBigProcessLock::Yes)      \
    S(set_mmap_name, NeedsBigProcessLock::Yes)              \
    S(set_process_name, NeedsBigProcessLock::Yes)           \
    S(set_thread_name, NeedsBigProcessLock::Yes)            \
    S(setegid, NeedsBigProcessLock::Yes)                    \
    S(seteuid, NeedsBigProcessLock::Yes)                    \
    S(setgid, NeedsBigProcessLock::Yes)                     \
    S(setgroups, NeedsBigProcessLock::Yes)                  \
    S(sethostname, NeedsBigProcessLock::No)                 \
    S(setkeymap, NeedsBigProcessLock::Yes)                  \
    S(setpgid, NeedsBigProcessLock::Yes)                    \
    S(setresgid, NeedsBigProcessLock::Yes)                  \
    S(setresuid, NeedsBigProcessLock::Yes)                  \
    S(setreuid, NeedsBigProcessLock::Yes)                   \
    S(setsid, NeedsBigProcessLock::Yes)                     \
    S(setsockopt, NeedsBigProcessLock::Yes)                 \
    S(setuid, NeedsBigProcessLock::Yes)                     \
    S(shutdown, NeedsBigProcessLock::Yes)                   \
    S(sigaction, NeedsBigProcessLock::Yes)                  \
    S(sigaltstack, NeedsBigProcessLock::Yes)                \
    S(sigpending, NeedsBigProcessLock::Yes)                 \
    S(sigprocmask, NeedsBigProcessLock::Yes)                \
    S(sigreturn, NeedsBigProcessLock::Yes)                  \
    S(sigtimedwait, NeedsBigProcessLock::Yes)               \
    S(socket, NeedsBigProcessLock::Yes)                     \
    S(socketpair, NeedsBigProcessLock::Yes)                 \
    S(stat, NeedsBigProcessLock::Yes)                       \
    S(statvfs, NeedsBigProcessLock::Yes)                    \
    S(symlink, NeedsBigProcessLock::Yes)                    \
    S(sync, NeedsBigProcessLock::No)                        \
    S(sysconf, NeedsBigProcessLock::No)                     \
    S(times, NeedsBigProcessLock::Yes)                      \
    S(ttyname, NeedsBigProcessLock::Yes)                    \
    S(umask, NeedsBigProcessLock::Yes)                      \
    S(umount, NeedsBigProcessLock::Yes)                     \
    S(uname, NeedsBigProcessLock::No)                       \
    S(unlink, NeedsBigProcessLock::Yes)                     \
    S(unveil, NeedsBigProcessLock::Yes)                     \
    S(utime, NeedsBigProcessLock::Yes)                      \
    S(waitid, NeedsBigProcessLock::Yes)                     \
    S(write, NeedsBigProcessLock::Yes)                      \
    S(writev, NeedsBigProcessLock::Yes)                     \
    S(yield, NeedsBigProcessLock::No)

namespace Syscall {

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) SC_##sys_call,
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
        __Count
};

constexpr StringView to_string(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) \
    case SC_##sys_call:                           \
        return #sys_call##sv;
        ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
    default:
        break;
    }
    return "Unknown"sv;
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
    void* addr;
    size_t size;
    size_t alignment;
    int32_t prot;
    int32_t flags;
    int32_t fd;
    int64_t offset;
    StringArgument name;
};

struct SC_mremap_params {
    void* old_address;
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
    sockaddr* addr;
    socklen_t* addrlen;
    int sockfd;
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
    const void* value;
    int sockfd;
    int level;
    int option;
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
    unsigned int detach_state = 0; // JOINABLE or DETACHED
    int schedule_priority = 30;    // THREAD_PRIORITY_NORMAL
    // FIXME: Implement guard pages in create_thread (unreadable pages at "overflow" end of stack)
    // "If an implementation rounds up the value of guardsize to a multiple of {PAGESIZE},
    // a call to pthread_attr_getguardsize() specifying attr shall store in the guardsize
    // parameter the guard size specified by the previous pthread_attr_setguardsize() function call"
    // ... ok, if you say so posix. Guess we get to lie to people about guard page size
    unsigned int guard_page_size = 0;          // Rounded up to PAGE_SIZE
    unsigned int reported_guard_page_size = 0; // The lie we tell callers
    unsigned int stack_size = 4 * MiB;         // Default PTHREAD_STACK_MIN
    void* stack_location;                      // nullptr means any, o.w. process virtual address
#    if ARCH(X86_64)
    FlatPtr rdi;
    FlatPtr rsi;
    FlatPtr rcx;
    FlatPtr rdx;
#    endif
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
    int dirfd;
    int follow_symlinks;
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
    StringArgument target;
    StringArgument fs_type;
    int source_fd;
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
    int dirfd;
    int follow_symlinks;
};

struct SC_ptrace_buf_params {
    MutableBufferArgument<u8, size_t> buf;
};

struct SC_ptrace_params {
    int request;
    pid_t tid;
    void* addr;
    FlatPtr data;
};

struct SC_set_coredump_metadata_params {
    StringArgument key;
    StringArgument value;
};

struct SC_inode_watcher_add_watch_params {
    StringArgument user_path;
    int fd;
    u32 event_mask;
};

struct SC_statvfs_params {
    StringArgument path;
    struct statvfs* buf;
};

struct SC_chmod_params {
    int dirfd;
    StringArgument path;
    u16 mode;
    int follow_symlinks;
};

void initialize();
int sync();

#    if ARCH(I386) || ARCH(X86_64)
inline uintptr_t invoke(Function function)
{
    uintptr_t result;
#        if ARCH(I386)
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function)
                 : "memory");
#        else
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function)
                 : "rcx", "r11", "memory");
#        endif
    return result;
}

template<typename T1>
inline uintptr_t invoke(Function function, T1 arg1)
{
    uintptr_t result;
#        if ARCH(I386)
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1)
                 : "memory");
#        else
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1)
                 : "rcx", "r11", "memory");
#        endif
    return result;
}

template<typename T1, typename T2>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2)
{
    uintptr_t result;
#        if ARCH(I386)
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "c"((uintptr_t)arg2)
                 : "memory");
#        else
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2)
                 : "rcx", "r11", "memory");
#        endif
    return result;
}

template<typename T1, typename T2, typename T3>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
    uintptr_t result;
#        if ARCH(I386)
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "c"((uintptr_t)arg2), "b"((uintptr_t)arg3)
                 : "memory");
#        else
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2), "b"((uintptr_t)arg3)
                 : "rcx", "r11", "memory");
#        endif
    return result;
}

template<typename T1, typename T2, typename T3, typename T4>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2, T3 arg3, T4 arg4)
{
    uintptr_t result;
#        if ARCH(I386)
    asm volatile("int $0x82"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "c"((uintptr_t)arg2), "b"((uintptr_t)arg3), "S"((uintptr_t)arg4)
                 : "memory");
#        else
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2), "b"((uintptr_t)arg3), "S"((uintptr_t)arg4)
                 : "memory");
#        endif
    return result;
}
#    endif
#endif

}

#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) using Syscall::SC_##sys_call;
ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL

}

using namespace Kernel;
