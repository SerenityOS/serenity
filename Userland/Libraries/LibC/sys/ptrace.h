/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/ptrace.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

// FIXME: PID/TID ISSUE
// Affects the entirety of LibDebug and Userland/strace.cpp.
// See also Kernel/Ptrace.cpp
long ptrace(int request, pid_t tid, void* addr, void* data);

__END_DECLS
