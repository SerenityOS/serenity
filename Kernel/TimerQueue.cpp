/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

Time Timer::remaining() const
{
    return m_remaining;
}

Time Timer::now(bool is_firing) const
{
    // NOTE: If is_firing is true then TimePrecision::Precise isn't really useful here.
    // We already have a quite precise time stamp because we just updated the time in the
    // interrupt handler. In those cases, just use coarse timestamps.
    auto clock_id = m_clock_id;
    if (is_firing)
        clock_id = TimeManagement::convert_clock_id(clock_id, TimePrecision::Coarse);

    return TimeManagement::the().current_time(clock_id).value();
}

TimerQueue& TimerQueue::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT TimerQueue::TimerQueue()
{
    m_ticks_per_second = TimeManagement::the().ticks_per_second();
}

RefPtr<Timer> TimerQueue::add_timer_without_id(clockid_t clock_id, const Time& deadline, Function<void()>&& callback)
{
    if (deadline <= TimeManagement::the().current_time(clock_id).value())
        return {};

    // Because timer handlers can execute on any processor and there is
    // a race between executing a timer handler and cancel_timer() this
    // *must* be a RefPtr<Timer>. Otherwise calling cancel_timer() could
    // inadvertently cancel another timer that has been created between
    // returning from the timer handler and a call to cancel_timer().
    auto timer = adopt_ref(*new Timer(clock_id, deadline, move(callback)));

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
    return timer->m_id;
}

void TimerQueue::add_timer_locked(NonnullRefPtr<Timer> timer)
{
    Time timer_expiration = timer->m_expires;

    VERIFY(!timer->is_queued());

    auto* timer_ptr = timer.ptr();
    dbgln("add timer {:p} expires at {}", timer_ptr, timer->m_expires);
    auto& queue = queue_for_timer(*timer);
    if (queue.list.is_empty()) {
        queue.list.append(&timer.leak_ref());
        queue.next_timer_due = timer_ptr;
        next_timer_was_updated_locked();
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
            if (next_timer_needs_update) {
                queue.next_timer_due = timer_ptr;
                next_timer_was_updated_locked();
            }
        } else {
            queue.list.append(&timer.leak_ref());
        }
    }
}

TimerId TimerQueue::add_timer(clockid_t clock_id, const Time& deadline, Function<void()>&& callback)
{
    auto expires = TimeManagement::the().current_time(clock_id).value();
    expires = expires + deadline;
    return add_timer(adopt_ref(*new Timer(clock_id, expires, move(callback))));
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
        if (!timer.set_done()) {
            // The timer was queued to execute but hasn't had a chance
            // to run. In this case, it should still be in m_timers_executing
            // and we don't need to spin. It still holds a reference
            // that will be dropped when it does get a chance to run,
            // but since we called set_done it will only drop its reference
            VERIFY(m_timers_executing.contains_slow(&timer));
            m_timers_executing.remove(&timer);
            return true;
        }
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
            // NOTE: We may still be in a critical section here. But that's
            // ok because then the callback was only queued but won't be
            // run and we would have bailed already because set_done
            // would have returned false.
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
        update_next_timer_due_locked(queue);
    // Whenever we remove a timer that was still queued (but hasn't been
    // fired) we added a reference to it. So, when removing it from the
    // queue we need to drop that reference.
    timer.unref();
}

bool TimerQueue::fire(bool from_interrupt)
{
    bool expired_any = false;
    bool had_any_timers = false;
    bool still_have_timers = false;
    ScopedSpinLock lock(g_timerqueue_lock);

    auto fire_timers = [&](Queue& queue) {
        auto* timer = queue.list.head();
        VERIFY(timer);
        VERIFY(queue.next_timer_due == timer);

        auto current_time = timer->now(true);
        dbgln("check if we can fire timers, now: {} timer {:p} > expires at {} -> comparison: {}", current_time, timer, timer->m_expires, current_time > timer->m_expires);
        while (timer && timer->now(true) > timer->m_expires) {
            queue.list.remove(timer);
            timer->set_queued(false);

            m_timers_executing.append(timer);

            update_next_timer_due_locked(queue);

            lock.unlock();

            expired_any |= true;
            dbgln("Firing timer {:p}", timer);

            // Defer executing the timer outside of the irq handler
            Processor::deferred_call_queue([this, timer]() {
                // Check if we were cancelled in between being triggered
                // by the timer irq handler and now. If so, just drop
                // our reference and don't execute the callback.
                if (!timer->set_done()) {
                    timer->m_callback();
                    ScopedSpinLock lock(g_timerqueue_lock);
                    m_timers_executing.remove(timer);
                }
                // Drop the reference we added when queueing the timer
                timer->unref();
            });

            lock.lock();
            timer = queue.list.head();
        }
    };

    if (!m_timer_queue_monotonic.list.is_empty()) {
        had_any_timers = true;
        fire_timers(m_timer_queue_monotonic);
        still_have_timers = true;
    }
    if (!m_timer_queue_realtime.list.is_empty()) {
        had_any_timers = true;
        fire_timers(m_timer_queue_realtime);
        still_have_timers = true;
    }
    if (still_have_timers || (!expired_any && from_interrupt && had_any_timers)) {
        // In tickless mode we may have triggered an interrupt too early,
        // especially with non-monotonic clocks. Check if we need to
        // schedule another timer
        next_timer_was_updated_locked();
    }
    return expired_any;
}

void TimerQueue::update_next_timer_due_locked(Queue& queue)
{
    VERIFY(g_timerqueue_lock.is_locked());

    if (auto* next_timer = queue.list.head())
        queue.next_timer_due = next_timer;
    else
        queue.next_timer_due = nullptr;
    next_timer_was_updated_locked();
}

void TimerQueue::tickless_update_system_timer()
{
    ScopedSpinLock lock(g_timerqueue_lock);
    tickless_update_system_timer_locked();
}

void TimerQueue::tickless_update_system_timer_locked()
{
    // If we're tickless mode we need to figure out when to fire the
    // next time. For realtime this will only be approximate, which
    // means that depending on how far out the expiration time is
    // we may end up scheduling too early (which would require us
    // to schedule another timer) or too late.
    Optional<Time> next_expiration;
    auto check_queue = [&]<bool is_monotonic>(Queue& queue) {
        auto* timer = queue.next_timer_due;
        if (!timer)
            return;
        Time deadline_monotonic;
        if constexpr (is_monotonic) {
            deadline_monotonic = timer->m_expires;
        } else {
            // TODO: For realtime expiration time far in the future we should
            // probably impose a max time to minimize overshoot. This would
            // trigger some "useless" timer interrupts periodically, but
            // we can then adjust for time changes (e.g. due to ntp)
            auto diff = TimeManagement::the().monotonic_to_epoch_diff();
            dbgln_if(TICKLESS_DEBUG, "diff to monotonic: {}", diff);
            deadline_monotonic = timer->m_expires - diff;
        }
        if (!next_expiration.has_value() || deadline_monotonic < next_expiration.value()) {
            next_expiration = deadline_monotonic;
            dbgln_if(TICKLESS_DEBUG, "Timer next up: {:p} ({})", timer, deadline_monotonic);
        }
    };
    check_queue.template operator()<true>(m_timer_queue_monotonic);
    check_queue.template operator()<false>(m_timer_queue_realtime);

    auto check_update_timer = [&] {
        if (next_expiration.has_value()) {
            auto result = TimeManagement::the().tickless_start_system_timer(next_expiration.value());
            if (result == TimeManagement::TicklessTimerResult::InPast) {
                // Timer wasn't scheduled, deadline was in the past
                dbgln("Next expiration {} in the past, fire as soon as possible", next_expiration.value());
                Processor::current().deferred_call_queue([] {
                    dbgln("Timer in the past, fire now");
                    if (!TimerQueue::the().fire(false))
                        dbgln_if(TICKLESS_DEBUG, "Timer in the past, no timers were expired!");
                });
            }
        } else {
            dbgln_if(TICKLESS_DEBUG, "Timer not pending");
            TimeManagement::the().tickless_cancel_system_timer();
        }
    };
    if (Processor::id() == 0)
        check_update_timer();
    else {
        Processor::smp_unicast(0, [&] {
            check_update_timer();
        }, true);
    }
}

}
