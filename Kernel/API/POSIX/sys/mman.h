/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>
#include <Kernel/API/POSIX/time.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MAP_FILE 0x00
#define MAP_SHARED 0x01
#define MAP_PRIVATE 0x02
#define MAP_FIXED 0x10
#define MAP_ANONYMOUS 0x20
#define MAP_ANON MAP_ANONYMOUS
#define MAP_STACK 0x40
#define MAP_NORESERVE 0x80
#define MAP_RANDOMIZED 0x100
#define MAP_PURGEABLE 0x200

#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_NONE 0x0

#define MAP_FAILED ((void*)-1)

#define MADV_SET_VOLATILE 0x100
#define MADV_SET_NONVOLATILE 0x200

#ifdef __cplusplus
}
#endif
