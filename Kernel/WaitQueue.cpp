/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Thread.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

bool WaitQueue::should_add_blocker(Thread::Blocker& b, void* data)
{
    VERIFY(data != nullptr); // Thread that is requesting to be blocked
    VERIFY(m_lock.is_locked());
    VERIFY(b.blocker_type() == Thread::Blocker::Type::Queue);
    if (m_wake_requested || !m_should_block) {
        m_wake_requested = false;
        dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: do not block thread {}, {}", this, data, m_should_block ? "wake was pending" : "not blocking");
        return false;
    }
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: should block thread {}", this, data);
    return true;
}

u32 WaitQueue::wake_one()
{
    u32 did_wake = 0;
    SpinlockLocker lock(m_lock);
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_one", this);
    bool did_unblock_one = unblock_all_blockers_whose_conditions_are_met_locked([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::WaitQueueBlocker&>(b);
        dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_one unblocking {}", this, data);
        if (blocker.unblock()) {
            stop_iterating = true;
            did_wake = 1;
            return true;
        }
        return false;
    });
    m_wake_requested = !did_unblock_one;
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_one woke {} threads", this, did_wake);
    return did_wake;
}

u32 WaitQueue::wake_n(u32 wake_count)
{
    if (wake_count == 0)
        return 0; // should we assert instead?
    SpinlockLocker lock(m_lock);
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_n({})", this, wake_count);
    u32 did_wake = 0;

    bool did_unblock_some = unblock_all_blockers_whose_conditions_are_met_locked([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::WaitQueueBlocker&>(b);
        dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_n unblocking {}", this, data);
        VERIFY(did_wake < wake_count);
        if (blocker.unblock()) {
            if (++did_wake >= wake_count)
                stop_iterating = true;
            return true;
        }
        return false;
    });
    m_wake_requested = !did_unblock_some;
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_n({}) woke {} threads", this, wake_count, did_wake);
    return did_wake;
}

u32 WaitQueue::wake_all()
{
    SpinlockLocker lock(m_lock);

    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_all", this);
    u32 did_wake = 0;

    bool did_unblock_any = unblock_all_blockers_whose_conditions_are_met_locked([&](Thread::Blocker& b, void* data, bool&) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::WaitQueueBlocker&>(b);

        dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_all unblocking {}", this, data);

        if (blocker.unblock()) {
            did_wake++;
            return true;
        }
        return false;
    });
    m_wake_requested = !did_unblock_any;
    dbgln_if(WAITQUEUE_DEBUG, "WaitQueue @ {}: wake_all woke {} threads", this, did_wake);
    return did_wake;
}

}
