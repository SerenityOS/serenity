/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>

extern "C" uintptr_t vector_table_el1;

namespace Kernel {

Processor* g_current_processor;

void Processor::initialize(u32 cpu)
{
    VERIFY(g_current_processor == nullptr);

    auto current_exception_level = static_cast<u64>(Aarch64::Asm::get_current_exception_level());
    dbgln("CPU{} started in: EL{}", cpu, current_exception_level);

    dbgln("Drop CPU{} to EL1", cpu);
    drop_to_exception_level_1();

    // Load EL1 vector table
    Aarch64::Asm::el1_vector_table_install(&vector_table_el1);

    g_current_processor = this;
}

[[noreturn]] void Processor::halt()
{
    for (;;)
        asm volatile("wfi");
}

}
