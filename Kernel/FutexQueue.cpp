/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/FutexQueue.h>
#include <Kernel/Thread.h>

//#define FUTEXQUEUE_DEBUG

namespace Kernel {

bool FutexQueue::should_add_blocker(Thread::Blocker& b, void* data)
{
    ASSERT(data != nullptr); // Thread that is requesting to be blocked
    ASSERT(m_lock.is_locked());
    ASSERT(b.blocker_type() == Thread::Blocker::Type::Futex);
#ifdef FUTEXQUEUE_DEBUG
    dbg() << "FutexQueue @ " << this << ": should block thread " << *static_cast<Thread*>(data);
#endif
    return true;
}

u32 FutexQueue::wake_n_requeue(u32 wake_count, const Function<FutexQueue*()>& get_target_queue, u32 requeue_count, bool& is_empty, bool& is_empty_target)
{
    is_empty_target = false;
    ScopedSpinLock lock(m_lock);
#ifdef FUTEXQUEUE_DEBUG
    dbg() << "FutexQueue @ " << this << ": wake_n_requeue(" << wake_count << ", " << requeue_count << ")";
#endif
    u32 did_wake = 0, did_requeue = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);
#ifdef FUTEXQUEUE_DEBUG
        dbg() << "FutexQueue @ " << this << ": wake_n_requeue unblocking " << *static_cast<Thread*>(data);
#endif
        ASSERT(did_wake < wake_count);
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
#ifdef FUTEXQUEUE_DEBUG
                dbg() << "FutexQueue @ " << this << ": wake_n_requeue requeueing " << blockers_to_requeue.size() << " blockers to " << target_futex_queue;
#endif
                // While still holding m_lock, notify each blocker
                for (auto& info : blockers_to_requeue) {
                    ASSERT(info.blocker->blocker_type() == Thread::Blocker::Type::Futex);
                    auto& blocker = *static_cast<Thread::FutexBlocker*>(info.blocker);
                    blocker.begin_requeue();
                }

                lock.unlock();
                did_requeue = blockers_to_requeue.size();

                ScopedSpinLock target_lock(target_futex_queue->m_lock);
                // Now that we have the lock of the target, append the blockers
                // and notify them that they completed the move
                for (auto& info : blockers_to_requeue) {
                    ASSERT(info.blocker->blocker_type() == Thread::Blocker::Type::Futex);
                    auto& blocker = *static_cast<Thread::FutexBlocker*>(info.blocker);
                    blocker.finish_requeue(*target_futex_queue);
                }
                target_futex_queue->do_append_blockers(move(blockers_to_requeue));
                is_empty_target = target_futex_queue->is_empty_locked();
            } else {
#ifdef FUTEXQUEUE_DEBUG
                dbg() << "FutexQueue @ " << this << ": wake_n_requeue could not get target queue to requeueing " << blockers_to_requeue.size() << " blockers";
#endif
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
#ifdef FUTEXQUEUE_DEBUG
    dbg() << "FutexQueue @ " << this << ": wake_n(" << wake_count << ")";
#endif
    u32 did_wake = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);
#ifdef FUTEXQUEUE_DEBUG
        dbg() << "FutexQueue @ " << this << ": wake_n unblocking " << *static_cast<Thread*>(data);
#endif
        ASSERT(did_wake < wake_count);
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
#ifdef FUTEXQUEUE_DEBUG
    dbg() << "FutexQueue @ " << this << ": wake_all";
#endif
    u32 did_wake = 0;
    do_unblock([&](Thread::Blocker& b, void* data, bool&) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Futex);
        auto& blocker = static_cast<Thread::FutexBlocker&>(b);
#ifdef FUTEXQUEUE_DEBUG
        dbg() << "FutexQueue @ " << this << ": wake_all unblocking " << *static_cast<Thread*>(data);
#endif
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
