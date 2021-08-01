/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct [[gnu::packed]] PtraceRegisters {
#if ARCH(I386)
    u32 eax;
    u32 ecx;
    u32 edx;
    u32 ebx;
    u32 esp;
    u32 ebp;
    u32 esi;
    u32 edi;
    u32 eip;
    u32 eflags;
#else
    u64 rax;
    u64 rcx;
    u64 rdx;
    u64 rbx;
    u64 rsp;
    u64 rbp;
    u64 rsi;
    u64 rdi;
    u64 rip;
    u64 r8;
    u64 r9;
    u64 r10;
    u64 r11;
    u64 r12;
    u64 r13;
    u64 r14;
    u64 r15;
    u64 rflags;
#endif
    u32 cs;
    u32 ss;
    u32 ds;
    u32 es;
    u32 fs;
    u32 gs;

#ifdef __cplusplus
    FlatPtr ip() const
    {
#    if ARCH(I386)
        return eip;
#    else
        return rip;
#    endif
    }

    void set_ip(FlatPtr ip)
    {
#    if ARCH(I386)
        eip = ip;
#    else
        rip = ip;
#    endif
    }

    FlatPtr bp() const
    {
#    if ARCH(I386)
        return ebp;
#    else
        return rbp;
#    endif
    }

    void set_bp(FlatPtr bp)
    {
#    if ARCH(I386)
        ebp = bp;
#    else
        rbp = bp;
#    endif
    }
#endif
};
