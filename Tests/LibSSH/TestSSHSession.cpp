/*
 * Copyright (c) 2026, SerenityOS contributors
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibSSH/Session.h>
#include <LibTest/TestCase.h>

TEST_CASE(receive_window_is_enforced_without_unsigned_underflow)
{
    auto session = TRY_OR_FAIL(SSH::Session::create(0, 8, 32768));

    EXPECT(session->can_receive_data(8));
    EXPECT(!session->can_receive_data(9));

    session->total_received_bytes = 7;
    EXPECT(session->can_receive_data(1));
    EXPECT(!session->can_receive_data(2));

    // A peer that has already exceeded the advertised window must not turn
    // the subtraction in the receive-window check into a large unsigned value.
    session->total_received_bytes = 9;
    EXPECT(!session->can_receive_data(0));
}

TEST_CASE(peer_window_credit_is_tracked_and_bounded)
{
    auto session = TRY_OR_FAIL(SSH::Session::create(0, 8, 32768));
    session->remote_window_size = 8;

    session->consume_remote_window(3);
    EXPECT_EQ(session->remote_window_size, 5u);
    TRY_OR_FAIL(session->adjust_remote_window(3));
    EXPECT_EQ(session->remote_window_size, 8u);

    session->remote_window_size = NumericLimits<u32>::max();
    EXPECT(session->adjust_remote_window(1).is_error());
}

TEST_CASE(waiting_for_peer_window_resumes_after_adjustment)
{
    auto session = TRY_OR_FAIL(SSH::Session::create(0, 8, 32768));
    bool resumed = false;

    auto wait_for_credit = [&]() -> Coroutine<ErrorOr<void>> {
        Core::EventLoop::current().deferred_invoke([&] {
            session->remote_window_size = 1;
            session->notify_remote_window_waiters();
        });
        CO_TRY(co_await session->wait_for_remote_window());
        resumed = true;
        co_return {};
    };

    TRY_OR_FAIL(Core::run_async_in_new_event_loop(wait_for_credit));
    EXPECT(resumed);
}

TEST_CASE(consuming_buffered_data_is_accounted)
{
    auto session = TRY_OR_FAIL(SSH::Session::create(0, 8, 32768));
    session->channel_data.append("abcdefgh"sv.bytes());
    session->total_received_bytes = 8;

    session->dequeue_channel_data(3);

    EXPECT_EQ(session->channel_data.data().size(), 5u);
    EXPECT_EQ(session->total_consumed_bytes, 3u);
}
