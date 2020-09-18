/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

WaitQueue::WaitQueue()
{
}

WaitQueue::~WaitQueue()
{
}

bool WaitQueue::enqueue(Thread& thread)
{
    ScopedSpinLock queue_lock(m_lock);
    if (m_wake_requested) {
        // wake_* was called when no threads were in the queue
        // we shouldn't wait at all
        m_wake_requested = false;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": enqueue: wake_all pending";
#endif
        return false;
    }
    m_threads.append(thread);
    return true;
}

bool WaitQueue::dequeue(Thread& thread)
{
    ScopedSpinLock queue_lock(m_lock);
    if (m_threads.contains(thread)) {
        m_threads.remove(thread);
        return true;
    }
    return false;
}

void WaitQueue::wake_one(Atomic<bool>* lock)
{
    ScopedSpinLock queue_lock(m_lock);
    if (lock)
        *lock = false;
    if (m_threads.is_empty()) {
        // Save the fact that a wake was requested
        m_wake_requested = true;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_one: nobody to wake, mark as pending";
#endif
        return;
    }
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_one:";
#endif
    auto* thread = m_threads.take_first();
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_one: wake thread " << *thread;
#endif
    thread->wake_from_queue();
    m_wake_requested = false;
    Scheduler::yield();
}

void WaitQueue::wake_n(u32 wake_count)
{
    ScopedSpinLock queue_lock(m_lock);
    if (m_threads.is_empty()) {
        // Save the fact that a wake was requested
        m_wake_requested = true;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_n: nobody to wake, mark as pending";
#endif
        return;
    }

#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_n: " << wake_count;
#endif
    for (u32 i = 0; i < wake_count; ++i) {
        Thread* thread = m_threads.take_first();
        if (!thread)
            break;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_n: wake thread " << *thread;
#endif
        thread->wake_from_queue();
    }
    m_wake_requested = false;
    Scheduler::yield();
}

void WaitQueue::wake_all()
{
    ScopedSpinLock queue_lock(m_lock);
    if (m_threads.is_empty()) {
        // Save the fact that a wake was requested
        m_wake_requested = true;
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_all: nobody to wake, mark as pending";
#endif
        return;
    }
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_all: ";
#endif
    while (!m_threads.is_empty()) {
        Thread* thread = m_threads.take_first();
#ifdef WAITQUEUE_DEBUG
        dbg() << "WaitQueue " << VirtualAddress(this) << ": wake_all: wake thread " << *thread;
#endif
        thread->wake_from_queue();
    }
    m_wake_requested = false;
    Scheduler::yield();
}

void WaitQueue::clear()
{
    ScopedSpinLock queue_lock(m_lock);
#ifdef WAITQUEUE_DEBUG
    dbg() << "WaitQueue " << VirtualAddress(this) << ": clear";
#endif
    m_threads.clear();
    m_wake_requested = false;
}

}
