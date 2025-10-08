/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WaitQueue.h>

// This is essentially an implementation of https://read.seas.harvard.edu/cs1610/2025/doc/wait-queues

namespace Kernel {

void WaitQueue::notify_all()
{
    SpinlockLocker queue_lock { m_lock };
    while (!m_waiters.is_empty()) {
        Waiter& waiter = *m_waiters.take_first();
        waiter.notify({});
    }
}

void WaitQueue::notify_one()
{
    SpinlockLocker queue_lock { m_lock };
    if (m_waiters.is_empty())
        return;

    Waiter& waiter = *m_waiters.take_first();
    waiter.notify({});
}

void WaitQueue::Waiter::notify(Badge<WaitQueue>)
{
    VERIFY(m_association.has_value());
    auto& thread = *m_association->thread;

    SpinlockLocker scheduler_lock(g_scheduler_lock);
    VERIFY(&thread != Thread::current());
    // The thread might already be runnable if it has already
    // been notified, but has not yet been scheduled again.
    if (thread.state() == Thread::State::Runnable)
        return;

    VERIFY(thread.state() == Thread::State::Blocked);
    thread.set_state(Thread::State::Runnable);
}

void WaitQueue::Waiter::prepare(WaitQueue& wait_queue)
{
    auto* current_thread = Thread::current();
    VERIFY(current_thread);

    m_association = { wait_queue, *current_thread };
    SpinlockLocker queue_lock { wait_queue.m_lock };
    {
        SpinlockLocker scheduler_lock(g_scheduler_lock);
        current_thread->set_state(Thread::State::Blocked);
    }
    wait_queue.m_waiters.append(*this);
}

void WaitQueue::Waiter::maybe_remove_self_from_queue()
{
    VERIFY(m_association.has_value());
    auto& wait_queue = m_association->wait_queue;
    VERIFY(wait_queue.m_lock.is_locked());

    if (m_wait_queue_list_node.is_in_list())
        m_wait_queue_list_node.remove();
}

void WaitQueue::Waiter::maybe_block()
{
    VERIFY(m_association.has_value());

    bool blocked = false;
    {
        SpinlockLocker scheduler_lock(g_scheduler_lock);
        blocked = m_association->thread->state() == Thread::State::Blocked;
    }
    if (blocked)
        Scheduler::yield();

    SpinlockLocker queue_lock { m_association->wait_queue.m_lock };
    maybe_remove_self_from_queue();
}

void WaitQueue::Waiter::clear()
{
    VERIFY(m_association.has_value());

    SpinlockLocker queue_lock { m_association->wait_queue.m_lock };
    maybe_remove_self_from_queue();

    auto& thread = *m_association->thread;
    SpinlockLocker scheduler_lock(g_scheduler_lock);
    VERIFY(&thread == Thread::current());
    VERIFY(thread.state() == Thread::State::Blocked || thread.state() == Thread::State::Runnable);
    thread.set_state(Thread::State::Running);

    m_association = {};
}

}
