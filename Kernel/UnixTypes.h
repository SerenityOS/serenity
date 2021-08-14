/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>
#include <Kernel/API/POSIX/dirent.h>
#include <Kernel/API/POSIX/fcntl.h>
#include <Kernel/API/POSIX/futex.h>
#include <Kernel/API/POSIX/net/if.h>
#include <Kernel/API/POSIX/net/if_arp.h>
#include <Kernel/API/POSIX/net/route.h>
#include <Kernel/API/POSIX/netinet/in.h>
#include <Kernel/API/POSIX/poll.h>
#include <Kernel/API/POSIX/sched.h>
#include <Kernel/API/POSIX/serenity.h>
#include <Kernel/API/POSIX/signal.h>
#include <Kernel/API/POSIX/stdio.h>
#include <Kernel/API/POSIX/sys/mman.h>
#include <Kernel/API/POSIX/sys/ptrace.h>
#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <Kernel/API/POSIX/sys/time.h>
#include <Kernel/API/POSIX/sys/times.h>
#include <Kernel/API/POSIX/sys/uio.h>
#include <Kernel/API/POSIX/sys/un.h>
#include <Kernel/API/POSIX/sys/utsname.h>
#include <Kernel/API/POSIX/sys/wait.h>
#include <Kernel/API/POSIX/termios.h>
#include <Kernel/API/POSIX/time.h>
#include <Kernel/API/POSIX/unistd.h>

// Kernel internal options.
#define O_NOFOLLOW_NOERROR (1 << 29)
#define O_UNLINK_INTERNAL (1 << 30)
// Avoid interference with AK/Types.h and LibC/sys/types.h by defining *separate* names:
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ThreadID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, SessionID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessGroupID);

typedef u32 __u32;
typedef u16 __u16;
typedef u8 __u8;
typedef int __s32;
typedef short __s16;

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
