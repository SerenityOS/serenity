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
#include <AK/Time.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

namespace Kernel {

static AK::Singleton<TimerQueue> s_the;
static SpinLock<u8> g_timerqueue_lock;

TimerQueue& TimerQueue::the()
{
    return *s_the;
}

TimerQueue::TimerQueue()
{
    m_ticks_per_second = TimeManagement::the().ticks_per_second();
}

RefPtr<Timer> TimerQueue::add_timer_without_id(const timespec& deadline, Function<void()>&& callback)
{
    if (deadline <= TimeManagement::the().monotonic_time())
        return {};

    // Because timer handlers can execute on any processor and there is
    // a race between executing a timer handler and cancel_timer() this
    // *must* be a RefPtr<Timer>. Otherwise calling cancel_timer() could
    // inadvertently cancel another timer that has been created between
    // returning from the timer handler and a call to cancel_timer().
    auto timer = adopt(*new Timer(time_to_ticks(deadline), move(callback)));

    ScopedSpinLock lock(g_timerqueue_lock);
    timer->m_id = 0; // Don't generate a timer id
    add_timer_locked(timer);
    return timer;
}

TimerId TimerQueue::add_timer(NonnullRefPtr<Timer>&& timer)
{
    ScopedSpinLock lock(g_timerqueue_lock);

    timer->m_id = ++m_timer_id_count;
    ASSERT(timer->m_id != 0); // wrapped
    add_timer_locked(move(timer));
    return m_timer_id_count;
}

void TimerQueue::add_timer_locked(NonnullRefPtr<Timer> timer)
{
    u64 timer_expiration = timer->m_expires;
    ASSERT(timer_expiration >= time_to_ticks(TimeManagement::the().monotonic_time()));

    ASSERT(!timer->is_queued());

    if (m_timer_queue.is_empty()) {
        m_timer_queue.append(&timer.leak_ref());
        m_next_timer_due = timer_expiration;
    } else {
        Timer* following_timer = nullptr;
        m_timer_queue.for_each([&](Timer& t) {
            if (t.m_expires > timer_expiration) {
                following_timer = &t;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (following_timer) {
            bool next_timer_needs_update = m_timer_queue.head() == following_timer;
            m_timer_queue.insert_before(following_timer, &timer.leak_ref());
            if (next_timer_needs_update)
                m_next_timer_due = timer_expiration;
        } else {
            m_timer_queue.append(&timer.leak_ref());
        }
    }
}

TimerId TimerQueue::add_timer(timeval& deadline, Function<void()>&& callback)
{
    auto expires = TimeManagement::the().monotonic_time();
    timespec_add_timeval(expires, deadline, expires);
    return add_timer(adopt(*new Timer(time_to_ticks(expires), move(callback))));
}

timespec TimerQueue::ticks_to_time(u64 ticks) const
{
    timespec tspec;
    tspec.tv_sec = ticks / m_ticks_per_second;
    tspec.tv_nsec = (ticks % m_ticks_per_second) * (1'000'000'000 / m_ticks_per_second);
    ASSERT(tspec.tv_nsec <= 1'000'000'000);
    return tspec;
}

u64 TimerQueue::time_to_ticks(const timespec& tspec) const
{
    u64 ticks = (u64)tspec.tv_sec * m_ticks_per_second;
    ticks += ((u64)tspec.tv_nsec * m_ticks_per_second) / 1'000'000'000;
    return ticks;
}

bool TimerQueue::cancel_timer(TimerId id)
{
    ScopedSpinLock lock(g_timerqueue_lock);
    Timer* found_timer = nullptr;
    if (m_timer_queue.for_each([&](Timer& timer) {
            if (timer.m_id == id) {
                found_timer = &timer;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        })
        != IterationDecision::Break) {
        // The timer may be executing right now, if it is then it should
        // be in m_timers_executing. If it is then release the lock
        // briefly to allow it to finish by removing itself
        // NOTE: This can only happen with multiple processors!
        while (m_timers_executing.for_each([&](Timer& timer) {
            if (timer.m_id == id)
                return IterationDecision::Break;
            return IterationDecision::Continue;
        }) == IterationDecision::Break) {
            // NOTE: This isn't the most efficient way to wait, but
            // it should only happen when multiple processors are used.
            // Also, the timers should execute pretty quickly, so it
            // should not loop here for very long. But we can't yield.
            lock.unlock();
            Processor::wait_check();
            lock.lock();
        }
        // We were not able to cancel the timer, but at this point
        // the handler should have completed if it was running!
        return false;
    }

    ASSERT(found_timer);
    bool was_next_timer = (m_timer_queue.head() == found_timer);

    m_timer_queue.remove(found_timer);
    found_timer->set_queued(false);

    if (was_next_timer)
        update_next_timer_due();

    lock.unlock();
    found_timer->unref();
    return true;
}

bool TimerQueue::cancel_timer(Timer& timer)
{
    ScopedSpinLock lock(g_timerqueue_lock);
    if (!m_timer_queue.contains_slow(&timer)) {
        // The timer may be executing right now, if it is then it should
        // be in m_timers_executing. If it is then release the lock
        // briefly to allow it to finish by removing itself
        // NOTE: This can only happen with multiple processors!
        while (m_timers_executing.contains_slow(&timer)) {
            // NOTE: This isn't the most efficient way to wait, but
            // it should only happen when multiple processors are used.
            // Also, the timers should execute pretty quickly, so it
            // should not loop here for very long. But we can't yield.
            lock.unlock();
            Processor::wait_check();
            lock.lock();
        }
        // We were not able to cancel the timer, but at this point
        // the handler should have completed if it was running!
        return false;
    }

    bool was_next_timer = (m_timer_queue.head() == &timer);
    m_timer_queue.remove(&timer);
    timer.set_queued(false);

    if (was_next_timer)
        update_next_timer_due();
    return true;
}

void TimerQueue::fire()
{
    ScopedSpinLock lock(g_timerqueue_lock);
    auto* timer = m_timer_queue.head();
    if (!timer)
        return;

    ASSERT(m_next_timer_due == timer->m_expires);

    while (timer && TimeManagement::the().monotonic_ticks() > timer->m_expires) {
        m_timer_queue.remove(timer);
        m_timers_executing.append(timer);

        update_next_timer_due();

        lock.unlock();
        timer->m_callback();
        lock.lock();

        m_timers_executing.remove(timer);
        timer->set_queued(false);
        timer->unref();

        timer = m_timer_queue.head();
    }
}

void TimerQueue::update_next_timer_due()
{
    ASSERT(g_timerqueue_lock.is_locked());

    if (auto* next_timer = m_timer_queue.head())
        m_next_timer_due = next_timer->m_expires;
    else
        m_next_timer_due = 0;
}

}
