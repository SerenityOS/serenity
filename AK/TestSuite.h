/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/CheckedFormatString.h>

namespace AK {

template<typename... Parameters>
void warnln(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&...);

// Declare a helper so that we can call it from VERIFY in included headers
// before defining TestSuite
inline void current_test_case_did_fail();

}

using AK::warnln;

#undef VERIFY
#define VERIFY(x)                                                                                    \
    do {                                                                                             \
        if (!(x)) {                                                                                  \
            ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: VERIFY({}) failed", __FILE__, __LINE__, #x); \
            current_test_case_did_fail();                                                            \
        }                                                                                            \
    } while (false)

#undef VERIFY_NOT_REACHED
#define VERIFY_NOT_REACHED()                                                                           \
    do {                                                                                               \
        ::AK::warnln("\033[31;1mFAIL\033[0m: {}:{}: VERIFY_NOT_REACHED() called", __FILE__, __LINE__); \
        ::abort();                                                                                     \
    } while (false)

#undef TODO
#define TODO()                                                                                   \
    do {                                                                                         \
        ::AK::warnln(stderr, "\033[31;1mFAIL\033[0m: {}:{}: TODO() called", __FILE__, __LINE__); \
        ::abort();                                                                               \
    } while (false)

#include <AK/Format.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <stdlib.h>
#include <sys/time.h>

namespace AK {

class TestElapsedTimer {
public:
    TestElapsedTimer() { restart(); }

    void restart() { gettimeofday(&m_started, nullptr); }

    u64 elapsed_milliseconds()
    {
        struct timeval now;
        gettimeofday(&now, nullptr);

        struct timeval delta;
        timersub(&now, &m_started, &delta);

        return delta.tv_sec * 1000 + delta.tv_usec / 1000;
    }

private:
    struct timeval m_started;
};

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

class TestSuite {
public:
    static TestSuite& the()
    {
        if (s_global == nullptr)
            s_global = new TestSuite();
        return *s_global;
    }

    static void release()
    {
        if (s_global)
            delete s_global;
        s_global = nullptr;
    }

    int run(const NonnullRefPtrVector<TestCase>&);
    int main(const String& suite_name, int argc, char** argv);
    NonnullRefPtrVector<TestCase> find_cases(const String& search, bool find_tests, bool find_benchmarks);
    void add_case(const NonnullRefPtr<TestCase>& test_case)
    {
        m_cases.append(test_case);
    }

    void current_test_case_did_fail() { m_current_test_case_passed = false; }

private:
    static TestSuite* s_global;
    NonnullRefPtrVector<TestCase> m_cases;
    u64 m_testtime = 0;
    u64 m_benchtime = 0;
    String m_suite_name;
    bool m_current_test_case_passed = true;
};

inline void current_test_case_did_fail() { TestSuite::the().current_test_case_did_fail(); }

int TestSuite::main(const String& suite_name, int argc, char** argv)
{
    m_suite_name = suite_name;

    Core::ArgsParser args_parser;

    bool do_tests_only = false;
    bool do_benchmarks_only = false;
    bool do_list_cases = false;
    const char* search_string = "*";

    args_parser.add_option(do_tests_only, "Only run tests.", "tests", 0);
    args_parser.add_option(do_benchmarks_only, "Only run benchmarks.", "bench", 0);
    args_parser.add_option(do_list_cases, "List available test cases.", "list", 0);
    args_parser.add_positional_argument(search_string, "Only run matching cases.", "pattern", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    const auto& matching_tests = find_cases(search_string, !do_benchmarks_only, !do_tests_only);

    if (do_list_cases) {
        outln("Available cases for {}:", suite_name);
        for (const auto& test : matching_tests) {
            outln("    {}", test.name());
        }
        return 0;
    }

    outln("Running {} cases out of {}.", matching_tests.size(), m_cases.size());

    return run(matching_tests);
}

NonnullRefPtrVector<TestCase> TestSuite::find_cases(const String& search, bool find_tests, bool find_benchmarks)
{
    NonnullRefPtrVector<TestCase> matches;
    for (const auto& t : m_cases) {
        if (!search.is_empty() && !t.name().matches(search, CaseSensitivity::CaseInsensitive)) {
            continue;
        }

        if (!find_tests && !t.is_benchmark()) {
            continue;
        }
        if (!find_benchmarks && t.is_benchmark()) {
            continue;
        }

        matches.append(t);
    }
    return matches;
}

int TestSuite::run(const NonnullRefPtrVector<TestCase>& tests)
{
    size_t test_count = 0;
    size_t test_failed_count = 0;
    size_t benchmark_count = 0;
    TestElapsedTimer global_timer;

    for (const auto& t : tests) {
        const auto test_type = t.is_benchmark() ? "benchmark" : "test";

        warnln("Running {} '{}'.", test_type, t.name());
        m_current_test_case_passed = true;

        TestElapsedTimer timer;
        t.func()();
        const auto time = timer.elapsed_milliseconds();

        dbgln("{} {} '{}' in {}ms", m_current_test_case_passed ? "Completed" : "Failed", test_type, t.name(), time);

        if (t.is_benchmark()) {
            m_benchtime += time;
            benchmark_count++;
        } else {
            m_testtime += time;
            test_count++;
        }

        if (!m_current_test_case_passed) {
            test_failed_count++;
        }
    }

    dbgln("Finished {} tests and {} benchmarks in {}ms ({}ms tests, {}ms benchmarks, {}ms other).",
        test_count,
        benchmark_count,
        global_timer.elapsed_milliseconds(),
        m_testtime,
        m_benchtime,
        global_timer.elapsed_milliseconds() - (m_testtime + m_benchtime));
    dbgln("Out of {} tests, {} passed and {} failed.", test_count, test_count - test_failed_count, test_failed_count);

    return (int)test_failed_count;
}

}

using AK::current_test_case_did_fail;
using AK::TestCase;
using AK::TestSuite;

#define __TESTCASE_FUNC(x) __test_##x
#define __TESTCASE_TYPE(x) __TestCase_##x

#define TEST_CASE(x)                                                                           \
    static void __TESTCASE_FUNC(x)();                                                          \
    struct __TESTCASE_TYPE(x) {                                                                \
        __TESTCASE_TYPE(x)                                                                     \
        () { TestSuite::the().add_case(adopt(*new TestCase(#x, __TESTCASE_FUNC(x), false))); } \
    };                                                                                         \
    static struct __TESTCASE_TYPE(x) __TESTCASE_TYPE(x);                                       \
    static void __TESTCASE_FUNC(x)()

#define __BENCHMARK_FUNC(x) __benchmark_##x
#define __BENCHMARK_TYPE(x) __BenchmarkCase_##x

#define BENCHMARK_CASE(x)                                                                      \
    static void __BENCHMARK_FUNC(x)();                                                         \
    struct __BENCHMARK_TYPE(x) {                                                               \
        __BENCHMARK_TYPE(x)                                                                    \
        () { TestSuite::the().add_case(adopt(*new TestCase(#x, __BENCHMARK_FUNC(x), true))); } \
    };                                                                                         \
    static struct __BENCHMARK_TYPE(x) __BENCHMARK_TYPE(x);                                     \
    static void __BENCHMARK_FUNC(x)()

#define TEST_MAIN(x)                                                \
    TestSuite* TestSuite::s_global = nullptr;                       \
    template<size_t N>                                              \
    constexpr size_t compiletime_lenof(const char(&)[N])            \
    {                                                               \
        return N - 1;                                               \
    }                                                               \
    int main(int argc, char** argv)                                 \
    {                                                               \
        static_assert(compiletime_lenof(#x) != 0, "Set SuiteName"); \
        int ret = TestSuite::the().main(#x, argc, argv);            \
        TestSuite::release();                                       \
        return ret;                                                 \
    }

#define EXPECT_EQ(a, b)                                                                                                                                                                \
    do {                                                                                                                                                                               \
        auto lhs = (a);                                                                                                                                                                \
        auto rhs = (b);                                                                                                                                                                \
        if (lhs != rhs) {                                                                                                                                                              \
            warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", __FILE__, __LINE__, #a, #b, FormatIfSupported { lhs }, FormatIfSupported { rhs }); \
            current_test_case_did_fail();                                                                                                                                              \
        }                                                                                                                                                                              \
    } while (false)

// If you're stuck and `EXPECT_EQ` seems to refuse to print anything useful,
// try this: It'll spit out a nice compiler error telling you why it doesn't print.
#define EXPECT_EQ_FORCE(a, b)                                                                                                              \
    do {                                                                                                                                   \
        auto lhs = (a);                                                                                                                    \
        auto rhs = (b);                                                                                                                    \
        if (lhs != rhs) {                                                                                                                  \
            warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_EQ({}, {}) failed with lhs={} and rhs={}", __FILE__, __LINE__, #a, #b, lhs, rhs); \
            current_test_case_did_fail();                                                                                                  \
        }                                                                                                                                  \
    } while (false)

#define EXPECT(x)                                                                              \
    do {                                                                                       \
        if (!(x)) {                                                                            \
            warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT({}) failed", __FILE__, __LINE__, #x); \
            current_test_case_did_fail();                                                      \
        }                                                                                      \
    } while (false)

#define EXPECT_APPROXIMATE(a, b)                                                                    \
    do {                                                                                            \
        auto expect_close_lhs = a;                                                                  \
        auto expect_close_rhs = b;                                                                  \
        auto expect_close_diff = expect_close_lhs - expect_close_rhs;                               \
        if (fabs(expect_close_diff) >= 0.000001) {                                                  \
            warnln("\033[31;1mFAIL\033[0m: {}:{}: EXPECT_APPROXIMATE({}, {})"                       \
                   " failed with lhs={}, rhs={}, (lhs-rhs)={}",                                     \
                __FILE__, __LINE__, #a, #b, expect_close_lhs, expect_close_rhs, expect_close_diff); \
            current_test_case_did_fail();                                                           \
        }                                                                                           \
    } while (false)
