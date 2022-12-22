/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Vector.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/InterruptDisabler.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/Time/TimeManagement.h>

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
    InterruptDisabler disabler;
    auto prev_critical = in_critical();
    auto& proc = current();
    proc.m_in_critical = 0;
    if (proc.m_in_irq == 0)
        proc.check_invoke_scheduler();
    return prev_critical;
}

u32 Processor::smp_wake_n_idle_processors(u32 wake_count)
{
    (void)wake_count;
    TODO_AARCH64();
}

void Processor::initialize_context_switching(Thread& initial_thread)
{
    (void)initial_thread;
    TODO_AARCH64();
}

void Processor::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    (void)from_thread;
    (void)to_thread;
    TODO_AARCH64();
}

void Processor::assume_context(Thread& thread, FlatPtr flags)
{
    (void)thread;
    (void)flags;
    TODO_AARCH64();
}

FlatPtr Processor::init_context(Thread& thread, bool leave_crit)
{
    (void)thread;
    (void)leave_crit;
    TODO_AARCH64();
}

void Processor::enter_trap(TrapFrame& trap, bool raise_irq)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);
    // FIXME: Figure out if we need prev_irq_level, see duplicated code in Kernel/Arch/x86/common/Processor.cpp
    if (raise_irq)
        m_in_irq++;
    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        trap.next_trap = current_trap;
        current_trap = &trap;
        // FIXME: Determine PreviousMode from TrapFrame when userspace programs can run on aarch64
        auto new_previous_mode = Thread::PreviousMode::KernelMode;
        if (current_thread->set_previous_mode(new_previous_mode)) {
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), new_previous_mode == Thread::PreviousMode::KernelMode, false);
        }
    } else {
        trap.next_trap = nullptr;
    }
}

void Processor::exit_trap(TrapFrame& trap)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);

    // Temporarily enter a critical section. This is to prevent critical
    // sections entered and left within e.g. smp_process_pending_messages
    // to trigger a context switch while we're executing this function
    // See the comment at the end of the function why we don't use
    // ScopedCritical here.
    m_in_critical = m_in_critical + 1;

    // FIXME: Figure out if we need prev_irq_level, see duplicated code in Kernel/Arch/x86/common/Processor.cpp
    m_in_irq = 0;

    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        current_trap = trap.next_trap;
        Thread::PreviousMode new_previous_mode;
        if (current_trap) {
            VERIFY(current_trap->regs);
            // FIXME: Determine PreviousMode from TrapFrame when userspace programs can run on aarch64
            new_previous_mode = Thread::PreviousMode::KernelMode;
        } else {
            // If we don't have a higher level trap then we're back in user mode.
            // Which means that the previous mode prior to being back in user mode was kernel mode
            new_previous_mode = Thread::PreviousMode::KernelMode;
        }

        if (current_thread->set_previous_mode(new_previous_mode))
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), true, false);
    }

    VERIFY_INTERRUPTS_DISABLED();

    // Leave the critical section without actually enabling interrupts.
    // We don't want context switches to happen until we're explicitly
    // triggering a switch in check_invoke_scheduler.
    m_in_critical = m_in_critical - 1;
    if (!m_in_irq && !m_in_critical)
        check_invoke_scheduler();
}

ErrorOr<Vector<FlatPtr, 32>> Processor::capture_stack_trace(Thread& thread, size_t max_frames)
{
    (void)thread;
    (void)max_frames;
    TODO_AARCH64();
    return Vector<FlatPtr, 32> {};
}

void Processor::check_invoke_scheduler()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(!m_in_irq);
    VERIFY(!m_in_critical);
    VERIFY(&Processor::current() == this);
    if (m_invoke_scheduler_async && m_scheduler_initialized) {
        m_invoke_scheduler_async = false;
        Scheduler::invoke_async();
    }
}

}
