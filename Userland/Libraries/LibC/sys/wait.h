/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <signal.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define WEXITSTATUS(status) (((status)&0xff00) >> 8)
#define WSTOPSIG(status) WEXITSTATUS(status)
#define WTERMSIG(status) ((status)&0x7f)
#define WIFEXITED(status) (WTERMSIG(status) == 0)
#define WIFSTOPPED(status) (((status)&0xff) == 0x7f)
#define WIFSIGNALED(status) (((char)(((status)&0x7f) + 1) >> 1) > 0)

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

pid_t waitpid(pid_t, int* wstatus, int options);
pid_t wait(int* wstatus);
int waitid(idtype_t idtype, id_t id, siginfo_t* infop, int options);

__END_DECLS
