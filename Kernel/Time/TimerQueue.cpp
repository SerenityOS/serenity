/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/Time/TimerQueue.h>

namespace Kernel {

static Singleton<TimerQueue> s_the;
static Spinlock<LockRank::None> g_timerqueue_lock {};

Duration Timer::remaining() const
{
    return m_remaining;
}

Duration Timer::now(bool is_firing) const
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
    return TimeManagement::the().current_time(clock_id);
}

TimerQueue& TimerQueue::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT TimerQueue::TimerQueue()
{
    m_ticks_per_second = TimeManagement::the().ticks_per_second();
}

bool TimerQueue::add_timer_without_id(NonnullRefPtr<Timer> timer, clockid_t clock_id, Duration const& deadline, Function<void()>&& callback)
{
    if (deadline <= TimeManagement::the().current_time(clock_id))
        return false;

    // Because timer handlers can execute on any processor and there is
    // a race between executing a timer handler and cancel_timer() this
    // *must* be a RefPtr<Timer>. Otherwise, calling cancel_timer() could
    // inadvertently cancel another timer that has been created between
    // returning from the timer handler and a call to cancel_timer().
    timer->setup(clock_id, deadline, move(callback));

    SpinlockLocker lock(g_timerqueue_lock);
    timer->m_id = 0; // Don't generate a timer id
    add_timer_locked(move(timer));
    return true;
}

TimerId TimerQueue::add_timer(NonnullRefPtr<Timer>&& timer)
{
    SpinlockLocker lock(g_timerqueue_lock);

    timer->m_id = ++m_timer_id_count;
    VERIFY(timer->m_id != 0); // wrapped
    auto id = timer->m_id;
    add_timer_locked(move(timer));
    return id;
}

void TimerQueue::add_timer_locked(NonnullRefPtr<Timer> timer)
{
    Duration timer_expiration = timer->m_expires;

    timer->clear_cancelled();
    timer->clear_callback_finished();
    timer->set_in_use();

    auto& queue = queue_for_timer(*timer);
    if (queue.list.is_empty()) {
        queue.list.append(timer.leak_ref());
        queue.next_timer_due = timer_expiration;
    } else {
        Timer* following_timer = nullptr;
        for (auto& t : queue.list) {
            if (t.m_expires > timer_expiration) {
                following_timer = &t;
                break;
            }
        }
        if (following_timer) {
            bool next_timer_needs_update = queue.list.first() == following_timer;
            queue.list.insert_before(*following_timer, timer.leak_ref());
            if (next_timer_needs_update)
                queue.next_timer_due = timer_expiration;
        } else {
            queue.list.append(timer.leak_ref());
        }
    }
}

bool TimerQueue::cancel_timer(Timer& timer, bool* was_in_use)
{
    bool in_use = timer.is_in_use();
    if (was_in_use)
        *was_in_use = in_use;

    // If the timer isn't in use, the cancellation is a no-op.
    if (!in_use) {
        VERIFY(!timer.m_list_node.is_in_list());
        return false;
    }

    bool did_already_run = timer.set_cancelled();
    auto& timer_queue = queue_for_timer(timer);
    if (!did_already_run) {
        timer.clear_in_use();

        SpinlockLocker lock(g_timerqueue_lock);
        if (timer_queue.list.contains(timer)) {
            // The timer has not fired, remove it
            VERIFY(timer.ref_count() > 1);
            remove_timer_locked(timer_queue, timer);
            return true;
        }

        // The timer was queued to execute but hasn't had a chance
        // to run. In this case, it should still be in m_timers_executing
        // and we don't need to spin. It still holds a reference
        // that will be dropped when it does get a chance to run,
        // but since we called set_cancelled it will only drop its reference
        VERIFY(m_timers_executing.contains(timer));
        m_timers_executing.remove(timer);
        return true;
    }

    // At this point the deferred call is queued and is being executed
    // on another processor. We need to wait until it's complete!
    while (!timer.is_callback_finished())
        Processor::wait_check();

    return false;
}

void TimerQueue::remove_timer_locked(Queue& queue, Timer& timer)
{
    bool was_next_timer = (queue.list.first() == &timer);
    queue.list.remove(timer);
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
    SpinlockLocker lock(g_timerqueue_lock);

    auto fire_timers = [&](Queue& queue) {
        auto* timer = queue.list.first();
        VERIFY(timer);
        VERIFY(queue.next_timer_due == timer->m_expires);

        while (timer && timer->now(true) > timer->m_expires) {
            queue.list.remove(*timer);

            m_timers_executing.append(*timer);

            update_next_timer_due(queue);

            lock.unlock();

            // Defer executing the timer outside of the irq handler
            Processor::deferred_call_queue([this, timer]() {
                // Check if we were cancelled in between being triggered
                // by the timer irq handler and now. If so, just drop
                // our reference and don't execute the callback.
                if (!timer->set_cancelled()) {
                    timer->m_callback();
                    SpinlockLocker lock(g_timerqueue_lock);
                    m_timers_executing.remove(*timer);
                }
                timer->clear_in_use();
                timer->set_callback_finished();
                // Drop the reference we added when queueing the timer
                timer->unref();
            });

            lock.lock();
            timer = queue.list.first();
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

    if (auto* next_timer = queue.list.first())
        queue.next_timer_due = next_timer->m_expires;
    else
        queue.next_timer_due = {};
}

}
