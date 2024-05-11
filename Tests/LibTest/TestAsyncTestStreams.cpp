/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/AsyncTestCase.h>
#include <LibTest/AsyncTestStreams.h>

ASYNC_TEST_CASE(input_basic)
{
    Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 1, 2, 2 });

    auto first_letter_coro = stream.read(1);
    EXPECT(first_letter_coro.await_ready());

    auto first_letter_view = CO_TRY_OR_FAIL(co_await first_letter_coro);
    EXPECT_EQ(StringView { first_letter_view }, "h"sv);

    auto next_letters_coro = stream.read(3);
    EXPECT(!next_letters_coro.await_ready());

    auto next_letters_view = CO_TRY_OR_FAIL(co_await next_letters_coro);
    EXPECT_EQ(StringView { next_letters_view }, "ell"sv);

    auto last_letter_coro = stream.peek_or_eof();
    EXPECT(last_letter_coro.await_ready());

    auto [last_letter_view, is_eof_1] = CO_TRY_OR_FAIL(co_await last_letter_coro);
    EXPECT_EQ(StringView { last_letter_view }, "o"sv);
    EXPECT(!is_eof_1);

    auto o_again = CO_TRY_OR_FAIL(co_await stream.read(1));
    EXPECT_EQ(StringView { o_again }, "o"sv);

    auto [empty_view, is_eof_2] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT(empty_view.is_empty());
    EXPECT(is_eof_2);

    CO_TRY_OR_FAIL(co_await stream.close());
}

ASYNC_TEST_CASE(input_eof_read)
{
    Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 5 });

    auto [hello, is_eof_1] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT_EQ(StringView { hello }, "hello"sv);
    EXPECT(!is_eof_1);

    auto [also_hello, is_eof_2] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT_EQ(StringView { also_hello }, "hello"sv);
    EXPECT(is_eof_2);

    auto [hello_one_more_time, is_eof_3] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT_EQ(StringView { hello_one_more_time }, "hello"sv);
    EXPECT(is_eof_3);

    auto hello_for_the_final_time = CO_TRY_OR_FAIL(co_await stream.read(5));
    EXPECT_EQ(StringView { hello_for_the_final_time }, "hello"sv);

    CO_TRY_OR_FAIL(co_await stream.close());
}

ASYNC_TEST_CASE(input_eof_read_2)
{
    Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 0, 5 });

    auto [hello, is_eof_1] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT_EQ(StringView { hello }, "hello"sv);
    EXPECT(!is_eof_1);

    auto hello_again = must_sync(stream.read(5));
    EXPECT_EQ(StringView { hello_again }, "hello"sv);

    auto [not_hello, is_eof_2] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
    EXPECT(not_hello.is_empty());
    EXPECT(is_eof_2);

    must_sync(stream.read(0));

    for (int i = 0; i < 2; ++i) {
        auto [not_hello_2, is_eof_3] = CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
        EXPECT(not_hello_2.is_empty());
        EXPECT(is_eof_3);
    }

    CO_TRY_OR_FAIL(co_await stream.close());
}

// FIXME: Maybe add some kind of intentionally failing tests?
ASYNC_TEST_CASE(input_unexpected_operations)
{
    {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 5 });
    }
    VERIFY(Test::current_test_result() == Test::TestResult::Failed);
    Test::set_current_test_result(Test::TestResult::NotRun);

    {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 5 });
    }
    VERIFY(Test::current_test_result() == Test::TestResult::NotRun);

    {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 1, 1, 1, 1, 1 });
        auto view = CO_TRY_OR_FAIL(co_await stream.read(5));
        EXPECT_EQ(StringView { view }, "hello"sv);
        CO_TRY_OR_FAIL(co_await stream.close());
    }
    VERIFY(Test::current_test_result() == Test::TestResult::NotRun);

    {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Close, { 1, 1, 1, 1, 1 });
        (void)co_await stream.close();
    }
    VERIFY(Test::current_test_result() == Test::TestResult::Failed);
    Test::set_current_test_result(Test::TestResult::NotRun);

    {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 1, 1, 1, 1, 1 });
        stream.reset();
    }
    VERIFY(Test::current_test_result() == Test::TestResult::NotRun);
}

ASYNC_TEST_CASE(input_reset_during_wait)
{
    Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 0, 5 });

    auto read_coro = stream.read(5);
    EXPECT(!read_coro.await_ready());

    stream.reset();

    auto error = co_await read_coro;
    EXPECT_EQ(error.error().code(), ECANCELED);
}

TEST_CASE(input_crash)
{
    EXPECT_CRASH("input_concurrent_reads", [] -> Test::Crash::Failure {
        Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 0, 5 });

        auto read_coro1 = stream.read(2);
        if (read_coro1.await_ready()) {
            // NOTE: Intentionally don't run destructors.
            _exit(0);
        }

        auto read_coro2 = stream.read(3);
        _exit(0);
    });

    EXPECT_CRASH("input_peek_read_condition_violation", [] {
        auto coro = [] -> Coroutine<void> {
            Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 2, 1, 1, 1 });

            auto peek_view_1 = CO_TRY_OR_FAIL(co_await stream.peek());
            if (peek_view_1.size() != 2)
                co_return;

            auto peek_view_2 = CO_TRY_OR_FAIL(co_await stream.peek());
            if (peek_view_2.size() != 3)
                co_return;

            auto peek_view_3 = CO_TRY_OR_FAIL(co_await stream.peek());
            if (peek_view_3.size() != 4)
                co_return;

            (void)co_await stream.read(2);
        };
        Core::run_async_in_new_event_loop(coro);
        return Test::Crash::Failure::DidNotCrash;
    });

    EXPECT_CRASH("one_less_than_eof", ([] {
        auto coro = [] -> Coroutine<void> {
            Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 5 });

            CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
            CO_TRY_OR_FAIL(co_await stream.peek_or_eof());
            CO_TRY_OR_FAIL(co_await stream.read(4));
            _exit(0);
        };
        Core::run_async_in_new_event_loop(coro);
        return Test::Crash::Failure::DidNotCrash;
    }));
}

ASYNC_TEST_CASE(input_close_ebusy)
{
    Test::AsyncMemoryInputStream stream("hello"sv, Test::StreamCloseExpectation::Reset, { 5 });
    auto error = co_await stream.close();
    EXPECT_EQ(error.error().code(), EBUSY);
}

ASYNC_TEST_CASE(output_basic)
{
    Test::AsyncMemoryOutputStream stream(Test::StreamCloseExpectation::Close);

    CO_TRY_OR_FAIL(co_await stream.write({ {
        "Consider a non-trivial loop $\\alpha$ in $\\R P^2$ "sv.bytes(),
        "and $f \\circ \\alpha$. $[f \\circ \\alpha]$ maps to some integer "sv.bytes(),
    } }));

    size_t nwritten = CO_TRY_OR_FAIL(co_await stream.write_some("$n$ from a fundamental group $\\pi_1(S^1)$."sv.bytes()));
    EXPECT_EQ(nwritten, 42uz);

    auto sentence = "Consider a non-trivial loop $\\alpha$ in $\\R P^2$ and $f \\circ \\alpha$. $[f \\circ \\alpha]$ maps to some integer $n$ from a fundamental group $\\pi_1(S^1)$."sv;
    EXPECT_EQ(sentence, StringView { stream.view() });

    CO_TRY_OR_FAIL(co_await stream.close());
}
