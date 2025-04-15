/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

struct __attribute__((packed)) __mcontext {
    __u64 rax;
    __u64 rcx;
    __u64 rdx;
    __u64 rbx;
    __u64 rsp;
    __u64 rbp;
    __u64 rsi;
    __u64 rdi;
    __u64 rip;
    __u64 r8;
    __u64 r9;
    __u64 r10;
    __u64 r11;
    __u64 r12;
    __u64 r13;
    __u64 r14;
    __u64 r15;
    __u64 rflags;
    __u32 cs;
    __u32 ss;
    __u32 ds;
    __u32 es;
    __u32 fs;
    __u32 gs;
};

#ifdef __cplusplus
}
#endif
