/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Coroutine.h>
#include <LibCore/EventLoop.h>
#include <LibTest/TestCase.h>

namespace {
Coroutine<int> id(int a)
{
    co_return a;
}

Coroutine<int> sum(int a, int b)
{
    int c = co_await id(a);
    int d = co_await id(b);
    co_return c + d;
}
}

TEST_CASE(no_spin)
{
    auto coro = sum(2, 3);
    EXPECT(coro.await_ready());
    EXPECT_EQ(coro.await_resume(), 5);
}

namespace {
struct LoopSpinner {
    bool await_ready() const { return false; }
    void await_suspend(std::coroutine_handle<> awaiter)
    {
        Core::deferred_invoke([awaiter] {
            awaiter.resume();
        });
    }
    void await_resume() { }
};

Coroutine<int> loop_spinner()
{
    co_await LoopSpinner {};
    co_return 42;
}

Coroutine<ErrorOr<int>> failing_loop_spinner()
{
    co_await LoopSpinner {};
    co_return Error::from_errno(ENOMEM);
}

Coroutine<int> two_level_loop_spinner()
{
    EXPECT_EQ(co_await loop_spinner(), 42);
    co_return 43;
}
}

TEST_CASE(loop_spinners)
{
    EXPECT_EQ(Core::run_async_in_new_event_loop(loop_spinner), 42);
    EXPECT_EQ(Core::run_async_in_new_event_loop(failing_loop_spinner).error().code(), ENOMEM);
    EXPECT_EQ(Core::run_async_in_new_event_loop(two_level_loop_spinner), 43);
}

namespace {
Coroutine<int> spinner1(Vector<int>& result)
{
    result.append(1);
    co_await LoopSpinner {};
    result.append(2);
    co_return 3;
}

Coroutine<int> spinner2(Vector<int>& result)
{
    result.append(4);
    co_await LoopSpinner {};
    result.append(5);
    co_return 6;
}

Coroutine<Vector<int>> interleaved()
{
    Vector<int> result;

    result.append(7);
    auto coro1 = spinner1(result);
    result.append(8);
    auto coro2 = spinner2(result);
    result.append(9);

    result.append(co_await coro2);
    result.append(co_await coro1);

    co_return result;
}
}

TEST_CASE(interleaved_coroutines)
{
    EXPECT_EQ(Core::run_async_in_new_event_loop(interleaved), (Vector { 7, 1, 8, 4, 9, 2, 5, 6, 3 }));
}

namespace {
Coroutine<void> void_coro(int& result)
{
    result = 45;
    co_return;
}
}

TEST_CASE(void_coro)
{
    int result = 0;
    auto coro = void_coro(result);
    EXPECT(coro.await_ready());
    EXPECT_EQ(result, 45);
}

namespace {
Coroutine<void> destructors_inner(Vector<int>& order)
{
    ScopeGuard guard = [&] {
        order.append(1);
    };
    co_await LoopSpinner {};
    order.append(2);
    co_return;
}

Coroutine<Vector<int>> destructors_outer()
{
    Vector<int> order;
    order.append(3);
    co_await destructors_inner(order);
    order.append(4);
    co_return order;
}
}

TEST_CASE(destructors_order)
{
    EXPECT_EQ(Core::run_async_in_new_event_loop(destructors_outer), (Vector { 3, 2, 1, 4 }));
}

namespace {
class Class {
    AK_MAKE_NONCOPYABLE(Class);

public:
    Class()
        : m_cookie(1)
    {
    }

    ~Class()
    {
        VERIFY(m_cookie >= 0);
        m_cookie = -1;
        AK::taint_for_optimizer(m_cookie);
    }

    Class(Class&& other)
    {
        VERIFY(other.m_cookie >= 0);
        m_cookie = exchange(other.m_cookie, 0) + 1;
        AK::taint_for_optimizer(m_cookie);
    }

    Class& operator=(Class&& other) = delete;

    int cookie() { return m_cookie; }

private:
    int m_cookie;
};

Coroutine<Class> return_class_1()
{
    co_await LoopSpinner {};
    co_return {};
}

Coroutine<Class> return_class_2()
{
    co_await LoopSpinner {};
    Class c;
    co_return c;
}

Coroutine<ErrorOr<Class>> return_class_3()
{
    co_await LoopSpinner {};
    co_return Class {};
}

Coroutine<void> move_count()
{
    {
        auto c = co_await return_class_1();
        // 1. Construct temporary as an argument for return_value.
        // 2. Move this temporary into Coroutine.
        // 3. Move class from Coroutine to local variable.
        EXPECT_EQ(c.cookie(), 3);
    }

    {
        auto c = co_await return_class_2();
        // 1. Construct new class and store it as a local variable.
        // 2. Move this temporary into Coroutine.
        // 3. Move class from Coroutine to local variable.
        EXPECT_EQ(c.cookie(), 3);
    }

    {
        auto c_or_error = co_await return_class_3();
        auto c = c_or_error.release_value();
        // 1. Construct temporary as an argument for the constructor of a temporary ErrorOr<Class>.
        // 2. Move temporary ErrorOr<Class> into Coroutine.
        // 3. Move ErrorOr<Class> from Coroutine to c_or_error.
        // 4. Move Class from c_or_error to c.
        EXPECT_EQ(c.cookie(), 4);
    }
}
}

TEST_CASE(move_count)
{
    Core::run_async_in_new_event_loop(move_count);
}

namespace {
Coroutine<ErrorOr<void>> co_try_success()
{
    auto c = CO_TRY(co_await return_class_3());
#ifndef AK_COROUTINE_DESTRUCTION_BROKEN
    // 1. Construct temporary as an argument for the constructor of a temporary ErrorOr<Class>.
    // 2. Move temporary ErrorOr<Class> into Coroutine.
    // -. Some magic is done in TryAwaiter.
    // 3. Move Class from ErrorOr<Class> inside Coroutine to c.
    EXPECT_EQ(c.cookie(), 3);
#else
    EXPECT_EQ(c.cookie(), 4);
#endif
    co_return {};
}

Coroutine<ErrorOr<void>> co_try_fail()
{
    ErrorOr<void> error = Error::from_string_literal("ERROR!");
    CO_TRY(move(error));
    co_return {};
}

Coroutine<ErrorOr<void>> co_try_fail_inner()
{
    co_await LoopSpinner {};
    co_return Error::from_string_literal("ERROR!");
}

Coroutine<ErrorOr<void>> co_try_fail_async()
{
    CO_TRY(co_await co_try_fail_inner());
    co_return {};
}
}

TEST_CASE(co_try)
{
    {
        auto result = Core::run_async_in_new_event_loop(co_try_success);
        EXPECT(!result.is_error());
    }

    {
        auto result = Core::run_async_in_new_event_loop(co_try_fail);
        EXPECT(result.is_error());
    }

    {
        auto result = Core::run_async_in_new_event_loop(co_try_fail_async);
        EXPECT(result.is_error());
    }
}

namespace {
Coroutine<void> nothing() { co_return; }
}

TEST_CASE(move_void_coroutine)
{
    auto void_coro = nothing();
    auto moved = move(void_coro);
    EXPECT(moved.await_ready());
}
