/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/Registers.h>

namespace Kernel::Aarch64::Asm {

inline void set_ttbr1_el1(FlatPtr ttbr1_el1)
{
    asm("msr ttbr1_el1, %[value]" ::[value] "r"(ttbr1_el1));
}

inline void set_ttbr0_el1(FlatPtr ttbr0_el1)
{
    asm("msr ttbr0_el1, %[value]" ::[value] "r"(ttbr0_el1));
}

inline void flush()
{
    asm("dsb ish");
    asm("isb");
}

[[noreturn]] inline void halt()
{
    for (;;) {
        asm volatile("wfi");
    }
}

enum class ExceptionLevel : u8 {
    EL0 = 0,
    EL1 = 1,
    EL2 = 2,
    EL3 = 3,
};

inline ExceptionLevel get_current_exception_level()
{
    u64 current_exception_level;

    asm("mrs  %[value], CurrentEL"
        : [value] "=r"(current_exception_level));

    current_exception_level = (current_exception_level >> 2) & 0x3;
    return static_cast<ExceptionLevel>(current_exception_level);
}

}
