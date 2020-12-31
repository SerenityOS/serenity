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

#include <Kernel/Thread.h>
#include <Kernel/WaitQueue.h>

//#define WAITQUEUE_DEBUG

namespace Kernel {

bool WaitQueue::should_add_blocker(Thread::Blocker& b, void* data)
{
    ASSERT(data != nullptr); // Thread that is requesting to be blocked
    ASSERT(m_lock.is_locked());
    ASSERT(b.blocker_type() == Thread::Blocker::Type::Queue);
    if (m_wake_requested) {
        m_wake_requested = false;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue @ " << this << ": do not block thread " << *static_cast<Thread*>(data) << ", wake was pending";
#endif
        return false;
    }
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue @ " << this << ": should block thread " << *static_cast<Thread*>(data);
#endif
    return true;
}

void WaitQueue::wake_one()
{
    ScopedSpinLock lock(m_lock);
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue @ " << this << ": wake_one";
#endif
    bool did_unblock_one = do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::QueueBlocker&>(b);
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue @ " << this << ": wake_one unblocking " << *static_cast<Thread*>(data);
#endif
        if (blocker.unblock()) {
            stop_iterating = true;
            return true;
        }
        return false;
    });
    m_wake_requested = !did_unblock_one;
}

void WaitQueue::wake_n(u32 wake_count)
{
    if (wake_count == 0)
        return; // should we assert instead?
    ScopedSpinLock lock(m_lock);
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue @ " << this << ": wake_n(" << wake_count << ")";
#endif
    bool did_unblock_some = do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::QueueBlocker&>(b);
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue @ " << this << ": wake_n unblocking " << *static_cast<Thread*>(data);
#endif
        ASSERT(wake_count > 0);
        if (blocker.unblock()) {
            if (--wake_count == 0)
                stop_iterating = true;
            return true;
        }
        return false;
    });
    m_wake_requested = !did_unblock_some;
}

void WaitQueue::wake_all()
{
    ScopedSpinLock lock(m_lock);
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue @ " << this << ": wake_all";
#endif
    bool did_unblock_any = do_unblock([&](Thread::Blocker& b, void* data, bool&) {
        ASSERT(data);
        ASSERT(b.blocker_type() == Thread::Blocker::Type::Queue);
        auto& blocker = static_cast<Thread::QueueBlocker&>(b);
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue @ " << this << ": wake_all unblocking " << *static_cast<Thread*>(data);
#endif
        bool did_unblock = blocker.unblock();
        ASSERT(did_unblock);
        return true;
    });
    m_wake_requested = !did_unblock_any;
}

}
