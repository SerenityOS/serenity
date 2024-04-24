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

#define WEXITSTATUS(status) (((status) & 0xff00) >> 8)
#define WSTOPSIG(status) WEXITSTATUS(status)
#define WTERMSIG(status) ((status) & 0x7f)
#define WIFEXITED(status) (WTERMSIG(status) == 0)
#define WIFSTOPPED(status) (((status) & 0xff) == 0x7f)
#define WIFSIGNALED(status) (((char)(((status) & 0x7f) + 1) >> 1) > 0)
#define WIFCONTINUED(status) ((status) == 0xffff)

#define WNOHANG 1
#define WUNTRACED 2
#define WSTOPPED WUNTRACED
#define WEXITED 4
#define WCONTINUED 8
#define WNOWAIT 0x1000000

typedef enum {
    P_ALL = 1,
    P_PID,
    P_PGID
} idtype_t;

#ifdef __cplusplus
}
#endif
