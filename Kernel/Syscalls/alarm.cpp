/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

KResultOr<unsigned> Process::sys$alarm(unsigned seconds)
{
    REQUIRE_PROMISE(stdio);
    unsigned previous_alarm_remaining = 0;
    if (m_alarm_timer) {
        if (TimerQueue::the().cancel_timer(*m_alarm_timer)) {
            // The timer hasn't fired. Round up the remaining time (if any)
            Time remaining = m_alarm_timer->remaining() + Time::from_nanoseconds(999'999'999);
            previous_alarm_remaining = remaining.to_truncated_seconds();
        }
        // We had an existing alarm, must return a non-zero value here!
        if (previous_alarm_remaining == 0)
            previous_alarm_remaining = 1;
    }

    if (seconds > 0) {
        auto deadline = TimeManagement::the().current_time(CLOCK_REALTIME_COARSE);
        deadline = deadline + Time::from_seconds(seconds);
        if (!m_alarm_timer) {
            m_alarm_timer = adopt_ref_if_nonnull(new Timer());
            if (!m_alarm_timer)
                return ENOMEM;
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
