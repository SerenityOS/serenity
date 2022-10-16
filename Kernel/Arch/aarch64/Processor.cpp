/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Vector.h>

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
    disable_interrupts();
    for (;;)
        asm volatile("wfi");
}

void Processor::flush_tlb_local(VirtualAddress, size_t)
{
    // FIXME: Figure out how to flush a single page
    asm volatile("dsb ishst");
    asm volatile("tlbi vmalle1is");
    asm volatile("dsb ish");
    asm volatile("isb");
}

void Processor::flush_tlb(Memory::PageDirectory const*, VirtualAddress vaddr, size_t page_count)
{
    flush_tlb_local(vaddr, page_count);
}

u32 Processor::clear_critical()
{
    TODO_AARCH64();
}

u32 Processor::smp_wake_n_idle_processors(u32 wake_count)
{
    (void)wake_count;
    TODO_AARCH64();
}

ErrorOr<Vector<FlatPtr, 32>> Processor::capture_stack_trace(Thread& thread, size_t max_frames)
{
    (void)thread;
    (void)max_frames;
    TODO_AARCH64();
    return Vector<FlatPtr, 32> {};
}

}
