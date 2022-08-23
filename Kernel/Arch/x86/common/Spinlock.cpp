/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Spinlock.h>

namespace Kernel {

InterruptsState Spinlock::lock()
{
    InterruptsState previous_interrupts_state = processor_interrupts_state();
    Processor::enter_critical();
    Processor::disable_interrupts();
    while (m_lock.exchange(1, AK::memory_order_acquire) != 0)
        Processor::wait_check();
    track_lock_acquire(m_rank);
    return previous_interrupts_state;
}

void Spinlock::unlock(InterruptsState previous_interrupts_state)
{
    VERIFY(is_locked());
    track_lock_release(m_rank);
    m_lock.store(0, AK::memory_order_release);

    Processor::leave_critical();
    restore_processor_interrupts_state(previous_interrupts_state);
}

InterruptsState RecursiveSpinlock::lock()
{
    InterruptsState previous_interrupts_state = processor_interrupts_state();
    Processor::disable_interrupts();
    Processor::enter_critical();
    auto& proc = Processor::current();
    FlatPtr cpu = FlatPtr(&proc);
    FlatPtr expected = 0;
    while (!m_lock.compare_exchange_strong(expected, cpu, AK::memory_order_acq_rel)) {
        if (expected == cpu)
            break;
        Processor::wait_check();
        expected = 0;
    }
    if (m_recursions == 0)
        track_lock_acquire(m_rank);
    m_recursions++;
    return previous_interrupts_state;
}

void RecursiveSpinlock::unlock(InterruptsState previous_interrupts_state)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(m_recursions > 0);
    VERIFY(m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current()));
    if (--m_recursions == 0) {
        track_lock_release(m_rank);
        m_lock.store(0, AK::memory_order_release);
    }

    Processor::leave_critical();
    restore_processor_interrupts_state(previous_interrupts_state);
}

}
