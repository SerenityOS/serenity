/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

READONLY_AFTER_INIT FPUState s_clean_fpu_state;
READONLY_AFTER_INIT Atomic<u32> g_total_processors;

template<typename T>
void ProcessorBase<T>::check_invoke_scheduler()
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
template void ProcessorBase<Processor>::check_invoke_scheduler();

template<typename T>
void ProcessorBase<T>::deferred_call_queue(Function<void()> callback)
{
    // NOTE: If we are called outside of a critical section and outside
    // of an irq handler, the function will be executed before we return!
    ScopedCritical critical;
    auto& cur_proc = Processor::current();

    auto* entry = cur_proc.m_deferred_call_pool.get_free();
    entry->handler_value() = move(callback);

    cur_proc.m_deferred_call_pool.queue_entry(entry);
}
template void ProcessorBase<Processor>::deferred_call_queue(Function<void()>);

template<typename T>
void ProcessorBase<T>::enter_trap(TrapFrame& trap, bool raise_irq)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);
#if ARCH(X86_64)
    // FIXME: Figure out if we need prev_irq_level
    trap.prev_irq_level = m_in_irq;
#endif
    if (raise_irq)
        m_in_irq++;
    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        trap.next_trap = current_trap;
        current_trap = &trap;
        auto new_previous_mode = trap.regs->previous_mode();
        if (current_thread->set_previous_mode(new_previous_mode)) {
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), new_previous_mode == ExecutionMode::Kernel, false);
        }
    } else {
        trap.next_trap = nullptr;
    }
}
template void ProcessorBase<Processor>::enter_trap(TrapFrame&, bool);

template<typename T>
u64 ProcessorBase<T>::time_spent_idle() const
{
    return m_idle_thread->time_in_user() + m_idle_thread->time_in_kernel();
}
template u64 ProcessorBase<Processor>::time_spent_idle() const;

template<typename T>
void ProcessorBase<T>::leave_critical()
{
    InterruptDisabler disabler;
    current().do_leave_critical();
}
template void ProcessorBase<Processor>::leave_critical();

template<typename T>
void ProcessorBase<T>::do_leave_critical()
{
    VERIFY(m_in_critical > 0);
    if (m_in_critical == 1) {
        if (m_in_irq == 0) {
            m_deferred_call_pool.execute_pending();
            VERIFY(m_in_critical == 1);
        }
        m_in_critical = 0;
        if (m_in_irq == 0)
            check_invoke_scheduler();
    } else {
        m_in_critical = m_in_critical - 1;
    }
}
template void ProcessorBase<Processor>::do_leave_critical();

void exit_kernel_thread(void)
{
    Thread::current()->exit();
}

void do_context_first_init(Thread* from_thread, Thread* to_thread)
{
    VERIFY(!Processor::are_interrupts_enabled());
    VERIFY(Processor::is_kernel_mode());

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {} (context_first_init)", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);

    VERIFY(to_thread == Thread::current());

    Scheduler::enter_current(*from_thread);

    auto in_critical = to_thread->saved_critical();
    VERIFY(in_critical > 0);
    Processor::restore_critical(in_critical);

    // Since we got here and don't have Scheduler::context_switch in the
    // call stack (because this is the first time we switched into this
    // context), we need to notify the scheduler so that it can release
    // the scheduler lock. We don't want to enable interrupts at this point
    // as we're still in the middle of a context switch. Doing so could
    // trigger a context switch within a context switch, leading to a crash.
    Scheduler::leave_on_first_switch(InterruptsState::Disabled);
}

}
