/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FutexQueue.h>
#include <Kernel/Thread.h>

namespace Kernel {

bool FutexQueue::should_add_blocker(Thread::Blocker& b, void* data)
{
    VERIFY(data != nullptr); // Thread that is requesting to be blocked
    VERIFY(m_lock.is_locked());
    VERIFY(b.blocker_type() == Thread::Blocker::Type::Futex);

    dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: should block thread {}", this, *static_cast<Thread*>(data));

    return true;
}

u32 FutexQueue::wake_n_requeue(u32 wake_count, const Function<FutexQueue*()>& get_target_queue, u32 requeue_count, bool& is_empty, bool& is_empty_target)
{
    is_empty_target = false;
    ScopedSpinLock lock(m_lock);

    dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n_requeue({}, {})", this, wake_count, requeue_count);

    u32 did_wake = 0, did_requeue = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);

        dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n_requeue unblocking {}", this, *static_cast<Thread*>(data));
        VERIFY(did_wake < wake_count);
        if (blocker.unblock()) {
            if (++did_wake >= wake_count)
                stop_iterating = true;
            return true;
        }
        return false;
    });
    is_empty = is_empty_locked();
    if (requeue_count > 0) {
        auto blockers_to_requeue = do_take_blockers(requeue_count);
        if (!blockers_to_requeue.is_empty()) {
            if (auto* target_futex_queue = get_target_queue()) {
                dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n_requeue requeueing {} blockers to {}", this, blockers_to_requeue.size(), target_futex_queue);

                // While still holding m_lock, notify each blocker
                for (auto& info : blockers_to_requeue) {
                    VERIFY(info.blocker->blocker_type() == Thread::Blocker::Type::Futex);
                    auto& blocker = *static_cast<Thread::FutexBlocker*>(info.blocker);
                    blocker.begin_requeue();
                }

                lock.unlock();
                did_requeue = blockers_to_requeue.size();

                ScopedSpinLock target_lock(target_futex_queue->m_lock);
                // Now that we have the lock of the target, append the blockers
                // and notify them that they completed the move
                for (auto& info : blockers_to_requeue) {
                    VERIFY(info.blocker->blocker_type() == Thread::Blocker::Type::Futex);
                    auto& blocker = *static_cast<Thread::FutexBlocker*>(info.blocker);
                    blocker.finish_requeue(*target_futex_queue);
                }
                target_futex_queue->do_append_blockers(move(blockers_to_requeue));
                is_empty_target = target_futex_queue->is_empty_locked();
            } else {
                dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n_requeue could not get target queue to requeue {} blockers", this, blockers_to_requeue.size());
                do_append_blockers(move(blockers_to_requeue));
            }
        }
    }
    return did_wake + did_requeue;
}

u32 FutexQueue::wake_n(u32 wake_count, const Optional<u32>& bitset, bool& is_empty)
{
    if (wake_count == 0)
        return 0; // should we assert instead?
    ScopedSpinLock lock(m_lock);
    dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n({})", this, wake_count);
    u32 did_wake = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);

        dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_n unblocking {}", this, *static_cast<Thread*>(data));
        VERIFY(did_wake < wake_count);
        if (bitset.has_value() ? blocker.unblock_bitset(bitset.value()) : blocker.unblock()) {
            if (++did_wake >= wake_count)
                stop_iterating = true;
            return true;
        }
        return false;
    });
    is_empty = is_empty_locked();
    return did_wake;
}

u32 FutexQueue::wake_all(bool& is_empty)
{
    ScopedSpinLock lock(m_lock);
    dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_all", this);
    u32 did_wake = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool&) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);
        dbgln_if(FUTEXQUEUE_DEBUG, "FutexQueue @ {}: wake_all unblocking {}", this, *static_cast<Thread*>(data));
        if (blocker.unblock(true)) {
            did_wake++;
            return true;
        }
        return false;
    });
    is_empty = is_empty_locked();
    return did_wake;
}

}
