/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/CheckedFormatString.h>
#include <AK/Math.h>
#include <LibTest/CrashTest.h>
#include <LibTest/Randomized/RandomnessSource.h>
#include <LibTest/TestResult.h>

namespace AK {
template<typename... Parameters>
void warnln(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&...);
}

namespace Test {
// Declare helpers so that we can call them from VERIFY in included headers
// the setter for TestResult is already declared in TestResult.h
TestResult current_test_result();

Randomized::RandomnessSource& randomness_source();
void set_randomness_source(Randomized::RandomnessSource);

bool is_reporting_enabled();
void enable_reporting();
void disable_reporting();

u64 randomized_runs();
}

#define EXPECT_EQ(a, b)                                                                                       \
    do {                                                                                                      \
        auto lhs = (a);                                                                                       \
        auto rhs = (b);                                                                                       \
        if (lhs != rhs) {                                                                                     \
            if (::Test::is_reporting_enabled())                                                               \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", \
                    __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, FormatIfSupported { rhs });        \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                      \
        }                                                                                                     \
    } while (false)

#define EXPECT_EQ_TRUTH(a, b)                                                                                                 \
    do {                                                                                                                      \
        auto lhs = (a);                                                                                                       \
        auto rhs = (b);                                                                                                       \
        bool ltruth = static_cast<bool>(lhs);                                                                                 \
        bool rtruth = static_cast<bool>(rhs);                                                                                 \
        if (ltruth != rtruth) {                                                                                               \
            if (::Test::is_reporting_enabled())                                                                               \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ_TRUTH({}, {}) failed with lhs={} ({}) and rhs={} ({})", \
                    __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, ltruth, FormatIfSupported { rhs }, rtruth);        \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                                      \
        }                                                                                                                     \
    } while (false)

// If you're stuck and `EXPECT_EQ` seems to refuse to print anything useful,
// try this: It'll spit out a nice compiler error telling you why it doesn't print.
#define EXPECT_EQ_FORCE(a, b)                                                                                 \
    do {                                                                                                      \
        auto lhs = (a);                                                                                       \
        auto rhs = (b);                                                                                       \
        if (lhs != rhs) {                                                                                     \
            if (::Test::is_reporting_enabled())                                                               \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", \
                    __FILE__, __LINE__, #a, #b, lhs, rhs);                                                    \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                      \
        }                                                                                                     \
    } while (false)

#define EXPECT_NE(a, b)                                                                                       \
    do {                                                                                                      \
        auto lhs = (a);                                                                                       \
        auto rhs = (b);                                                                                       \
        if (lhs == rhs) {                                                                                     \
            if (::Test::is_reporting_enabled())                                                               \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_NE({}, {}) failed with lhs={} and rhs={}", \
                    __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, FormatIfSupported { rhs });        \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                      \
        }                                                                                                     \
    } while (false)

#define EXPECT(x)                                                                                        \
    do {                                                                                                 \
        if (!(x)) {                                                                                      \
            if (::Test::is_reporting_enabled())                                                          \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT({}) failed", __FILE__, __LINE__, #x); \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                 \
        }                                                                                                \
    } while (false)

#define EXPECT_APPROXIMATE_WITH_ERROR(a, b, err)                                                                \
    do {                                                                                                        \
        auto expect_close_lhs = a;                                                                              \
        auto expect_close_rhs = b;                                                                              \
        auto expect_close_diff = static_cast<double>(expect_close_lhs) - static_cast<double>(expect_close_rhs); \
        if (AK::fabs(expect_close_diff) > (err)) {                                                              \
            if (::Test::is_reporting_enabled())                                                                 \
                ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_APPROXIMATE({}, {})"                         \
                             " failed with lhs={}, rhs={}, (lhs-rhs)={}",                                       \
                    __FILE__, __LINE__, #a, #b, expect_close_lhs, expect_close_rhs, expect_close_diff);         \
            ::Test::set_current_test_result(::Test::TestResult::Failed);                                        \
        }                                                                                                       \
    } while (false)

#define EXPECT_APPROXIMATE(a, b) EXPECT_APPROXIMATE_WITH_ERROR(a, b, 0.0000005)

#define REJECT(message)                                                \
    do {                                                               \
        if (::Test::is_reporting_enabled())                            \
            ::AK::warnln("\033[31;1mREJECTED\033[0m: {}:{}: {}",       \
                __FILE__, __LINE__, #message);                         \
        ::Test::set_current_test_result(::Test::TestResult::Rejected); \
    } while (false)

#define ASSUME(x)                                                                                                      \
    do {                                                                                                               \
        if (!(x)) {                                                                                                    \
            if (::Test::is_reporting_enabled())                                                                        \
                ::AK::warnln("\033[31;1mREJECTED\033[0m: {}:{}: Couldn't generate random value satisfying ASSUME({})", \
                    __FILE__, __LINE__, #x);                                                                           \
            ::Test::set_current_test_result(::Test::TestResult::Rejected);                                             \
            return;                                                                                                    \
        }                                                                                                              \
    } while (false)

#define FAIL(message)                                                                      \
    do {                                                                                   \
        if (::Test::is_reporting_enabled())                                                \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: {}", __FILE__, __LINE__, message); \
        ::Test::set_current_test_result(::Test::TestResult::Failed);                       \
    } while (false)

// To use, specify the lambda to execute in a sub process and verify it exits:
//  EXPECT_CRASH("This should fail", []{
//      return Test::Crash::Failure::DidNotCrash;
//  });
#define EXPECT_CRASH(test_message, test_func)                            \
    do {                                                                 \
        Test::Crash crash(test_message, test_func);                      \
        if (!crash.run())                                                \
            ::Test::set_current_test_result(::Test::TestResult::Failed); \
    } while (false)

#define EXPECT_CRASH_WITH_SIGNAL(test_message, signal, test_func)        \
    do {                                                                 \
        Test::Crash crash(test_message, test_func, (signal));            \
        if (!crash.run())                                                \
            ::Test::set_current_test_result(::Test::TestResult::Failed); \
    } while (false)

#define EXPECT_NO_CRASH(test_message, test_func)                         \
    do {                                                                 \
        Test::Crash crash(test_message, test_func, 0);                   \
        if (!crash.run())                                                \
            ::Test::set_current_test_result(::Test::TestResult::Failed); \
    } while (false)

#define TRY_OR_FAIL(expression)                                                                      \
    ({                                                                                               \
        /* Ignore -Wshadow to allow nesting the macro. */                                            \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                             \
            auto&& _temporary_result = (expression));                                                \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        if (_temporary_result.is_error()) [[unlikely]] {                                             \
            FAIL(_temporary_result.release_error());                                                 \
            return;                                                                                  \
        }                                                                                            \
        _temporary_result.release_value();                                                           \
    })
