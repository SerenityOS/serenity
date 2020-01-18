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

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <Kernel/Scheduler.h>
#include <Kernel/TimerQueue.h>

static TimerQueue* s_the;

TimerQueue& TimerQueue::the()
{
    if (!s_the)
        s_the = new TimerQueue;
    return *s_the;
}

u64 TimerQueue::add_timer(NonnullOwnPtr<Timer>&& timer)
{
    ASSERT(timer->expires > g_uptime);

    timer->id = ++m_timer_id_count;

    auto following_timer = m_timer_queue.find([&timer](auto& other) { return other->expires > timer->expires; });
    if (following_timer.is_end())
        m_timer_queue.append(move(timer));
    else
        m_timer_queue.insert_before(following_timer, move(timer));

    update_next_timer_due();

    return m_timer_id_count;
}

u64 TimerQueue::add_timer(u64 duration, TimeUnit unit, Function<void()>&& callback)
{
    NonnullOwnPtr timer = make<Timer>();
    timer->expires = g_uptime + duration * unit;
    timer->callback = move(callback);
    return add_timer(move(timer));
}

bool TimerQueue::cancel_timer(u64 id)
{
    auto it = m_timer_queue.find([id](auto& timer) { return timer->id == id; });
    if (it.is_end())
        return false;
    m_timer_queue.remove(it);
    update_next_timer_due();
    return true;
}

void TimerQueue::fire()
{
    if (m_timer_queue.is_empty())
        return;

    ASSERT(m_next_timer_due == m_timer_queue.first()->expires);

    while (!m_timer_queue.is_empty() && g_uptime > m_timer_queue.first()->expires) {
        auto timer = m_timer_queue.take_first();
        timer->callback();
    }

    update_next_timer_due();
}

void TimerQueue::update_next_timer_due()
{
    if (m_timer_queue.is_empty())
        m_next_timer_due = 0;
    else
        m_next_timer_due = m_timer_queue.first()->expires;
}
