/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/sys_wait.h.html
#include <signal.h>

#include <Kernel/API/POSIX/signal.h>
#include <Kernel/API/POSIX/sys/wait.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

pid_t waitpid(pid_t, int* wstatus, int options);
pid_t wait(int* wstatus);
int waitid(idtype_t idtype, id_t id, siginfo_t* infop, int options);

__END_DECLS
