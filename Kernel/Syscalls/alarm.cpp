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

#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

KResultOr<unsigned> Process::sys$alarm(unsigned seconds)
{
    REQUIRE_PROMISE(stdio);
    unsigned previous_alarm_remaining = 0;
    if (auto alarm_timer = move(m_alarm_timer)) {
        if (TimerQueue::the().cancel_timer(*alarm_timer)) {
            // The timer hasn't fired. Round up the remaining time (if any)
            Time remaining = alarm_timer->remaining() + Time::from_nanoseconds(999'999'999);
            previous_alarm_remaining = remaining.to_truncated_seconds();
        }
        // We had an existing alarm, must return a non-zero value here!
        if (previous_alarm_remaining == 0)
            previous_alarm_remaining = 1;
    }

    if (seconds > 0) {
        auto deadline = TimeManagement::the().current_time(CLOCK_REALTIME_COARSE).value();
        deadline = deadline + Time::from_seconds(seconds);
        m_alarm_timer = TimerQueue::the().add_timer_without_id(CLOCK_REALTIME_COARSE, deadline, [this]() {
            [[maybe_unused]] auto rc = send_signal(SIGALRM, nullptr);
        });
    }
    return previous_alarm_remaining;
}

}
