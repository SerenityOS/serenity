/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h> // intentionally first -- we redefine VERIFY and friends in here

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

namespace Test {

using TestFunction = Function<void()>;

class TestCase : public RefCounted<TestCase> {
public:
    TestCase(const String& name, TestFunction&& fn, bool is_benchmark)
        : m_name(name)
        , m_function(move(fn))
        , m_is_benchmark(is_benchmark)
    {
    }

    bool is_benchmark() const { return m_is_benchmark; }
    const String& name() const { return m_name; }
    const TestFunction& func() const { return m_function; }

private:
    String m_name;
    TestFunction m_function;
    bool m_is_benchmark;
};

// Helper to hide implementation of TestSuite from users
void add_test_case_to_suite(const NonnullRefPtr<TestCase>& test_case);
void set_suite_setup_function(Function<void()> setup);
}

#define TEST_SETUP                                                  \
    static void __setup();                                          \
    struct __setup_type {                                           \
        __setup_type() { Test::set_suite_setup_function(__setup); } \
    };                                                              \
    static struct __setup_type __setup_type;                        \
    static void __setup()

#define __TESTCASE_FUNC(x) __test_##x
#define __TESTCASE_TYPE(x) __TestCase_##x

#define TEST_CASE(x)                                                                                    \
    static void __TESTCASE_FUNC(x)();                                                                   \
    struct __TESTCASE_TYPE(x) {                                                                         \
        __TESTCASE_TYPE(x)                                                                              \
        () { add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(#x, __TESTCASE_FUNC(x), false))); } \
    };                                                                                                  \
    static struct __TESTCASE_TYPE(x) __TESTCASE_TYPE(x);                                                \
    static void __TESTCASE_FUNC(x)()

#define __BENCHMARK_FUNC(x) __benchmark_##x
#define __BENCHMARK_TYPE(x) __BenchmarkCase_##x

#define BENCHMARK_CASE(x)                                                                               \
    static void __BENCHMARK_FUNC(x)();                                                                  \
    struct __BENCHMARK_TYPE(x) {                                                                        \
        __BENCHMARK_TYPE(x)                                                                             \
        () { add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(#x, __BENCHMARK_FUNC(x), true))); } \
    };                                                                                                  \
    static struct __BENCHMARK_TYPE(x) __BENCHMARK_TYPE(x);                                              \
    static void __BENCHMARK_FUNC(x)()
