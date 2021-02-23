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

ALWAYS_INLINE static u64 time_to_ns(const timespec& ts)
{
    return (u64)ts.tv_sec * 1000000000ull + ts.tv_nsec;
}

ALWAYS_INLINE static timespec ns_to_time(u64 ns)
{
    return { (time_t)(ns / 1000000000ull), (long)(ns % 1000000000ull) };
}

timespec Timer::remaining() const
{
    if (m_remaining == 0)
        return {};
    return ns_to_time(m_remaining);
}

u64 Timer::now(bool is_firing) const
{
    // NOTE: If is_firing is true then TimePrecision::Precise isn't really useful here.
    // We already have a quite precise time stamp because we just updated the time in the
    // interrupt handler. In those cases, just use coarse timestamps.
    auto clock_id = m_clock_id;
    if (is_firing) {
        switch (clock_id) {
        case CLOCK_MONOTONIC:
            clock_id = CLOCK_MONOTONIC_COARSE;
            break;
        case CLOCK_MONOTONIC_RAW:
            // TODO: use a special CLOCK_MONOTONIC_RAW_COARSE like mechanism here
            break;
        case CLOCK_REALTIME:
            clock_id = CLOCK_REALTIME_COARSE;
            break;
        default:
            break;
        }
    }
    return time_to_ns(TimeManagement::the().current_time(clock_id).value());
}

TimerQueue& TimerQueue::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT TimerQueue::TimerQueue()
{
    m_ticks_per_second = TimeManagement::the().ticks_per_second();
}

RefPtr<Timer> TimerQueue::add_timer_without_id(clockid_t clock_id, const timespec& deadline, Function<void()>&& callback)
{
    if (deadline <= TimeManagement::the().current_time(clock_id).value())
        return {};

    // Because timer handlers can execute on any processor and there is
    // a race between executing a timer handler and cancel_timer() this
    // *must* be a RefPtr<Timer>. Otherwise calling cancel_timer() could
    // inadvertently cancel another timer that has been created between
    // returning from the timer handler and a call to cancel_timer().
    auto timer = adopt(*new Timer(clock_id, time_to_ns(deadline), move(callback)));

    ScopedSpinLock lock(g_timerqueue_lock);
    timer->m_id = 0; // Don't generate a timer id
    add_timer_locked(timer);
    return timer;
}

TimerId TimerQueue::add_timer(NonnullRefPtr<Timer>&& timer)
{
    ScopedSpinLock lock(g_timerqueue_lock);

    timer->m_id = ++m_timer_id_count;
    VERIFY(timer->m_id != 0); // wrapped
    add_timer_locked(move(timer));
    return m_timer_id_count;
}

void TimerQueue::add_timer_locked(NonnullRefPtr<Timer> timer)
{
    u64 timer_expiration = timer->m_expires;

    VERIFY(!timer->is_queued());

    auto& queue = queue_for_timer(*timer);
    if (queue.list.is_empty()) {
        queue.list.append(&timer.leak_ref());
        queue.next_timer_due = timer_expiration;
    } else {
        Timer* following_timer = nullptr;
        queue.list.for_each([&](Timer& t) {
            if (t.m_expires > timer_expiration) {
                following_timer = &t;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (following_timer) {
            bool next_timer_needs_update = queue.list.head() == following_timer;
            queue.list.insert_before(following_timer, &timer.leak_ref());
            if (next_timer_needs_update)
                queue.next_timer_due = timer_expiration;
        } else {
            queue.list.append(&timer.leak_ref());
        }
    }
}

TimerId TimerQueue::add_timer(clockid_t clock_id, timeval& deadline, Function<void()>&& callback)
{
    auto expires = TimeManagement::the().current_time(clock_id).value();
    timespec_add_timeval(expires, deadline, expires);
    return add_timer(adopt(*new Timer(clock_id, time_to_ns(expires), move(callback))));
}

bool TimerQueue::cancel_timer(TimerId id)
{
    Timer* found_timer = nullptr;
    Queue* timer_queue = nullptr;

    ScopedSpinLock lock(g_timerqueue_lock);
    if (m_timer_queue_monotonic.list.for_each([&](Timer& timer) {
            if (timer.m_id == id) {
                found_timer = &timer;
                timer_queue = &m_timer_queue_monotonic;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        })
        != IterationDecision::Break) {
        m_timer_queue_realtime.list.for_each([&](Timer& timer) {
            if (timer.m_id == id) {
                found_timer = &timer;
                timer_queue = &m_timer_queue_realtime;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    }

    if (!found_timer) {
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

    VERIFY(found_timer);
    VERIFY(timer_queue);
    remove_timer_locked(*timer_queue, *found_timer);
    return true;
}

bool TimerQueue::cancel_timer(Timer& timer)
{
    auto& timer_queue = queue_for_timer(timer);
    ScopedSpinLock lock(g_timerqueue_lock);
    if (!timer_queue.list.contains_slow(&timer)) {
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

    VERIFY(timer.ref_count() > 1);
    remove_timer_locked(timer_queue, timer);
    return true;
}

void TimerQueue::remove_timer_locked(Queue& queue, Timer& timer)
{
    bool was_next_timer = (queue.list.head() == &timer);
    queue.list.remove(&timer);
    timer.set_queued(false);
    auto now = timer.now(false);
    if (timer.m_expires > now)
        timer.m_remaining = timer.m_expires - now;

    if (was_next_timer)
        update_next_timer_due(queue);
    // Whenever we remove a timer that was still queued (but hasn't been
    // fired) we added a reference to it. So, when removing it from the
    // queue we need to drop that reference.
    timer.unref();
}

void TimerQueue::fire()
{
    ScopedSpinLock lock(g_timerqueue_lock);

    auto fire_timers = [&](Queue& queue) {
        auto* timer = queue.list.head();
        VERIFY(timer);
        VERIFY(queue.next_timer_due == timer->m_expires);

        while (timer && timer->now(true) > timer->m_expires) {
            queue.list.remove(timer);
            timer->set_queued(false);

            m_timers_executing.append(timer);

            update_next_timer_due(queue);

            lock.unlock();

            // Defer executing the timer outside of the irq handler
            Processor::current().deferred_call_queue([this, timer]() {
                timer->m_callback();
                ScopedSpinLock lock(g_timerqueue_lock);
                m_timers_executing.remove(timer);
                // Drop the reference we added when queueing the timer
                timer->unref();
            });

            lock.lock();
            timer = queue.list.head();
        }
    };

    if (!m_timer_queue_monotonic.list.is_empty())
        fire_timers(m_timer_queue_monotonic);
    if (!m_timer_queue_realtime.list.is_empty())
        fire_timers(m_timer_queue_realtime);
}

void TimerQueue::update_next_timer_due(Queue& queue)
{
    VERIFY(g_timerqueue_lock.is_locked());

    if (auto* next_timer = queue.list.head())
        queue.next_timer_due = next_timer->m_expires;
    else
        queue.next_timer_due = 0;
}

}
