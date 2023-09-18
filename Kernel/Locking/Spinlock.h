/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Locking/LockRank.h>

namespace Kernel {

template<LockRank Rank>
class Spinlock {
    AK_MAKE_NONCOPYABLE(Spinlock);
    AK_MAKE_NONMOVABLE(Spinlock);

public:
    Spinlock() = default;

    InterruptsState lock()
    {
        InterruptsState previous_interrupts_state = Processor::interrupts_state();
        Processor::enter_critical();
        Processor::disable_interrupts();
        while (m_lock.exchange(1, AK::memory_order_acquire) != 0)
            Processor::wait_check();
        track_lock_acquire(m_rank);
        return previous_interrupts_state;
    }

    void unlock(InterruptsState previous_interrupts_state)
    {
        VERIFY(is_locked());
        track_lock_release(m_rank);
        m_lock.store(0, AK::memory_order_release);

        Processor::leave_critical();
        Processor::restore_interrupts_state(previous_interrupts_state);
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<u8> m_lock { 0 };
    static constexpr LockRank const m_rank { Rank };
};

template<LockRank Rank>
class RecursiveSpinlock {
    AK_MAKE_NONCOPYABLE(RecursiveSpinlock);
    AK_MAKE_NONMOVABLE(RecursiveSpinlock);

public:
    RecursiveSpinlock() = default;

    InterruptsState lock()
    {
        InterruptsState previous_interrupts_state = Processor::interrupts_state();
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

    void unlock(InterruptsState previous_interrupts_state)
    {
        VERIFY_INTERRUPTS_DISABLED();
        VERIFY(m_recursions > 0);
        VERIFY(m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current()));
        if (--m_recursions == 0) {
            track_lock_release(m_rank);
            m_lock.store(0, AK::memory_order_release);
        }

        Processor::leave_critical();
        Processor::restore_interrupts_state(previous_interrupts_state);
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked_by_current_processor() const
    {
        return m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current());
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<FlatPtr> m_lock { 0 };
    u32 m_recursions { 0 };
    static constexpr LockRank const m_rank { Rank };
};

template<typename LockType>
class [[nodiscard]] SpinlockLocker {
    AK_MAKE_NONCOPYABLE(SpinlockLocker);

public:
    SpinlockLocker() = delete;
    SpinlockLocker& operator=(SpinlockLocker&&) = delete;

    SpinlockLocker(LockType& lock)
        : m_lock(&lock)
    {
        VERIFY(m_lock);
        m_previous_interrupts_state = m_lock->lock();
        m_have_lock = true;
    }

    SpinlockLocker(SpinlockLocker&& from)
        : m_lock(from.m_lock)
        , m_previous_interrupts_state(from.m_previous_interrupts_state)
        , m_have_lock(from.m_have_lock)
    {
        from.m_lock = nullptr;
        from.m_previous_interrupts_state = InterruptsState::Disabled;
        from.m_have_lock = false;
    }

    ~SpinlockLocker()
    {
        if (m_lock && m_have_lock) {
            m_lock->unlock(m_previous_interrupts_state);
        }
    }

    ALWAYS_INLINE void lock()
    {
        VERIFY(m_lock);
        VERIFY(!m_have_lock);
        m_previous_interrupts_state = m_lock->lock();
        m_have_lock = true;
    }

    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_lock);
        VERIFY(m_have_lock);
        m_lock->unlock(m_previous_interrupts_state);
        m_previous_interrupts_state = InterruptsState::Disabled;
        m_have_lock = false;
    }

    [[nodiscard]] ALWAYS_INLINE bool have_lock() const
    {
        return m_have_lock;
    }

private:
    LockType* m_lock { nullptr };
    InterruptsState m_previous_interrupts_state { InterruptsState::Disabled };
    bool m_have_lock { false };
};

}
