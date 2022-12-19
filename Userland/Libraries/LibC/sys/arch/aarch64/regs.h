/*
 * Copyright (c) 2022, Konrad <konradek@github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define RREGISTER(num)        \
    union {                   \
        u64 x##num;           \
        struct {              \
            u32 _unused##num; \
            u32 w##num;       \
        };                    \
    }

struct [[gnu::packed]] PtraceRegisters {
    // arguments & return values
    RREGISTER(0);
    RREGISTER(1);
    RREGISTER(2);
    RREGISTER(3);
    RREGISTER(4);
    RREGISTER(5);
    RREGISTER(6);
    RREGISTER(7);
    // syscall number
    RREGISTER(8);
    // temporary
    RREGISTER(9);
    RREGISTER(10);
    RREGISTER(11);
    RREGISTER(12);
    RREGISTER(13);
    RREGISTER(14);
    RREGISTER(15);
    // ipc & platform
    RREGISTER(16);
    RREGISTER(17);
    RREGISTER(18);
    // caller safe
    RREGISTER(19);
    RREGISTER(20);
    RREGISTER(21);
    RREGISTER(22);
    RREGISTER(23);
    RREGISTER(24);
    RREGISTER(25);
    RREGISTER(26);
    RREGISTER(27);
    RREGISTER(28);
    // frame register
    RREGISTER(29);
    // link register, lr
    RREGISTER(30);
    // one of two registers depending on the instruction context:
    // - stack pointer for stack-related instructions, rsp, sp
    // - zero register for read and ignore for write, rzr (xzr, wzr)
    RREGISTER(31);

    u64 sp; // non-general-purpose
    u64 pc; // non-general-purpose

    // flags
    u64 nzcv;

    // FIXME: Add FPU registers and Flags

    u64 ip() const { return pc; }
    void set_ip(u64 value) { pc = value; } // FIXME?
    u64 bp() const { return x29; }
    void set_bp(u64 value) { x29 = value; }
};
