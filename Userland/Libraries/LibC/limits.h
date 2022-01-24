/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <bits/wchar.h>

#ifndef PAGE_SIZE
#    define PAGE_SIZE 4096
#endif

#define HOST_NAME_MAX 64

#define PATH_MAX 4096
#if !defined MAXPATHLEN && defined PATH_MAX
#    define MAXPATHLEN PATH_MAX
#endif

#define NAME_MAX 255

#define TTY_NAME_MAX 32

#define PIPE_BUF 4096

#define INT_MAX INT32_MAX
#define INT_MIN INT32_MIN

#define UINT_MAX UINT32_MAX

#define CHAR_BIT 8
#define SCHAR_MIN (-128)
#define SCHAR_MAX 127
#define UCHAR_MAX 255

#define SHRT_MAX 32768
#define SHRT_MIN (-SHRT_MAX - 1)

#define USHRT_MAX 65535

#define LONG_MAX 2147483647L
#define LONG_MIN (-LONG_MAX - 1L)

#define ULONG_MAX 4294967295UL

#define LONG_LONG_MAX 9223372036854775807LL
#define LONG_LONG_MIN (-LONG_LONG_MAX - 1LL)

#define LLONG_MAX LONG_LONG_MAX
#define LLONG_MIN LONG_LONG_MIN

#define ULONG_LONG_MAX 18446744073709551615ULL
#define ULLONG_MAX ULONG_LONG_MAX

#define CHAR_MIN SCHAR_MIN
#define CHAR_MAX SCHAR_MAX

#define CHAR_WIDTH 8
#define SCHAR_WIDTH 8
#define UCHAR_WIDTH 8

#define SHRT_WIDTH 16
#define USHRT_WIDTH 16

#define INT_WIDTH 32
#define UINT_WIDTH 32

#define LONG_WIDTH 32
#define ULONG_WIDTH 32

#define LLONG_WIDTH 64
#define ULLONG_WIDTH 64

#define ARG_MAX 65536

#define PTHREAD_STACK_MIN 65536

#define SSIZE_MAX 2147483647

#define LINK_MAX 4096

#define TZNAME_MAX 64

#ifdef __USE_POSIX
#    include <bits/posix1_lim.h>
#endif
