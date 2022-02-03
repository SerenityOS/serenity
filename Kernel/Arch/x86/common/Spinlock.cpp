/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/Spinlock.h>

namespace Kernel {

u32 Spinlock::lock()
{
    u32 prev_flags = cpu_flags();
    Processor::enter_critical();
    cli();
    while (m_lock.exchange(1, AK::memory_order_acquire) != 0)
        Processor::wait_check();
    track_lock_acquire(m_rank);
    return prev_flags;
}

void Spinlock::unlock(u32 prev_flags)
{
    VERIFY(is_locked());
    track_lock_release(m_rank);
    m_lock.store(0, AK::memory_order_release);
    if ((prev_flags & 0x200) != 0)
        sti();
    else
        cli();

    Processor::leave_critical();
}

u32 RecursiveSpinlock::lock()
{
    u32 prev_flags = cpu_flags();
    cli();
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
    return prev_flags;
}

void RecursiveSpinlock::unlock(u32 prev_flags)
{
    VERIFY(m_recursions > 0);
    VERIFY(m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current()));
    if (--m_recursions == 0) {
        track_lock_release(m_rank);
        m_lock.store(0, AK::memory_order_release);
    }
    if ((prev_flags & 0x200) != 0)
        sti();
    else
        cli();

    Processor::leave_critical();
}

}
