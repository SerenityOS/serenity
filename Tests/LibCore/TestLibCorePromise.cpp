/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/Promise.h>
#include <LibTest/TestSuite.h>

TEST_CASE(promise_await_async_event)
{
    Core::EventLoop loop;

    auto promise = MUST(Core::Promise<int>::try_create());

    loop.deferred_invoke([=] {
        promise->resolve(42);
    });

    auto result = promise->await();
    EXPECT(!result.is_error());
    EXPECT_EQ(result.value(), 42);
}

TEST_CASE(promise_await_async_event_rejection)
{
    Core::EventLoop loop;

    auto promise = MUST(Core::Promise<int>::try_create());

    loop.deferred_invoke([=] {
        promise->reject(AK::Error::from_string_literal("lol no"));
    });

    auto result = promise->await();
    EXPECT(result.is_error());
    EXPECT_EQ(result.error().string_literal(), "lol no"sv);
}

TEST_CASE(promise_chain_handlers)
{
    Core::EventLoop loop;

    bool resolved = false;
    bool rejected = false;

    NonnullRefPtr<Core::Promise<int>> promise = MUST(Core::Promise<int>::try_create())
                                                    ->when_resolved([&](int&) -> ErrorOr<void> { resolved = true; return {}; })
                                                    .when_rejected([&](AK::Error const&) { rejected = true; });

    loop.deferred_invoke([=] {
        promise->resolve(42);
    });

    (void)promise->await();
    EXPECT(resolved);
    EXPECT(!rejected);
}
