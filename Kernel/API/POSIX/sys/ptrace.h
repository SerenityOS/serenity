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

#define PT_TRACE_ME 1
#define PT_ATTACH 2
#define PT_CONTINUE 3
#define PT_SYSCALL 4
#define PT_GETREGS 5
#define PT_DETACH 6
#define PT_PEEK 7
#define PT_POKE 8
#define PT_SETREGS 9

// Serenity extensions:
#define PT_POKEDEBUG 10
#define PT_PEEKDEBUG 11
#define PT_PEEKBUF 12

#define PT_READ_I PT_PEEK
#define PT_READ_D PT_PEEK
#define PT_WRITE_I PT_POKE
#define PT_WRITE_D PT_POKE

#define DEBUG_STATUS_REGISTER 6
#define DEBUG_CONTROL_REGISTER 7

#ifdef __cplusplus
}
#endif
