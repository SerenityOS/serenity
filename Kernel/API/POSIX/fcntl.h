/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

#define F_DUPFD 0
#define F_GETFD 1
#define F_SETFD 2
#define F_GETFL 3
#define F_SETFL 4
#define F_ISTTY 5
#define F_GETLK 6
#define F_SETLK 7
#define F_SETLKW 8
#define F_DUPFD_CLOEXEC 9

#define FD_CLOEXEC 1

#define O_RDONLY (1 << 0)
#define O_WRONLY (1 << 1)
#define O_RDWR (O_RDONLY | O_WRONLY)
#define O_ACCMODE (O_RDONLY | O_WRONLY)
#define O_EXEC (1 << 2)
#define O_CREAT (1 << 3)
#define O_EXCL (1 << 4)
#define O_NOCTTY (1 << 5)
#define O_TRUNC (1 << 6)
#define O_APPEND (1 << 7)
#define O_NONBLOCK (1 << 8)
#define O_DIRECTORY (1 << 9)
#define O_NOFOLLOW (1 << 10)
#define O_CLOEXEC (1 << 11)
#define O_DIRECT (1 << 12)
#define O_SYNC (1 << 13)

#define F_RDLCK ((short)0)
#define F_WRLCK ((short)1)
#define F_UNLCK ((short)2)

#define AT_FDCWD -100
#define AT_SYMLINK_NOFOLLOW 0x100
#define AT_REMOVEDIR 0x200
#define AT_EACCESS 0x400

struct flock {
    short l_type;
    short l_whence;
    off_t l_start;
    off_t l_len;
    pid_t l_pid;
};

#ifdef __cplusplus
}
#endif
