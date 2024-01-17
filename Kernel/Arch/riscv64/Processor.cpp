/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

Processor* g_current_processor;

template<typename T>
void ProcessorBase<T>::early_initialize(u32 cpu)
{
    VERIFY(g_current_processor == nullptr);
    m_cpu = cpu;

    g_current_processor = static_cast<Processor*>(this);
}

template<typename T>
void ProcessorBase<T>::initialize(u32)
{
    m_deferred_call_pool.init();

    // Enable the FPU
    auto sstatus = RISCV64::CSR::SSTATUS::read();
    sstatus.FS = RISCV64::CSR::SSTATUS::FloatingPointStatus::Initial;
    RISCV64::CSR::SSTATUS::write(sstatus);

    initialize_interrupts();
}

template<typename T>
[[noreturn]] void ProcessorBase<T>::halt()
{
    // WFI ignores the value of sstatus.SIE, so we can't use disable_interrupts().
    // Instead, disable all interrupts sources by setting sie to zero.
    RISCV64::CSR::write(RISCV64::CSR::Address::SIE, 0);
    for (;;)
        asm volatile("wfi");
}

template<typename T>
void ProcessorBase<T>::flush_tlb_local(VirtualAddress, size_t)
{
    // FIXME: Don't flush all pages
    flush_entire_tlb_local();
}

template<typename T>
void ProcessorBase<T>::flush_entire_tlb_local()
{
    asm volatile("sfence.vma");
}

template<typename T>
void ProcessorBase<T>::flush_tlb(Memory::PageDirectory const*, VirtualAddress vaddr, size_t page_count)
{
    flush_tlb_local(vaddr, page_count);
}

template<typename T>
u32 ProcessorBase<T>::clear_critical()
{
    InterruptDisabler disabler;
    auto prev_critical = in_critical();
    auto& proc = current();
    proc.m_in_critical = 0;
    if (proc.m_in_irq == 0)
        proc.check_invoke_scheduler();
    return prev_critical;
}

template<typename T>
u32 ProcessorBase<T>::smp_wake_n_idle_processors(u32)
{
    // FIXME: Actually wake up other cores when SMP is supported for riscv64.
    return 0;
}

template<typename T>
void ProcessorBase<T>::initialize_context_switching(Thread&)
{
    TODO_RISCV64();
}

template<typename T>
void ProcessorBase<T>::switch_context(Thread*&, Thread*&)
{
    TODO_RISCV64();
}

extern "C" FlatPtr do_init_context(Thread*, u32)
{
    TODO_RISCV64();
}

template<typename T>
void ProcessorBase<T>::assume_context(Thread&, InterruptsState)
{
    TODO_RISCV64();
}

template<typename T>
FlatPtr ProcessorBase<T>::init_context(Thread&, bool)
{
    TODO_RISCV64();
}

template<typename T>
void ProcessorBase<T>::exit_trap(TrapFrame& trap)
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

    // Process the deferred call queue. Among other things, this ensures
    // that any pending thread unblocks happen before we enter the scheduler.
    m_deferred_call_pool.execute_pending();

    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        current_trap = trap.next_trap;
        ExecutionMode new_previous_mode;
        if (current_trap) {
            VERIFY(current_trap->regs);
            new_previous_mode = current_trap->regs->previous_mode();
        } else {
            // If we don't have a higher level trap then we're back in user mode.
            // Which means that the previous mode prior to being back in user mode was kernel mode
            new_previous_mode = ExecutionMode::Kernel;
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

template<typename T>
ErrorOr<Vector<FlatPtr, 32>> ProcessorBase<T>::capture_stack_trace(Thread&, size_t)
{
    dbgln("FIXME: Implement Processor::capture_stack_trace() for riscv64");
    return Vector<FlatPtr, 32> {};
}

NAKED void thread_context_first_enter(void)
{
    asm("unimp");
}

NAKED void do_assume_context(Thread*, u32)
{
    asm("unimp");
}

template<typename T>
StringView ProcessorBase<T>::platform_string()
{
    return "riscv64"sv;
}

template<typename T>
void ProcessorBase<T>::set_thread_specific_data(VirtualAddress)
{
    TODO_RISCV64();
}

template<typename T>
void ProcessorBase<T>::wait_for_interrupt() const
{
    asm("wfi");
}

template<typename T>
Processor& ProcessorBase<T>::by_id(u32)
{
    TODO_RISCV64();
}

}

#include <Kernel/Arch/ProcessorFunctions.include>
