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

private:
    DeprecatedString m_name;
    TestFunction m_function;
    bool m_is_benchmark;
};

inline void print_rejections(HashMap<StringView, u32> const& rejections)
{
    warnln("Couldn't generate a test value. Reasons:");

    Vector<Tuple<StringView, u32>> vec;

    for (auto tuple : rejections) {
        warnln(" - {} ({}x)", tuple.key, tuple.value);
    }
}

// TODO can this be made into static method of TestCase without forcing <T> on TestCase?
template<typename T>
static NonnullRefPtr<TestCase> property_based_test_case(DeprecatedString const& name, void (*test_fn)(T), Generator<T>&& generator)
{
    TestFunction test_case_fn = [test_fn, generator = move(generator)]() {
        // TODO disable the fail reporting while we try to run the test fn multiple times
        for (u32 i = 0; i < MAX_GENERATED_VALUES_PER_TEST; ++i) {
            HashMap<StringView, u32> rejections;
            bool generated_successfully = false;
            for (u8 gen_attempt = 0; gen_attempt < MAX_GEN_ATTEMPTS_PER_VALUE && !generated_successfully; ++gen_attempt) {
                auto source = RandSource::live();
                auto gen_result = generator(source);
                if (gen_result.is_generated()) {
                    Generated<T> g = gen_result.get_generated();
                    generated_successfully = true;
                    test_fn(g.value);
                    if (!did_current_test_case_pass()) {
                        Generated<T> best_failure = shrink(g, generator, test_fn);
                        // TODO put the fail reporting back
                        warnln("Test failed for value: {}", best_failure.value);
                        return;
                    }
                } else {
                    Rejected r = gen_result.get_rejected();
                    u32 count = rejections.get(r.reason).value_or(0);
                    rejections.set(r.reason, count + 1);
                }
            }
            if (!generated_successfully) {
                // The loop above got to the full MAX_GEN_ATTEMPTS_PER_VALUE and gave up
                print_rejections(rejections);

                // We couldn't generate a value.
                // TODO: Maybe we should add a third test result kind as this is
                // not technically a test failure?
                current_test_case_did_fail();
                return;
            }
        }
        // MAX_GENERATED_VALUES_PER_TEST values generated, all passed the test.
        current_test_case_did_pass();
    };
    return make_ref_counted<TestCase>(name, test_case_fn, false);
}

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

// Property based test

#define __PBTEST_FUNC(name) __pbtest_##name
#define __PBTEST_TYPE(name) __PbtestCase_##name

#define PBTEST_CASE(name, generator, identifier)                                                               \
    template<typename T>                                                                                       \
    static void __PBTEST_FUNC(name)(T(identifier));                                                            \
    struct __PBTEST_TYPE(name) {                                                                               \
        __PBTEST_TYPE(name)                                                                                    \
        ()                                                                                                     \
        {                                                                                                      \
            add_test_case_to_suite(::Test::property_based_test_case(#name, __PBTEST_FUNC(name), (generator))); \
        }                                                                                                      \
    };                                                                                                         \
    static struct __PBTEST_TYPE(name) __PBTEST_TYPE(name);                                                     \
    template<typename T>                                                                                       \
    static void __PBTEST_FUNC(name)(T(identifier))
