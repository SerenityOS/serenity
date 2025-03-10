/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Userspace.h>
#include <Kernel/API/POSIX/sched.h>

#ifdef KERNEL
#    include <AK/Error.h>
#    include <Kernel/Arch/RegisterState.h>
#endif

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
#define ENUMERATE_SYSCALLS(S)                              \
    S(accept4, NeedsBigProcessLock::No)                    \
    S(adjtime, NeedsBigProcessLock::No)                    \
    S(alarm, NeedsBigProcessLock::No)                      \
    S(archctl, NeedsBigProcessLock::No)                    \
    S(anon_create, NeedsBigProcessLock::No)                \
    S(annotate_mapping, NeedsBigProcessLock::No)           \
    S(bind, NeedsBigProcessLock::No)                       \
    S(bindmount, NeedsBigProcessLock::No)                  \
    S(chdir, NeedsBigProcessLock::No)                      \
    S(chmod, NeedsBigProcessLock::No)                      \
    S(chown, NeedsBigProcessLock::No)                      \
    S(clock_gettime, NeedsBigProcessLock::No)              \
    S(clock_nanosleep, NeedsBigProcessLock::No)            \
    S(clock_getres, NeedsBigProcessLock::No)               \
    S(clock_settime, NeedsBigProcessLock::No)              \
    S(close, NeedsBigProcessLock::No)                      \
    S(connect, NeedsBigProcessLock::No)                    \
    S(copy_mount, NeedsBigProcessLock::No)                 \
    S(create_inode_watcher, NeedsBigProcessLock::No)       \
    S(create_thread, NeedsBigProcessLock::No)              \
    S(dbgputstr, NeedsBigProcessLock::No)                  \
    S(detach_thread, NeedsBigProcessLock::No)              \
    S(disown, NeedsBigProcessLock::No)                     \
    S(dump_backtrace, NeedsBigProcessLock::No)             \
    S(dup2, NeedsBigProcessLock::No)                       \
    S(execve, NeedsBigProcessLock::Yes)                    \
    S(exit, NeedsBigProcessLock::Yes)                      \
    S(exit_thread, NeedsBigProcessLock::Yes)               \
    S(faccessat, NeedsBigProcessLock::No)                  \
    S(fchdir, NeedsBigProcessLock::No)                     \
    S(fchmod, NeedsBigProcessLock::No)                     \
    S(fchown, NeedsBigProcessLock::No)                     \
    S(fcntl, NeedsBigProcessLock::No)                      \
    S(fork, NeedsBigProcessLock::No)                       \
    S(fstat, NeedsBigProcessLock::No)                      \
    S(fstatvfs, NeedsBigProcessLock::No)                   \
    S(fsopen, NeedsBigProcessLock::No)                     \
    S(fsmount, NeedsBigProcessLock::No)                    \
    S(fsync, NeedsBigProcessLock::No)                      \
    S(ftruncate, NeedsBigProcessLock::No)                  \
    S(futex, NeedsBigProcessLock::Yes)                     \
    S(futimens, NeedsBigProcessLock::No)                   \
    S(get_dir_entries, NeedsBigProcessLock::No)            \
    S(get_root_session_id, NeedsBigProcessLock::No)        \
    S(get_stack_bounds, NeedsBigProcessLock::No)           \
    S(getcwd, NeedsBigProcessLock::No)                     \
    S(getegid, NeedsBigProcessLock::No)                    \
    S(geteuid, NeedsBigProcessLock::No)                    \
    S(getgid, NeedsBigProcessLock::No)                     \
    S(getgroups, NeedsBigProcessLock::No)                  \
    S(gethostname, NeedsBigProcessLock::No)                \
    S(getkeymap, NeedsBigProcessLock::No)                  \
    S(getpeername, NeedsBigProcessLock::No)                \
    S(getpgid, NeedsBigProcessLock::No)                    \
    S(getpgrp, NeedsBigProcessLock::No)                    \
    S(getpid, NeedsBigProcessLock::No)                     \
    S(getppid, NeedsBigProcessLock::No)                    \
    S(getrandom, NeedsBigProcessLock::No)                  \
    S(getresgid, NeedsBigProcessLock::No)                  \
    S(getresuid, NeedsBigProcessLock::No)                  \
    S(getrusage, NeedsBigProcessLock::No)                  \
    S(getsid, NeedsBigProcessLock::No)                     \
    S(getsockname, NeedsBigProcessLock::No)                \
    S(getsockopt, NeedsBigProcessLock::No)                 \
    S(gettid, NeedsBigProcessLock::No)                     \
    S(getuid, NeedsBigProcessLock::No)                     \
    S(inode_watcher_add_watch, NeedsBigProcessLock::No)    \
    S(inode_watcher_remove_watch, NeedsBigProcessLock::No) \
    S(ioctl, NeedsBigProcessLock::No)                      \
    S(join_thread, NeedsBigProcessLock::No)                \
    S(kill, NeedsBigProcessLock::No)                       \
    S(kill_thread, NeedsBigProcessLock::No)                \
    S(killpg, NeedsBigProcessLock::No)                     \
    S(link, NeedsBigProcessLock::No)                       \
    S(listen, NeedsBigProcessLock::No)                     \
    S(lseek, NeedsBigProcessLock::No)                      \
    S(madvise, NeedsBigProcessLock::No)                    \
    S(map_time_page, NeedsBigProcessLock::No)              \
    S(mkdir, NeedsBigProcessLock::No)                      \
    S(mknod, NeedsBigProcessLock::No)                      \
    S(mmap, NeedsBigProcessLock::No)                       \
    S(mprotect, NeedsBigProcessLock::No)                   \
    S(mremap, NeedsBigProcessLock::No)                     \
    S(msync, NeedsBigProcessLock::No)                      \
    S(munmap, NeedsBigProcessLock::No)                     \
    S(open, NeedsBigProcessLock::No)                       \
    S(perf_event, NeedsBigProcessLock::Yes)                \
    S(perf_register_string, NeedsBigProcessLock::Yes)      \
    S(pipe, NeedsBigProcessLock::No)                       \
    S(pledge, NeedsBigProcessLock::No)                     \
    S(poll, NeedsBigProcessLock::No)                       \
    S(posix_fallocate, NeedsBigProcessLock::No)            \
    S(prctl, NeedsBigProcessLock::No)                      \
    S(profiling_disable, NeedsBigProcessLock::Yes)         \
    S(profiling_enable, NeedsBigProcessLock::Yes)          \
    S(profiling_free_buffer, NeedsBigProcessLock::Yes)     \
    S(ptrace, NeedsBigProcessLock::Yes)                    \
    S(purge, NeedsBigProcessLock::Yes)                     \
    S(read, NeedsBigProcessLock::Yes)                      \
    S(pread, NeedsBigProcessLock::Yes)                     \
    S(readlink, NeedsBigProcessLock::No)                   \
    S(readv, NeedsBigProcessLock::Yes)                     \
    S(realpath, NeedsBigProcessLock::No)                   \
    S(recvfd, NeedsBigProcessLock::No)                     \
    S(recvmsg, NeedsBigProcessLock::Yes)                   \
    S(rename, NeedsBigProcessLock::No)                     \
    S(remount, NeedsBigProcessLock::No)                    \
    S(rmdir, NeedsBigProcessLock::No)                      \
    S(scheduler_get_parameters, NeedsBigProcessLock::No)   \
    S(scheduler_set_parameters, NeedsBigProcessLock::No)   \
    S(sendfd, NeedsBigProcessLock::No)                     \
    S(sendmsg, NeedsBigProcessLock::Yes)                   \
    S(set_mmap_name, NeedsBigProcessLock::No)              \
    S(setegid, NeedsBigProcessLock::No)                    \
    S(seteuid, NeedsBigProcessLock::No)                    \
    S(setgid, NeedsBigProcessLock::No)                     \
    S(setgroups, NeedsBigProcessLock::No)                  \
    S(sethostname, NeedsBigProcessLock::No)                \
    S(setkeymap, NeedsBigProcessLock::No)                  \
    S(setpgid, NeedsBigProcessLock::No)                    \
    S(setregid, NeedsBigProcessLock::No)                   \
    S(setresgid, NeedsBigProcessLock::No)                  \
    S(setresuid, NeedsBigProcessLock::No)                  \
    S(setreuid, NeedsBigProcessLock::No)                   \
    S(setsid, NeedsBigProcessLock::No)                     \
    S(setsockopt, NeedsBigProcessLock::No)                 \
    S(setuid, NeedsBigProcessLock::No)                     \
    S(shutdown, NeedsBigProcessLock::No)                   \
    S(sigaction, NeedsBigProcessLock::Yes)                 \
    S(sigaltstack, NeedsBigProcessLock::Yes)               \
    S(sigpending, NeedsBigProcessLock::No)                 \
    S(sigprocmask, NeedsBigProcessLock::No)                \
    S(sigreturn, NeedsBigProcessLock::No)                  \
    S(sigsuspend, NeedsBigProcessLock::No)                 \
    S(sigtimedwait, NeedsBigProcessLock::No)               \
    S(socket, NeedsBigProcessLock::No)                     \
    S(socketpair, NeedsBigProcessLock::No)                 \
    S(stat, NeedsBigProcessLock::No)                       \
    S(statvfs, NeedsBigProcessLock::No)                    \
    S(symlink, NeedsBigProcessLock::No)                    \
    S(sync, NeedsBigProcessLock::No)                       \
    S(sysconf, NeedsBigProcessLock::No)                    \
    S(times, NeedsBigProcessLock::No)                      \
    S(umask, NeedsBigProcessLock::No)                      \
    S(umount, NeedsBigProcessLock::No)                     \
    S(uname, NeedsBigProcessLock::No)                      \
    S(unlink, NeedsBigProcessLock::No)                     \
    S(unshare_attach, NeedsBigProcessLock::No)             \
    S(unshare_create, NeedsBigProcessLock::No)             \
    S(unveil, NeedsBigProcessLock::No)                     \
    S(utime, NeedsBigProcessLock::No)                      \
    S(utimensat, NeedsBigProcessLock::No)                  \
    S(waitid, NeedsBigProcessLock::Yes)                    \
    S(write, NeedsBigProcessLock::Yes)                     \
    S(pwritev, NeedsBigProcessLock::Yes)                   \
    S(yield, NeedsBigProcessLock::No)

namespace Syscall {

#ifdef KERNEL
ErrorOr<FlatPtr> handle(RegisterState&, FlatPtr function, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3, FlatPtr arg4);
#endif

enum Function {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) SC_##sys_call,
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
        __Count
};

#ifdef AK_OS_SERENITY
struct StringArgument {
    char const* characters;
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
    u32 const* sigmask;
};

struct SC_clock_nanosleep_params {
    int clock_id;
    int flags;
    const struct timespec* requested_sleep;
    struct timespec* remaining_sleep;
};

struct SC_clock_getres_params {
    int clock_id;
    struct timespec* result;
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
    void const* value;
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
        timespec const* timeout;
        uintptr_t val2;
    };
    u32* userspace_address2;
    u32 val3;
};

struct SC_setkeymap_params {
    u32 const* map;
    u32 const* shift_map;
    u32 const* alt_map;
    u32 const* altgr_map;
    u32 const* shift_altgr_map;
    StringArgument map_name;
};

struct SC_unshare_create_params {
    int type;
    int flags;
};

struct SC_unshare_attach_params {
    int type;
    int id;
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
    unsigned int stack_size = 1 * MiB;         // Equal to Thread::default_userspace_stack_size
    void* stack_location;                      // nullptr means any, o.w. process virtual address
    void* (*entry)(void*);
    void* entry_argument;
    void* tls_pointer;
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
    int dirfd;
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
    dev_t dev;
    int dirfd;
};

struct SC_symlink_params {
    StringArgument target;
    StringArgument linkpath;
    int dirfd;
};

struct SC_rename_params {
    int olddirfd;
    StringArgument old_path;
    int newdirfd;
    StringArgument new_path;
};

struct SC_fsopen_params {
    StringArgument fs_type;
    int flags;
};

struct SC_fsmount_params {
    int vfs_root_context_id;
    int mount_fd;
    StringArgument target;
    int source_fd;
};

struct SC_bindmount_params {
    int vfs_root_context_id;
    StringArgument target;
    int source_fd;
    int flags;
};

struct SC_remount_params {
    int vfs_root_context_id;
    StringArgument target;
    int flags;
};

struct SC_umount_params {
    int vfs_root_context_id;
    StringArgument target;
};

struct SC_copy_mount_params {
    int original_vfs_root_context_id;
    int target_vfs_root_context_id;
    StringArgument original_path;
    StringArgument target_path;
    int flags;
};

struct SC_pledge_params {
    StringArgument promises;
    StringArgument execpromises;
};

struct SC_unveil_params {
    int flags;
    StringArgument path;
    StringArgument permissions;
};

struct SC_utimensat_params {
    int dirfd;
    StringArgument path;
    struct timespec const* times;
    int flag;
};

struct SC_futimens_params {
    int fd;
    struct timespec const* times;
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

enum class SchedulerParametersMode : bool {
    Process,
    Thread,
};

struct SC_scheduler_parameters_params {
    pid_t pid_or_tid;
    SchedulerParametersMode mode;
    struct sched_param parameters;
};

struct SC_faccessat_params {
    int dirfd;
    StringArgument pathname;
    int mode;
    int flags;
};

void initialize();
int sync();

#    if ARCH(X86_64) || ARCH(AARCH64) || ARCH(RISCV64)
inline uintptr_t invoke(Function function)
{
#        if ARCH(X86_64)
    uintptr_t result;
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function)
                 : "rcx", "r11", "memory");
#        elif ARCH(AARCH64)
    uintptr_t result;
    register uintptr_t x0 asm("x0");
    register uintptr_t x8 asm("x8") = function;
    asm volatile("svc #0"
                 : "=r"(x0)
                 : "r"(x8)
                 : "memory");
    result = x0;
#        elif ARCH(RISCV64)
    register uintptr_t a7 asm("a7") = function;
    register uintptr_t result asm("a0");
    asm volatile("ecall"
                 : "=r"(result)
                 : "r"(a7)
                 : "memory");
#        endif
    return result;
}

template<typename T1>
inline uintptr_t invoke(Function function, T1 arg1)
{
#        if ARCH(X86_64)
    uintptr_t result;
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1)
                 : "rcx", "r11", "memory");
#        elif ARCH(AARCH64)
    uintptr_t result;
    register uintptr_t x0 asm("x0");
    register uintptr_t x1 asm("x1") = arg1;
    register uintptr_t x8 asm("x8") = function;
    asm volatile("svc #0"
                 : "=r"(x0)
                 : "r"(x1), "r"(x8)
                 : "memory");
    result = x0;
#        elif ARCH(RISCV64)
    register uintptr_t a0 asm("a0") = arg1;
    register uintptr_t a7 asm("a7") = function;
    register uintptr_t result asm("a0");
    asm volatile("ecall"
                 : "=r"(result)
                 : "0"(a0), "r"(a7)
                 : "memory");
#        endif
    return result;
}

template<typename T1, typename T2>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2)
{
#        if ARCH(X86_64)
    uintptr_t result;
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2)
                 : "rcx", "r11", "memory");
#        elif ARCH(AARCH64)
    uintptr_t result;
    register uintptr_t x0 asm("x0");
    register uintptr_t x1 asm("x1") = arg1;
    register uintptr_t x2 asm("x2") = arg2;
    register uintptr_t x8 asm("x8") = function;
    asm volatile("svc #0"
                 : "=r"(x0)
                 : "r"(x1), "r"(x2), "r"(x8)
                 : "memory");
    result = x0;
#        elif ARCH(RISCV64)
    register uintptr_t a0 asm("a0") = arg1;
    register uintptr_t a1 asm("a1") = arg2;
    register uintptr_t a7 asm("a7") = function;
    register uintptr_t result asm("a0");
    asm volatile("ecall"
                 : "=r"(result)
                 : "0"(a0), "r"(a1), "r"(a7)
                 : "memory");
#        endif
    return result;
}

template<typename T1, typename T2, typename T3>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2, T3 arg3)
{
#        if ARCH(X86_64)
    uintptr_t result;
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2), "b"((uintptr_t)arg3)
                 : "rcx", "r11", "memory");
#        elif ARCH(AARCH64)
    uintptr_t result;
    register uintptr_t x0 asm("x0");
    register uintptr_t x1 asm("x1") = arg1;
    register uintptr_t x2 asm("x2") = arg2;
    register uintptr_t x3 asm("x3") = arg3;
    register uintptr_t x8 asm("x8") = function;
    asm volatile("svc #0"
                 : "=r"(x0)
                 : "r"(x1), "r"(x2), "r"(x3), "r"(x8)
                 : "memory");
    result = x0;
#        elif ARCH(RISCV64)
    register uintptr_t a0 asm("a0") = arg1;
    register uintptr_t a1 asm("a1") = arg2;
    register uintptr_t a2 asm("a2") = arg3;
    register uintptr_t a7 asm("a7") = function;
    register uintptr_t result asm("a0");
    asm volatile("ecall"
                 : "=r"(result)
                 : "0"(a0), "r"(a1), "r"(a2), "r"(a7)
                 : "memory");
#        endif
    return result;
}

template<typename T1, typename T2, typename T3, typename T4>
inline uintptr_t invoke(Function function, T1 arg1, T2 arg2, T3 arg3, T4 arg4)
{
#        if ARCH(X86_64)
    uintptr_t result;
    asm volatile("syscall"
                 : "=a"(result)
                 : "a"(function), "d"((uintptr_t)arg1), "D"((uintptr_t)arg2), "b"((uintptr_t)arg3), "S"((uintptr_t)arg4)
                 : "rcx", "r11", "memory");
#        elif ARCH(AARCH64)
    uintptr_t result;
    register uintptr_t x0 asm("x0");
    register uintptr_t x1 asm("x1") = arg1;
    register uintptr_t x2 asm("x2") = arg2;
    register uintptr_t x3 asm("x3") = arg3;
    register uintptr_t x4 asm("x4") = arg4;
    register uintptr_t x8 asm("x8") = function;
    asm volatile("svc #0"
                 : "=r"(x0)
                 : "r"(x1), "r"(x2), "r"(x3), "r"(x4), "r"(x8)
                 : "memory");
    result = x0;
#        elif ARCH(RISCV64)
    register uintptr_t a0 asm("a0") = arg1;
    register uintptr_t a1 asm("a1") = arg2;
    register uintptr_t a2 asm("a2") = arg3;
    register uintptr_t a3 asm("a3") = arg4;
    register uintptr_t a7 asm("a7") = function;
    register uintptr_t result asm("a0");
    asm volatile("ecall"
                 : "=r"(result)
                 : "0"(a0), "r"(a1), "r"(a2), "r"(a3), "r"(a7)
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
