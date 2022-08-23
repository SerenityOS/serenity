/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>
#include <Kernel/Arch/Spinlock.h>
#include <Kernel/Locking/LockRank.h>

namespace Kernel {

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
