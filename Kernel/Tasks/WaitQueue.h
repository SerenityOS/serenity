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
        template<CallableAs<bool> F>
        ErrorOr<void> wait_until(WaitQueue& wait_queue, F should_wake)
        {
            // LockRank::None is just a dummy value.
            return wait_until_impl<LockRank::None>(wait_queue, should_wake, OptionalNone {});
        }

        template<LockRank Rank, CallableAs<bool> F>
        ErrorOr<void> wait_until(WaitQueue& wait_queue, Spinlock<Rank>& lock, F should_wake)
        {
            return wait_until_impl(wait_queue, should_wake, lock);
        }

        template<typename T, LockRank Rank, CallableAs<bool, T&> F>
        ErrorOr<void> wait_until(WaitQueue& wait_queue, SpinlockProtected<T, Rank>& spinlock_protected, F should_wake)
        {
            return wait_until_impl<Rank>(wait_queue, [&spinlock_protected, &should_wake] { return should_wake(spinlock_protected.m_value); }, spinlock_protected.m_spinlock);
        }

        void notify(Badge<WaitQueue>);

    private:
        template<LockRank Rank, CallableAs<bool> F>
        ErrorOr<void> wait_until_impl(WaitQueue& wait_queue, F should_wake, Optional<Spinlock<Rank>&> user_lock)
        {
            bool was_interrupted = false;

            SpinlockLocker scheduler_lock(g_scheduler_lock);

            auto lock = [&user_lock] -> InterruptsState {
                if (user_lock.has_value())
                    return user_lock->lock();
                return {};
            };
            auto unlock = [&user_lock](InterruptsState interrupts_state) {
                if (user_lock.has_value())
                    user_lock->unlock(interrupts_state);
            };

            auto interrupt_state = lock();

            while (true) {
                prepare(wait_queue);

                if (should_wake())
                    break;
                if (was_interrupted = Thread::current()->was_interrupted(); was_interrupted)
                    break;

                unlock(interrupt_state);
                scheduler_lock.unlock();

                maybe_block();

                scheduler_lock.lock();
                interrupt_state = lock();
            }

            clear();

            unlock(interrupt_state);
            scheduler_lock.unlock();

            // This is always cleared since the thread might have been both interrupted and woken up.
            // If both of those occurred during the same cycle, then we won't report the interrupt,
            // but we still need to clear it.
            Thread::current()->clear_interrupted();
            if (was_interrupted) {
                return EINTR;
            }

            return {};
        }

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

    template<CallableAs<bool> F>
    ErrorOr<void> wait_until(F should_wake)
    {
        Waiter waiter;
        return waiter.wait_until(*this, move(should_wake));
    }

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
