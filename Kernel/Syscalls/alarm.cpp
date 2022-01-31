/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$alarm(unsigned seconds)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    unsigned previous_alarm_remaining = 0;
    if (m_alarm_timer) {
        bool was_in_use = false;
        if (TimerQueue::the().cancel_timer(*m_alarm_timer, &was_in_use)) {
            // The timer hasn't fired. Round up the remaining time (if any)
            Time remaining = m_alarm_timer->remaining() + Time::from_nanoseconds(999'999'999);
            previous_alarm_remaining = remaining.to_truncated_seconds();
        }
        // We had an existing alarm, must return a non-zero value here!
        if (was_in_use && previous_alarm_remaining == 0)
            previous_alarm_remaining = 1;
    }

    if (seconds > 0) {
        auto deadline = TimeManagement::the().current_time(CLOCK_REALTIME_COARSE);
        deadline = deadline + Time::from_seconds(seconds);
        if (!m_alarm_timer) {
            m_alarm_timer = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Timer));
        }
        auto timer_was_added = TimerQueue::the().add_timer_without_id(*m_alarm_timer, CLOCK_REALTIME_COARSE, deadline, [this]() {
            [[maybe_unused]] auto rc = send_signal(SIGALRM, nullptr);
        });
        if (!timer_was_added)
            return ENOMEM;
    }
    return previous_alarm_remaining;
}

}
