/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Time.h>
#include <LibCore/ElapsedTimer.h>
#include <LibTest/TestCase.h>
#include <signal.h>
#include <unistd.h>

class SuccessContext {
public:
    static Atomic<bool> alarm_fired;

    static Core::ElapsedTimer signal_timer;

    static constexpr auto timer_value = Duration::from_seconds(1);

    static void test_signal_handler(int signal)
    {
        auto actual_duration = SuccessContext::signal_timer.elapsed_time();
        auto expected_duration = SuccessContext::timer_value;

        // Add a small buffer to allow for latency on the system.
        constexpr auto buffer_duration = Duration::from_milliseconds(50);

        dbgln("Signal Times - Actual: {} Expected: {}", actual_duration.to_milliseconds(), expected_duration.to_milliseconds());
        EXPECT(actual_duration >= expected_duration);
        EXPECT(actual_duration < expected_duration + buffer_duration);

        EXPECT_EQ(signal, SIGALRM);
        SuccessContext::alarm_fired = true;
    }
};

Atomic<bool> SuccessContext::alarm_fired { false };
Core::ElapsedTimer SuccessContext::signal_timer {};

TEST_CASE(success_case)
{
    signal(SIGALRM, SuccessContext::test_signal_handler);

    SuccessContext::signal_timer.start();
    auto previous_time = alarm(SuccessContext::timer_value.to_seconds());
    EXPECT_EQ(previous_time, 0u);

    auto sleep_time = SuccessContext::timer_value + Duration::from_seconds(1);
    sleep(sleep_time.to_seconds());

    EXPECT(SuccessContext::alarm_fired);
}

// Regression test for issues #9071
// See: https://github.com/SerenityOS/serenity/issues/9071
TEST_CASE(regression_inifinite_loop)
{
    constexpr auto hour_long_timer_value = Duration::from_seconds(60 * 60);

    // Create an alarm timer significantly far into the future.
    auto previous_time = alarm(hour_long_timer_value.to_seconds());
    EXPECT_EQ(previous_time, 0u);

    // Update the alarm with a zero value before the previous timer expires.
    previous_time = alarm(0);
    EXPECT_EQ(previous_time, hour_long_timer_value.to_seconds());

    // Update the alarm with a zero value again, this shouldn't get stuck
    // in an infinite loop trying to cancel the previous timer in the kernel.
    previous_time = alarm(0);
    EXPECT_EQ(previous_time, 0u);
}
