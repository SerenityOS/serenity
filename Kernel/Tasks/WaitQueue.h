/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Optional.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

class WaitQueue {
    friend class Waiter;
    class Waiter {
    public:
        template<LockRank Rank, CallableAs<bool> F>
        ErrorOr<void> wait_until(WaitQueue& wait_queue, Spinlock<Rank>& lock, F should_wake)
        {
            bool was_interrupted = false;
            auto interrupt_state = lock.lock();
            while (true) {
                prepare(wait_queue);
                if (should_wake())
                    break;
                if (was_interrupted = Thread::current()->was_interrupted(); was_interrupted)
                    break;
                lock.unlock(interrupt_state);
                maybe_block();
                interrupt_state = lock.lock();
            }
            clear();
            lock.unlock(interrupt_state);
            // This is always cleared since the thread might have been both interrupted and woken up.
            // If both of those occurred during the same cycle, then we won't report the interrupt,
            // but we still need to clear it.
            Thread::current()->clear_interrupted();
            if (was_interrupted) {
                return EINTR;
            }

            return {};
        }

        template<typename T, LockRank Rank, CallableAs<bool, T&> F>
        ErrorOr<void> wait_until(WaitQueue& wait_queue, SpinlockProtected<T, Rank>& spinlock_protected, F should_wake)
        {
            bool was_interrupted = false;
            auto interrupt_state = spinlock_protected.m_spinlock.lock();
            while (true) {
                prepare(wait_queue);
                if (should_wake(spinlock_protected.m_value))
                    break;
                if (was_interrupted = Thread::current()->was_interrupted(); was_interrupted)
                    break;
                spinlock_protected.m_spinlock.unlock(interrupt_state);
                maybe_block();
                interrupt_state = spinlock_protected.m_spinlock.lock();
            }
            clear();
            spinlock_protected.m_spinlock.unlock(interrupt_state);
            // See the note in the above function.
            Thread::current()->clear_interrupted();
            if (was_interrupted) {
                return EINTR;
            }

            return {};
        }

        void notify(Badge<WaitQueue>);

    private:
        struct Association {
            WaitQueue& wait_queue;
            NonnullRefPtr<Thread> thread;
        };

        void prepare(WaitQueue&);
        void maybe_remove_self_from_queue();
        void maybe_block();
        void clear();

        Spinlock<LockRank::Thread> m_lock {};
        Optional<Association> m_association;
        IntrusiveListNode<Waiter> m_wait_queue_list_node;

    public:
        using ListInWaitQueue = IntrusiveList<&Waiter::m_wait_queue_list_node>;
    };

public:
    void notify_all();
    void notify_one();

    template<LockRank Rank, CallableAs<bool> F>
    ErrorOr<void> wait_until(Spinlock<Rank>& lock, F should_wake)
    {
        Waiter waiter;
        return waiter.wait_until(*this, lock, move(should_wake));
    }

    template<typename T, LockRank Rank, CallableAs<bool, T&> F>
    ErrorOr<void> wait_until(SpinlockProtected<T, Rank>& spinlock_protected, F should_wake)
    {
        Waiter waiter;
        return waiter.wait_until(*this, spinlock_protected, move(should_wake));
    }

private:
    Spinlock<LockRank::Thread> m_lock {};
    Waiter::ListInWaitQueue m_waiters;
};

}
