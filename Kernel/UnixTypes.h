/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>
#include <Kernel/API/POSIX/fcntl.h>
#include <Kernel/API/POSIX/futex.h>
#include <Kernel/API/POSIX/net/if.h>
#include <Kernel/API/POSIX/net/if_arp.h>
#include <Kernel/API/POSIX/net/route.h>
#include <Kernel/API/POSIX/netinet/in.h>
#include <Kernel/API/POSIX/signal.h>
#include <Kernel/API/POSIX/sys/mman.h>
#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <Kernel/API/POSIX/sys/un.h>
#include <Kernel/API/POSIX/termios.h>
#include <Kernel/API/POSIX/time.h>

// Kernel internal options.
#define O_NOFOLLOW_NOERROR (1 << 29)
#define O_UNLINK_INTERNAL (1 << 30)

#define MS_NODEV (1 << 0)
#define MS_NOEXEC (1 << 1)
#define MS_NOSUID (1 << 2)
#define MS_BIND (1 << 3)
#define MS_RDONLY (1 << 4)
#define MS_REMOUNT (1 << 5)

enum {
    _SC_MONOTONIC_CLOCK,
    _SC_NPROCESSORS_CONF,
    _SC_NPROCESSORS_ONLN,
    _SC_OPEN_MAX,
    _SC_TTY_NAME_MAX,
    _SC_PAGESIZE,
    _SC_GETPW_R_SIZE_MAX,
    _SC_CLK_TCK,
};

enum {
    PERF_EVENT_SAMPLE = 1,
    PERF_EVENT_MALLOC = 2,
    PERF_EVENT_FREE = 4,
    PERF_EVENT_MMAP = 8,
    PERF_EVENT_MUNMAP = 16,
    PERF_EVENT_PROCESS_CREATE = 32,
    PERF_EVENT_PROCESS_EXEC = 64,
    PERF_EVENT_PROCESS_EXIT = 128,
    PERF_EVENT_THREAD_CREATE = 256,
    PERF_EVENT_THREAD_EXIT = 512,
    PERF_EVENT_CONTEXT_SWITCH = 1024,
    PERF_EVENT_KMALLOC = 2048,
    PERF_EVENT_KFREE = 4096,
    PERF_EVENT_PAGE_FAULT = 8192,
    PERF_EVENT_SYSCALL = 16384,
    PERF_EVENT_SIGNPOST = 32768,
};

#define WNOHANG 1
#define WUNTRACED 2
#define WSTOPPED WUNTRACED
#define WEXITED 4
#define WCONTINUED 8
#define WNOWAIT 0x1000000

#define R_OK 4
#define W_OK 2
#define X_OK 1
#define F_OK 0

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

#define MADV_SET_VOLATILE 0x100
#define MADV_SET_NONVOLATILE 0x200

#define F_DUPFD 0
#define F_GETFD 1
#define F_SETFD 2
#define F_GETFL 3
#define F_SETFL 4
#define F_ISTTY 5
#define F_GETLK 6
#define F_SETLK 7
#define F_SETLKW 8

#define FD_CLOEXEC 1

// Avoid interference with AK/Types.h and LibC/sys/types.h by defining *separate* names:
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ThreadID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, SessionID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessGroupID);

struct tms {
    clock_t tms_utime;
    clock_t tms_stime;
    clock_t tms_cutime;
    clock_t tms_cstime;
};

typedef i64 off_t;
typedef i64 time_t;

typedef u32 blksize_t;
typedef u32 blkcnt_t;

#define POLLIN (1u << 0)
#define POLLPRI (1u << 1)
#define POLLOUT (1u << 2)
#define POLLERR (1u << 3)
#define POLLHUP (1u << 4)
#define POLLNVAL (1u << 5)
#define POLLRDHUP (1u << 13)

struct pollfd {
    int fd;
    short events;
    short revents;
};

typedef u32 __u32;
typedef u16 __u16;
typedef u8 __u8;
typedef int __s32;
typedef short __s16;

typedef u32 useconds_t;
typedef i32 suseconds_t;

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

typedef enum {
    P_ALL = 1,
    P_PID,
    P_PGID
} idtype_t;

#define UTSNAME_ENTRY_LEN 65

struct utsname {
    char sysname[UTSNAME_ENTRY_LEN];
    char nodename[UTSNAME_ENTRY_LEN];
    char release[UTSNAME_ENTRY_LEN];
    char version[UTSNAME_ENTRY_LEN];
    char machine[UTSNAME_ENTRY_LEN];
};

struct iovec {
    void* iov_base;
    size_t iov_len;
};

struct sched_param {
    int sched_priority;
};

#define AT_FDCWD -100
#define AT_SYMLINK_NOFOLLOW 0x100

#define PURGE_ALL_VOLATILE 0x1
#define PURGE_ALL_CLEAN_INODE 0x2

#define PT_TRACE_ME 1
#define PT_ATTACH 2
#define PT_CONTINUE 3
#define PT_SYSCALL 4
#define PT_GETREGS 5
#define PT_DETACH 6
#define PT_PEEK 7
#define PT_POKE 8
#define PT_SETREGS 9
#define PT_POKEDEBUG 10
#define PT_PEEKDEBUG 11

// Used in struct dirent
enum {
    DT_UNKNOWN = 0,
#define DT_UNKNOWN DT_UNKNOWN
    DT_FIFO = 1,
#define DT_FIFO DT_FIFO
    DT_CHR = 2,
#define DT_CHR DT_CHR
    DT_DIR = 4,
#define DT_DIR DT_DIR
    DT_BLK = 6,
#define DT_BLK DT_BLK
    DT_REG = 8,
#define DT_REG DT_REG
    DT_LNK = 10,
#define DT_LNK DT_LNK
    DT_SOCK = 12,
#define DT_SOCK DT_SOCK
    DT_WHT = 14
#define DT_WHT DT_WHT
};

typedef uint64_t fsblkcnt_t;
typedef uint64_t fsfilcnt_t;

#define ST_RDONLY 0x1
#define ST_NOSUID 0x2

struct statvfs {
    unsigned long f_bsize;
    unsigned long f_frsize;
    fsblkcnt_t f_blocks;
    fsblkcnt_t f_bfree;
    fsblkcnt_t f_bavail;

    fsfilcnt_t f_files;
    fsfilcnt_t f_ffree;
    fsfilcnt_t f_favail;

    unsigned long f_fsid;
    unsigned long f_flag;
    unsigned long f_namemax;
};
