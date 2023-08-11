/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/limits.h>
#include <Kernel/API/serenity_limits.h>
#include <bits/posix1_lim.h>
#include <bits/stdint.h>
#include <bits/wchar.h>

#define PIPE_BUF 4096

#define INT_MAX INT32_MAX
#define INT_MIN INT32_MIN

#define UINT_MAX UINT32_MAX

#define CHAR_BIT 8
#define SCHAR_MIN (-128)
#define SCHAR_MAX 127
#define UCHAR_MAX 255

#define SHRT_MAX 32767
#define SHRT_MIN (-SHRT_MAX - 1)

#define USHRT_MAX 65535

#define LONG_MAX 9223372036854775807L
#define LONG_MIN (-LONG_MAX - 1L)

#define ULONG_MAX 18446744073709551615UL

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

#define LONG_WIDTH 64
#define ULONG_WIDTH 64

#define LLONG_WIDTH 64
#define ULLONG_WIDTH 64

#define SSIZE_MAX LONG_MAX

#define LINK_MAX 4096

#define TZNAME_MAX 64

#define PASS_MAX 128
