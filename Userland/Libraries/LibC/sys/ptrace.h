/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/types.h>

__BEGIN_DECLS

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

#define DEBUG_STATUS_REGISTER 6
#define DEBUG_CONTROL_REGISTER 7

// FIXME: PID/TID ISSUE
// Affects the entirety of LibDebug and Userland/strace.cpp.
// See also Kernel/Ptrace.cpp
int ptrace(int request, pid_t tid, void* addr, int data);

__END_DECLS
