/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibTest/TestCase.h>

TEST_CASE(event_loop_error_handling)
{
    Core::EventLoop event_loop;

    auto some_callback_which_might_error = []() -> ErrorOr<void> {
        return Error::from_string_literal("Oh noes!"sv);
    };

    Core::deferred_invoke([&] {
        Core::EventLoop::current().try_callback(some_callback_which_might_error());
    });

    auto reaper = Core::Timer::create_single_shot(250, [] {
        warnln("I waited for the event loop to exit with an error, but it never did!");
        VERIFY_NOT_REACHED();
    });

    auto result = event_loop.exec();
    EXPECT(result.is_error());
    EXPECT_EQ(result.error().string_literal(), "Oh noes!"sv);
}
