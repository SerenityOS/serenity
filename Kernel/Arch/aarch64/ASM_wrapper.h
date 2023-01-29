/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/aarch64/Processor.h>
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

inline FlatPtr get_ttbr0_el1()
{
    FlatPtr ttbr0_el1;
    asm("mrs %[value], ttbr0_el1\n"
        : [value] "=r"(ttbr0_el1));
    return ttbr0_el1;
}

inline void set_sp_el1(FlatPtr sp_el1)
{
    asm("msr sp_el1, %[value]" ::[value] "r"(sp_el1));
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

inline void wait_cycles(int n)
{
    // FIXME: Make timer-based.
    for (int i = 0; i < n; i = i + 1) {
        Processor::pause();
    }
}

inline void load_el1_vector_table(void* vector_table)
{
    asm("msr VBAR_EL1, %[value]" ::[value] "r"(vector_table));
}

inline void enter_el2_from_el3()
{
    // NOTE: This also copies the current stack pointer into SP_EL2, as
    //       the processor is set up to use SP_EL2 when jumping into EL2.
    asm volatile("    mov x0, sp\n"
                 "    msr sp_el2, x0\n"
                 "    adr x0, entered_el2\n"
                 "    msr elr_el3, x0\n"
                 "    eret\n"
                 "entered_el2:" ::
                     : "x0");
}

inline void enter_el1_from_el2()
{
    // NOTE: This also copies the current stack pointer into SP_EL1, as
    //       the processor is set up to use SP_EL1 when jumping into EL1.
    asm volatile("    mov x0, sp\n"
                 "    msr sp_el1, x0\n"
                 "    adr x0, entered_el1\n"
                 "    msr elr_el2, x0\n"
                 "    eret\n"
                 "entered_el1:" ::
                     : "x0");
}

inline u64 read_rndrrs()
{
    u64 value = 0;

    asm volatile(
        "retry:\n"
        "mrs %[value], s3_3_c2_c4_1 \n" // encoded RNDRRS register
        "b.eq retry\n"
        : [value] "=r"(value));

    return value;
}

}

namespace Kernel {

inline bool are_interrupts_enabled()
{
    return Processor::are_interrupts_enabled();
}

}
