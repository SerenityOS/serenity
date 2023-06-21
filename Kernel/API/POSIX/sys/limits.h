/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// FIXME: This file can be included from Lagom, but it should only be included on Serenity. When that is the case, we can remove the ifdef
#ifndef PAGE_SIZE
#    define PAGE_SIZE 4096
#endif

#define PATH_MAX 4096
#if !defined MAXPATHLEN && defined PATH_MAX
#    define MAXPATHLEN PATH_MAX
#endif

#define NAME_MAX 255

#define HOST_NAME_MAX 64

#define TTY_NAME_MAX 32

#define NGROUPS_MAX 32

#define ARG_MAX 65536

#define PTHREAD_STACK_MIN (64 * 1024) // 64KiB
