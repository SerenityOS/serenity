/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h> // intentionally first -- we redefine VERIFY and friends in here
#include <LibTest/PBT/GenResult.h>
#include <LibTest/PBT/Generator.h>
#include <LibTest/PBT/RandSource.h>
#include <LibTest/PBT/Shrink.h>

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/QuickSort.h>
#include <AK/RefCounted.h>
#include <AK/Tuple.h>
#include <AK/Vector.h>

#ifndef MAX_GENERATED_VALUES_PER_TEST
#    define MAX_GENERATED_VALUES_PER_TEST 100
#endif

#ifndef MAX_GEN_ATTEMPTS_PER_VALUE
#    define MAX_GEN_ATTEMPTS_PER_VALUE 15
#endif

namespace Test {

using TestFunction = Function<void()>;

inline void run_with_rand_source(RandSource source, TestFunction const& test_fn)
{
    set_rand_source(move(source));
    test_fn();
    if (current_test_result() == TestResult::NotRun) {
        set_current_test_result(TestResult::Passed);
    }
}

class TestCase : public RefCounted<TestCase> {
public:
    TestCase(DeprecatedString const& name, TestFunction&& fn, bool is_benchmark)
        : m_name(name)
        , m_function(move(fn))
        , m_is_benchmark(is_benchmark)
    {
    }

    TestCase(DeprecatedString const& name, TestFunction& fn, bool is_benchmark)
        : m_name(name)
        , m_function(move(fn))
        , m_is_benchmark(is_benchmark)
    {
    }

    bool is_benchmark() const { return m_is_benchmark; }
    DeprecatedString const& name() const { return m_name; }
    TestFunction const& func() const { return m_function; }

    static NonnullRefPtr<TestCase> randomized(DeprecatedString const& name, TestFunction&& test_fn)
    {
        TestFunction test_case_fn = [test_fn = move(test_fn)]() {
            for (u32 i = 0; i < MAX_GENERATED_VALUES_PER_TEST; ++i) {
                bool generated_successfully = false;
                u8 gen_attempt;
                for (gen_attempt = 0; gen_attempt < MAX_GEN_ATTEMPTS_PER_VALUE && !generated_successfully; ++gen_attempt) {
                    // We're going to run the test function many times, so let's turn off the reporting until we finish.
                    disable_reporting();

                    set_current_test_result(TestResult::NotRun);
                    run_with_rand_source(RandSource::live(), test_fn);
                    switch (current_test_result()) {
                    case TestResult::NotRun:
                        break; // TODO I'd like to use VERIFY_NOT_REACHED() here
                    case TestResult::Passed: {
                        generated_successfully = true;
                        break;
                    }
                    case TestResult::Failed: {
                        generated_successfully = true;
                        RandomRun first_failure = rand_source().run();
                        RandomRun best_failure = shrink(first_failure, test_fn);
                        // Run one last time with reporting on, so that the user can see the minimal failure
                        enable_reporting();
                        run_with_rand_source(RandSource::recorded(best_failure), test_fn);
                        return;
                    }
                    case TestResult::Rejected:
                        break;
                    case TestResult::HitLimit:
                        break;
                    case TestResult::Overrun:
                        break;
                    default:
                        break; // TODO I'd like to use VERIFY_NOT_REACHED() here
                    }
                }
                if (!generated_successfully) {
                    // TODO I'd like to do: VERIFY(gen_attempt == MAX_GEN_ATTEMPTS_PER_VALUE);
                    // Meaning the loop above got to the full MAX_GEN_ATTEMPTS_PER_VALUE and gave up
                    // Run one last time with reporting on, so that the user gets the REJECTED message.
                    enable_reporting();
                    RandomRun last_failure = rand_source().run();
                    run_with_rand_source(RandSource::recorded(last_failure), test_fn);
                    return;
                }
            }
            // MAX_GENERATED_VALUES_PER_TEST values generated, all passed the test.
            // TODO I'd like to do: VERIFY(current_test_result() == TestResult::Passed);
        };
        return make_ref_counted<TestCase>(name, test_case_fn, false);
    }

private:
    DeprecatedString m_name;
    TestFunction m_function;
    bool m_is_benchmark;
};

// Helper to hide implementation of TestSuite from users
void add_test_case_to_suite(NonnullRefPtr<TestCase> const& test_case);
void set_suite_setup_function(Function<void()> setup);
}

#define TEST_SETUP                                   \
    static void __setup();                           \
    struct __setup_type {                            \
        __setup_type()                               \
        {                                            \
            Test::set_suite_setup_function(__setup); \
        }                                            \
    };                                               \
    static struct __setup_type __setup_type;         \
    static void __setup()

// Unit test

#define __TESTCASE_FUNC(x) __test_##x
#define __TESTCASE_TYPE(x) __TestCase_##x

#define TEST_CASE(x)                                                                                 \
    static void __TESTCASE_FUNC(x)();                                                                \
    struct __TESTCASE_TYPE(x) {                                                                      \
        __TESTCASE_TYPE(x)                                                                           \
        ()                                                                                           \
        {                                                                                            \
            add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(#x, __TESTCASE_FUNC(x), false))); \
        }                                                                                            \
    };                                                                                               \
    static struct __TESTCASE_TYPE(x) __TESTCASE_TYPE(x);                                             \
    static void __TESTCASE_FUNC(x)()

// Benchmark

#define __BENCHMARK_FUNC(x) __benchmark_##x
#define __BENCHMARK_TYPE(x) __BenchmarkCase_##x

#define BENCHMARK_CASE(x)                                                                            \
    static void __BENCHMARK_FUNC(x)();                                                               \
    struct __BENCHMARK_TYPE(x) {                                                                     \
        __BENCHMARK_TYPE(x)                                                                          \
        ()                                                                                           \
        {                                                                                            \
            add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(#x, __BENCHMARK_FUNC(x), true))); \
        }                                                                                            \
    };                                                                                               \
    static struct __BENCHMARK_TYPE(x) __BENCHMARK_TYPE(x);                                           \
    static void __BENCHMARK_FUNC(x)()

// Randomized test

#define __RANDOMIZED_TEST_FUNC(x) __randomized_test_##x
#define __RANDOMIZED_TEST_TYPE(x) __RandomizedTestCase_##x

#define RANDOMIZED_TEST_CASE(x)                                                                  \
    static void __RANDOMIZED_TEST_FUNC(x)();                                                     \
    struct __RANDOMIZED_TEST_TYPE(x) {                                                           \
        __RANDOMIZED_TEST_TYPE(x)                                                                \
        ()                                                                                       \
        {                                                                                        \
            add_test_case_to_suite(::Test::TestCase::randomized(#x, __RANDOMIZED_TEST_FUNC(x))); \
        }                                                                                        \
    };                                                                                           \
    static struct __RANDOMIZED_TEST_TYPE(x) __RANDOMIZED_TEST_TYPE(x);                           \
    static void __RANDOMIZED_TEST_FUNC(x)()

#define GEN(identifier, value)  \
    auto(identifier) = (value); \
    if (::Test::can_report())   \
    ::AK::warnln("{} = {}", #identifier, (identifier))
