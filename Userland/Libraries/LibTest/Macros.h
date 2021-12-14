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

namespace AK {
template<typename... Parameters>
void warnln(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&...);
}

namespace Test {
// Declare a helper so that we can call it from VERIFY in included headers
void current_test_case_did_fail();
}

#undef VERIFY
#define VERIFY(x)                                                                                    \
    do {                                                                                             \
        if (!(x)) {                                                                                  \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: VERIFY({}) failed", __FILE__, __LINE__, #x); \
            ::Test::current_test_case_did_fail();                                                    \
        }                                                                                            \
    } while (false)

#undef VERIFY_NOT_REACHED
#define VERIFY_NOT_REACHED()                                                                           \
    do {                                                                                               \
        ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: VERIFY_NOT_REACHED() called", __FILE__, __LINE__); \
        ::abort();                                                                                     \
    } while (false)

#undef TODO
#define TODO()                                                                           \
    do {                                                                                 \
        ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: TODO() called", __FILE__, __LINE__); \
        ::abort();                                                                       \
    } while (false)

#define EXPECT_EQ(a, b)                                                                                                                                                                      \
    do {                                                                                                                                                                                     \
        auto lhs = (a);                                                                                                                                                                      \
        auto rhs = (b);                                                                                                                                                                      \
        if (lhs != rhs) {                                                                                                                                                                    \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, FormatIfSupported { rhs }); \
            ::Test::current_test_case_did_fail();                                                                                                                                            \
        }                                                                                                                                                                                    \
    } while (false)

#define EXPECT_EQ_TRUTH(a, b)                                                                                             \
    do {                                                                                                                  \
        auto lhs = (a);                                                                                                   \
        auto rhs = (b);                                                                                                   \
        bool ltruth = static_cast<bool>(lhs);                                                                             \
        bool rtruth = static_cast<bool>(rhs);                                                                             \
        if (ltruth != rtruth) {                                                                                           \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ_TRUTH({}, {}) failed with lhs={} ({}) and rhs={} ({})", \
                __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, ltruth, FormatIfSupported { rhs }, rtruth);        \
            ::Test::current_test_case_did_fail();                                                                         \
        }                                                                                                                 \
    } while (false)

// If you're stuck and `EXPECT_EQ` seems to refuse to print anything useful,
// try this: It'll spit out a nice compiler error telling you why it doesn't print.
#define EXPECT_EQ_FORCE(a, b)                                                                                                                    \
    do {                                                                                                                                         \
        auto lhs = (a);                                                                                                                          \
        auto rhs = (b);                                                                                                                          \
        if (lhs != rhs) {                                                                                                                        \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", __FILE__, __LINE__, #a, #b, lhs, rhs); \
            ::Test::current_test_case_did_fail();                                                                                                \
        }                                                                                                                                        \
    } while (false)

#define EXPECT_NE(a, b)                                                                                                                                                                      \
    do {                                                                                                                                                                                     \
        auto lhs = (a);                                                                                                                                                                      \
        auto rhs = (b);                                                                                                                                                                      \
        if (lhs == rhs) {                                                                                                                                                                    \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_NE({}, {}) failed with lhs={} and rhs={}", __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, FormatIfSupported { rhs }); \
            ::Test::current_test_case_did_fail();                                                                                                                                            \
        }                                                                                                                                                                                    \
    } while (false)

#define EXPECT(x)                                                                                    \
    do {                                                                                             \
        if (!(x)) {                                                                                  \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT({}) failed", __FILE__, __LINE__, #x); \
            ::Test::current_test_case_did_fail();                                                    \
        }                                                                                            \
    } while (false)

#define EXPECT_APPROXIMATE(a, b)                                                                                \
    do {                                                                                                        \
        auto expect_close_lhs = a;                                                                              \
        auto expect_close_rhs = b;                                                                              \
        auto expect_close_diff = static_cast<double>(expect_close_lhs) - static_cast<double>(expect_close_rhs); \
        if (AK::fabs(expect_close_diff) > 0.0000005) {                                                          \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_APPROXIMATE({}, {})"                             \
                         " failed with lhs={}, rhs={}, (lhs-rhs)={}",                                           \
                __FILE__, __LINE__, #a, #b, expect_close_lhs, expect_close_rhs, expect_close_diff);             \
            ::Test::current_test_case_did_fail();                                                               \
        }                                                                                                       \
    } while (false)

#define FAIL(message)                                                                  \
    do {                                                                               \
        ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: {}", __FILE__, __LINE__, message); \
        ::Test::current_test_case_did_fail();                                          \
    } while (false)

// To use, specify the lambda to execute in a sub process and verify it exits:
//  EXPECT_CRASH("This should fail", []{
//      return Test::Crash::Failure::DidNotCrash;
//  });
#define EXPECT_CRASH(test_message, test_func)       \
    do {                                            \
        Test::Crash crash(test_message, test_func); \
        if (!crash.run())                           \
            ::Test::current_test_case_did_fail();   \
    } while (false)

#define EXPECT_CRASH_WITH_SIGNAL(test_message, signal, test_func) \
    do {                                                          \
        Test::Crash crash(test_message, test_func, (signal));     \
        if (!crash.run())                                         \
            ::Test::current_test_case_did_fail();                 \
    } while (false)

#define EXPECT_NO_CRASH(test_message, test_func)       \
    do {                                               \
        Test::Crash crash(test_message, test_func, 0); \
        if (!crash.run())                              \
            ::Test::current_test_case_did_fail();      \
    } while (false)
