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
#define MAP_FIXED_NOREPLACE 0x400

#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_NONE 0x0

#define MAP_FAILED ((void*)-1)

#define MADV_NORMAL 0x0
#define MADV_SET_VOLATILE 0x1
#define MADV_SET_NONVOLATILE 0x2
#define MADV_DONTNEED 0x3
#define MADV_WILLNEED 0x4
#define MADV_SEQUENTIAL 0x5
#define MADV_RANDOM 0x6

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_madvise.html
#define POSIX_MADV_NORMAL MADV_NORMAL
#define POSIX_MADV_DONTNEED MADV_DONTNEED
#define POSIX_MADV_WILLNEED MADV_WILLNEED
#define POSIX_MADV_SEQUENTIAL MADV_SEQUENTIAL
#define POSIX_MADV_RANDOM MADV_RANDOM

#define MS_SYNC 1
#define MS_ASYNC 2
#define MS_INVALIDATE 4

#ifdef __cplusplus
}
#endif
