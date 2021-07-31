/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibTest/TestCase.h>
#include <unistd.h>

// Regression test for issues #9071
// See: https://github.com/SerenityOS/serenity/issues/9071
TEST_CASE(regression_inifinite_loop)
{
    constexpr auto hour_long_timer_value = Time::from_seconds(60 * 60);

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
