/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/Time.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, TimerId);

class Timer final : public AtomicRefCounted<Timer> {
    friend class TimerQueue;

public:
    void setup(clockid_t clock_id, Duration expires, Function<void()>&& callback)
    {
        VERIFY(!is_queued());
        m_clock_id = clock_id;
        m_expires = expires;
        m_callback = move(callback);
    }

    ~Timer()
    {
        VERIFY(!is_queued());
    }

    Duration remaining() const;

private:
    TimerId m_id;
    clockid_t m_clock_id;
    Duration m_expires;
    Duration m_remaining {};
    Function<void()> m_callback;
    Atomic<bool> m_cancelled { false };
    Atomic<bool> m_callback_finished { false };
    Atomic<bool> m_in_use { false };

    bool operator<(Timer const& rhs) const
    {
        return m_expires < rhs.m_expires;
    }
    bool operator>(Timer const& rhs) const
    {
        return m_expires > rhs.m_expires;
    }
    bool operator==(Timer const& rhs) const
    {
        return m_id == rhs.m_id;
    }

    void clear_cancelled() { return m_cancelled.store(false, AK::memory_order_release); }
    bool set_cancelled() { return m_cancelled.exchange(true, AK::memory_order_acq_rel); }

    bool is_in_use() { return m_in_use.load(AK::memory_order_acquire); }
    void set_in_use() { m_in_use.store(true, AK::memory_order_release); }
    void clear_in_use() { return m_in_use.store(false, AK::memory_order_release); }

    bool is_callback_finished() const { return m_callback_finished.load(AK::memory_order_acquire); }
    void clear_callback_finished() { m_callback_finished.store(false, AK::memory_order_release); }
    void set_callback_finished() { m_callback_finished.store(true, AK::memory_order_release); }

    Duration now(bool) const;

    bool is_queued() const { return m_list_node.is_in_list(); }

public:
    IntrusiveListNode<Timer> m_list_node;
    using List = IntrusiveList<&Timer::m_list_node>;
};

class TimerQueue {
    friend class Timer;

public:
    TimerQueue();
    static TimerQueue& the();

    TimerId add_timer(NonnullRefPtr<Timer>&&);
    bool add_timer_without_id(NonnullRefPtr<Timer>, clockid_t, Duration const&, Function<void()>&&);
    bool cancel_timer(Timer& timer, bool* was_in_use = nullptr);
    void fire();

private:
    struct Queue {
        Timer::List list;
        Duration next_timer_due {};
    };
    void remove_timer_locked(Queue&, Timer&);
    void update_next_timer_due(Queue&);
    void add_timer_locked(NonnullRefPtr<Timer>);

    Queue& queue_for_timer(Timer& timer)
    {
        switch (timer.m_clock_id) {
        case CLOCK_MONOTONIC:
        case CLOCK_MONOTONIC_COARSE:
        case CLOCK_MONOTONIC_RAW:
            return m_timer_queue_monotonic;
        case CLOCK_REALTIME:
        case CLOCK_REALTIME_COARSE:
            return m_timer_queue_realtime;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    u64 m_timer_id_count { 0 };
    u64 m_ticks_per_second { 0 };
    Queue m_timer_queue_monotonic;
    Queue m_timer_queue_realtime;
    Timer::List m_timers_executing;
};

}
