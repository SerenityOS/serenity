/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <LibCore/EventLoop.h>
#include <LibTest/TestCase.h>

#ifndef AK_COROUTINE_STATEMENT_EXPRS_BROKEN
#    define CO_TRY_OR_FAIL(expression)                                                                   \
        ({                                                                                               \
            /* Ignore -Wshadow to allow nesting the macro. */                                            \
            AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                             \
                auto _temporary_result = (expression));                                                  \
            static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
                "Do not return a reference from a fallible expression");                                 \
            if (_temporary_result.is_error()) [[unlikely]] {                                             \
                FAIL(_temporary_result.release_error());                                                 \
                co_return;                                                                               \
            }                                                                                            \
            _temporary_result.release_value();                                                           \
        })
#elifndef AK_COROUTINE_DESTRUCTION_BROKEN
namespace AK::Detail::Test {

// FIXME: Straight-forward way to implement CO_TRY_OR_FAIL we use for Clang causes GCC to ICE.
template<typename T>
struct TryOrFailAwaiter {
    TryOrFailAwaiter(T& expression)
        : m_expression(&expression)
    {
    }

    TryOrFailAwaiter(T&& expression)
        : m_expression(&expression)
    {
    }

    TryOrFailAwaiter(Coroutine<T>&&)
    {
        static_assert(DependentFalse<T>, "Despite the CO_ prefix in CO_TRY_OR_FAIL, you should co_await the argument of it.");
    }

    bool await_ready() { return false; }

    template<typename U>
    requires IsSpecializationOf<T, ErrorOr>
    std::coroutine_handle<> await_suspend(std::coroutine_handle<U> handle)
    {
        if (!m_expression->is_error()) {
            return handle;
        } else {
            auto awaiter = handle.promise().m_awaiter;
            auto* coroutine = handle.promise().m_coroutine;
            using ReturnType = RemoveReference<decltype(*coroutine)>::ReturnType;
            static_assert(SameAs<ReturnType, void>,
                "CO_TRY_OR_FAIL can only be used inside ASYNC_TEST_CASE functions");

            // Fail the current test.
            FAIL(m_expression->release_error());

            // Forcefully stop coroutine.
            coroutine->m_handle = {};

            // Run destructors for locals in the coroutine that failed.
            handle.destroy();

            // Lastly, transfer control to the parent (or nothing, if parent is not yet suspended).
            if (awaiter)
                return awaiter;
            return std::noop_coroutine();
        }
    }

    decltype(auto) await_resume()
    {
        return m_expression->release_value();
    }

    T* m_expression { nullptr };
};

}

#    define CO_TRY_OR_FAIL(expression) (co_await ::AK::Detail::Test::TryOrFailAwaiter { (expression) })
#else
#    error Unable to work around compiler bugs in definiton of CO_TRY_OR_FAIL.
#endif

namespace Test {

#define __ASYNC_TESTCASE_FUNC(x) __async_test_##x

#define ASYNC_TEST_CASE(x)                                                                           \
    static Coroutine<void> __ASYNC_TESTCASE_FUNC(x)();                                               \
                                                                                                     \
    static void __TESTCASE_FUNC(x)()                                                                 \
    {                                                                                                \
        Core::run_async_in_new_event_loop(__ASYNC_TESTCASE_FUNC(x));                                 \
    }                                                                                                \
                                                                                                     \
    struct __TESTCASE_TYPE(x) {                                                                      \
        __TESTCASE_TYPE(x)                                                                           \
        ()                                                                                           \
        {                                                                                            \
            add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(#x, __TESTCASE_FUNC(x), false))); \
        }                                                                                            \
    };                                                                                               \
    static struct __TESTCASE_TYPE(x) __TESTCASE_TYPE(x);                                             \
    static Coroutine<void> __ASYNC_TESTCASE_FUNC(x)()

}
