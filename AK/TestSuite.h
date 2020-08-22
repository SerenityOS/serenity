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

#define AK_TEST_SUITE

#define ASSERT(x)                                                                                         \
    {                                                                                                     \
        if (!(x))                                                                                         \
            fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%d: ASSERT(%s) failed\n", __FILE__, __LINE__, #x); \
    }

#define RELEASE_ASSERT(x)                                                                                         \
    {                                                                                                             \
        if (!(x))                                                                                                 \
            fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%d: RELEASE_ASSERT(%s) failed\n", __FILE__, __LINE__, #x); \
    }

#define ASSERT_NOT_REACHED()                                                                                \
    {                                                                                                       \
        fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%d: ASSERT_NOT_REACHED() called\n", __FILE__, __LINE__); \
        abort();                                                                                            \
    }

#define TODO()                                                                                \
    {                                                                                         \
        fprintf(stderr, "\033[31;1mFAIL\033[0m: %s:%d: TODO() called\n", __FILE__, __LINE__); \
        abort();                                                                              \
    }

#include <stdio.h>

#include <AK/Function.h>
#include <AK/LogStream.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>

#include <LibCore/ArgsParser.h>

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

using TestFunction = AK::Function<void()>;

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

    void run(const NonnullRefPtrVector<TestCase>&);
    void main(const String& suite_name, int argc, char** argv);
    NonnullRefPtrVector<TestCase> find_cases(const String& search, bool find_tests, bool find_benchmarks);
    void add_case(const NonnullRefPtr<TestCase>& test_case)
    {
        m_cases.append(test_case);
    }

private:
    static TestSuite* s_global;
    NonnullRefPtrVector<TestCase> m_cases;
    u64 m_testtime = 0;
    u64 m_benchtime = 0;
    String m_suite_name;
};

void TestSuite::main(const String& suite_name, int argc, char** argv)
{
    m_suite_name = suite_name;

    Core::ArgsParser args_parser;

    bool do_tests_only = false;
    bool do_benchmarks_only = false;
    bool do_list_cases = false;
    const char* search_string = "*";

    args_parser.add_option(do_tests_only, "Only run tests.", "tests", 0);
    args_parser.add_option(do_benchmarks_only, "Only run benchmarks.", "bench", 0);
    args_parser.add_option(do_list_cases, "List avaliable test cases.", "list", 0);
    args_parser.add_positional_argument(search_string, "Only run matching cases.", "pattern", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    const auto& matching_tests = find_cases(search_string, !do_benchmarks_only, !do_tests_only);

    if (do_list_cases) {
        out() << "Avaliable cases for " << suite_name << ":";
        for (const auto& test : matching_tests) {
            out() << "    " << test.name();
        }
    } else {
        out() << "Running " << matching_tests.size() << " cases out of " << m_cases.size();

        run(matching_tests);
    }
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

void TestSuite::run(const NonnullRefPtrVector<TestCase>& tests)
{
    size_t test_count = 0;
    size_t benchmark_count = 0;
    TestElapsedTimer global_timer;

    for (const auto& t : tests) {
        const auto test_type = t.is_benchmark() ? "benchmark" : "test";

        dbg() << "START Running " << test_type << " " << t.name();

        TestElapsedTimer timer;
        t.func()();
        const auto time = timer.elapsed_milliseconds();

        warn() << "\033[32;1mPASS\033[0m: " << time << " ms running " << test_type << " " << t.name();

        if (t.is_benchmark()) {
            m_benchtime += time;
            benchmark_count++;
        } else {
            m_testtime += time;
            test_count++;
        }
    }

    dbg() << "Finished " << test_count << " tests and " << benchmark_count << " benchmarks in " << global_timer.elapsed_milliseconds() << " ms ("
          << m_testtime << " tests, " << m_benchtime << " benchmarks, " << (global_timer.elapsed_milliseconds() - (m_testtime + m_benchtime)) << " other)";
}

}

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
        TestSuite::the().main(#x, argc, argv);                      \
        TestSuite::release();                                       \
    }

#define EXPECT_EQ(a, b)                                                                                           \
    {                                                                                                             \
        if ((a) != (b))                                                                                           \
            warn() << "\033[31;1mFAIL\033[0m: " __FILE__ ":" << __LINE__ << ": EXPECT_EQ(" #a ", " #b ") failed"; \
    }

#define EXPECT(x)                                                                                      \
    {                                                                                                  \
        if (!(x))                                                                                      \
            warn() << "\033[31;1mFAIL\033[0m: " __FILE__ ":" << __LINE__ << ": EXPECT(" #x ") failed"; \
    }
