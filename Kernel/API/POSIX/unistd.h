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

#define STDIN_FILENO 0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2

#define R_OK 4
#define W_OK 2
#define X_OK 1
#define F_OK 0

#define MS_NODEV (1 << 0)
#define MS_NOEXEC (1 << 1)
#define MS_NOSUID (1 << 2)
#define MS_BIND (1 << 3)
#define MS_RDONLY (1 << 4)
#define MS_REMOUNT (1 << 5)
#define MS_WXALLOWED (1 << 6)
#define MS_AXALLOWED (1 << 7)
#define MS_NOREGULAR (1 << 8)
#define MS_SRCHIDDEN (1 << 9)
#define MS_IMMUTABLE (1 << 10)

enum {
    _SC_MONOTONIC_CLOCK,
    _SC_NPROCESSORS_CONF,
    _SC_NPROCESSORS_ONLN,
    _SC_OPEN_MAX,
    _SC_HOST_NAME_MAX,
    _SC_TTY_NAME_MAX,
    _SC_PAGESIZE,
    _SC_GETPW_R_SIZE_MAX,
    _SC_GETGR_R_SIZE_MAX,
    _SC_CLK_TCK,
    _SC_SYMLOOP_MAX,
    _SC_MAPPED_FILES,
    _SC_ARG_MAX,
    _SC_IOV_MAX,
    _SC_PHYS_PAGES,
};

#define _SC_MONOTONIC_CLOCK _SC_MONOTONIC_CLOCK
#define _SC_NPROCESSORS_CONF _SC_NPROCESSORS_CONF
#define _SC_NPROCESSORS_ONLN _SC_NPROCESSORS_ONLN
#define _SC_OPEN_MAX _SC_OPEN_MAX
#define _SC_PAGESIZE _SC_PAGESIZE
#define _SC_PAGE_SIZE _SC_PAGESIZE
#define _SC_HOST_NAME_MAX _SC_HOST_NAME_MAX
#define _SC_TTY_NAME_MAX _SC_TTY_NAME_MAX
#define _SC_GETPW_R_SIZE_MAX _SC_GETPW_R_SIZE_MAX
#define _SC_GETGR_R_SIZE_MAX _SC_GETGR_R_SIZE_MAX
#define _SC_CLK_TCK _SC_CLK_TCK
#define _SC_SYMLOOP_MAX _SC_SYMLOOP_MAX
#define _SC_MAPPED_FILES _SC_MAPPED_FILES
#define _SC_ARG_MAX _SC_ARG_MAX
#define _SC_IOV_MAX _SC_IOV_MAX
#define _SC_PHYS_PAGES _SC_PHYS_PAGES

#ifdef __cplusplus
}
#endif
