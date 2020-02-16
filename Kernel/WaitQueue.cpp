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

namespace Kernel {

WaitQueue::WaitQueue()
{
}

WaitQueue::~WaitQueue()
{
}

void WaitQueue::enqueue(Thread& thread)
{
    InterruptDisabler disabler;
    m_threads.append(thread);
}

void WaitQueue::wake_one(Atomic<bool>* lock)
{
    InterruptDisabler disabler;
    if (lock)
        *lock = false;
    if (m_threads.is_empty())
        return;
    if (auto* thread = m_threads.take_first())
        thread->wake_from_queue();
    Scheduler::stop_idling();
}

void WaitQueue::wake_all()
{
    InterruptDisabler disabler;
    if (m_threads.is_empty())
        return;
    while (!m_threads.is_empty())
        m_threads.take_first()->wake_from_queue();
    Scheduler::stop_idling();
}

}
