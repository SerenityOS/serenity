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
#include <AK/Singleton.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

namespace Kernel {

static AK::Singleton<TimerQueue> s_the;

TimerQueue& TimerQueue::the()
{
    return *s_the;
}

TimerQueue::TimerQueue()
{
    m_ticks_per_second = TimeManagement::the().ticks_per_second();
}

TimerId TimerQueue::add_timer(NonnullOwnPtr<Timer>&& timer)
{
    u64 timer_expiration = timer->expires;
    ASSERT(timer_expiration >= g_uptime);

    timer->id = ++m_timer_id_count;

    if (m_timer_queue.is_empty()) {
        m_timer_queue.append(move(timer));
        m_next_timer_due = timer_expiration;
    } else {
        auto following_timer = m_timer_queue.find([&timer_expiration](auto& other) { return other->expires > timer_expiration; });

        if (following_timer.is_end()) {
            m_timer_queue.append(move(timer));
        } else {
            auto next_timer_needs_update = following_timer.is_begin();
            m_timer_queue.insert_before(following_timer, move(timer));

            if (next_timer_needs_update)
                m_next_timer_due = timer_expiration;
        }
    }

    return m_timer_id_count;
}

TimerId TimerQueue::add_timer(timeval& deadline, Function<void()>&& callback)
{
    NonnullOwnPtr timer = make<Timer>();
    timer->expires = g_uptime + seconds_to_ticks(deadline.tv_sec) + microseconds_to_ticks(deadline.tv_usec);
    timer->callback = move(callback);
    return add_timer(move(timer));
}

bool TimerQueue::cancel_timer(TimerId id)
{
    auto it = m_timer_queue.find([id](auto& timer) { return timer->id == id; });
    if (it.is_end())
        return false;

    auto was_next_timer = it.is_begin();
    m_timer_queue.remove(it);

    if (was_next_timer)
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

}
