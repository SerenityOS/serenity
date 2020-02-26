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

#include <stdio.h>

#define ASSERT(x)                                             \
    if (!(x)) {                                               \
        fprintf(stderr, "\033[33;1mASSERT\033[0m: " #x "\n"); \
    }

#define ASSERT_NOT_REACHED() fprintf(stderr, "\033[31;1mASSERT_NOT_REACHED\033[0m\n");
#define RELEASE_ASSERT ASSERT

#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <chrono>

namespace AK {

class TestElapsedTimer {
    typedef std::chrono::high_resolution_clock clock;

public:
    TestElapsedTimer() { restart(); }
    void restart() { m_started = clock::now(); }
    int64_t elapsed()
    {
        auto end = clock::now();
        auto elapsed = end - m_started;
        return std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count();
    }

private:
    std::chrono::time_point<clock> m_started;
};

class TestException {
public:
    TestException(const String& file, int line, const String& s)
        : file(file)
        , line(line)
        , reason(s)
    {
    }

    String to_string() const
    {
        String outfile = file;
        // ###
        //auto slash = file.lastIndexOf("/");
        //if (slash > 0) {
        //    outfile = outfile.right(outfile.length() - slash - 1);
        //}
        return String::format("%s:%d: %s", outfile.characters(), line, reason.characters());
    }

private:
    String file;
    int line = 0;
    String reason;
};

typedef AK::Function<void()> TestFunction;

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
        delete s_global;
        s_global = nullptr;
    }

    void run(const NonnullRefPtrVector<TestCase>& tests);
    void main(const String& suite_name, int argc, char** argv);
    NonnullRefPtrVector<TestCase> find_cases(const String& search, bool find_tests, bool find_benchmarks);
    void add_case(const NonnullRefPtr<TestCase>& test_case)
    {
        m_cases.append(test_case);
    }

private:
    static TestSuite* s_global;
    NonnullRefPtrVector<TestCase> m_cases;
    uint64_t m_testtime = 0;
    uint64_t m_benchtime = 0;
    String m_suite_name;
};

void TestSuite::main(const String& suite_name, int argc, char** argv)
{
    m_suite_name = suite_name;
    bool find_tests = true;
    bool find_benchmarks = true;

    String search_string;
    for (int i = 1; i < argc; i++) {
        if (!String(argv[i]).starts_with("--")) {
            search_string = argv[i];
        } else if (String(argv[i]) == String("--bench")) {
            find_tests = false;
        } else if (String(argv[i]) == String("--test")) {
            find_benchmarks = false;
        } else if (String(argv[i]) == String("--help")) {
            dbg() << "Available tests for " << suite_name << ":";
            const auto& tests = find_cases("*", true, false);
            for (const auto& t : tests) {
                dbg() << "\t" << t.name();
            }
            dbg() << "Available benchmarks for " << suite_name << ":";
            const auto& benches = find_cases("*", false, true);
            for (const auto& t : benches) {
                dbg() << "\t" << t.name();
            }
            exit(0);
        }
    }

    const auto& matches = find_cases(search_string, find_tests, find_benchmarks);
    if (matches.size() == 0) {
        dbg() << "0 matches when searching for " << search_string << " (out of " << m_cases.size() << ")";
        exit(1);
    }
    dbg() << "Running " << matches.size() << " cases out of " << m_cases.size();
    run(matches);
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
    int test_count = 0;
    int benchmark_count = 0;
    TestElapsedTimer global_timer;
    for (const auto& t : tests) {
        dbg() << "START Running " << (t.is_benchmark() ? "benchmark" : "test") << " " << t.name();
        TestElapsedTimer timer;
        try {
            t.func()();
        } catch (const TestException& t) {
            fprintf(stderr, "\033[31;1mFAIL\033[0m: %s\n", t.to_string().characters());
            exit(1);
        }
        auto time = timer.elapsed();
        fprintf(stderr, "\033[32;1mPASS\033[0m: %d ms running %s %s\n", (int)time, (t.is_benchmark() ? "benchmark" : "test"), t.name().characters());
        if (t.is_benchmark()) {
            m_benchtime += time;
            benchmark_count++;
        } else {
            m_testtime += time;
            test_count++;
        }
    }
    dbg() << "Finished " << test_count << " tests and " << benchmark_count << " benchmarks in " << (int)global_timer.elapsed() << " ms ("
          << (int)m_testtime << " tests, " << (int)m_benchtime << " benchmarks, " << int(global_timer.elapsed() - (m_testtime + m_benchtime)) << " other)";
}

}

using AK::TestCase;
using AK::TestException;
using AK::TestSuite;

#define xstr(s) ___str(s)
#define ___str(s) #s

#define TESTCASE_TYPE_NAME(x) TestCase_##x

/*! Define a test case function. */
#define TEST_CASE(x)                                                                 \
    static void x();                                                                 \
    struct TESTCASE_TYPE_NAME(x) {                                                   \
        TESTCASE_TYPE_NAME(x)                                                        \
        () { TestSuite::the().add_case(adopt(*new TestCase(___str(x), x, false))); } \
    };                                                                               \
    static struct TESTCASE_TYPE_NAME(x) TESTCASE_TYPE_NAME(x);                       \
    static void x()

#define BENCHMARK_TYPE_NAME(x) TestCase_##x

#define BENCHMARK_CASE(x)                                                           \
    static void x();                                                                \
    struct BENCHMARK_TYPE_NAME(x) {                                                 \
        BENCHMARK_TYPE_NAME(x)                                                      \
        () { TestSuite::the().add_case(adopt(*new TestCase(___str(x), x, true))); } \
    };                                                                              \
    static struct BENCHMARK_TYPE_NAME(x) BENCHMARK_TYPE_NAME(x);                    \
    static void x()

/*! Define the main function of the testsuite. All TEST_CASE functions will be executed. */
#define TEST_MAIN(SuiteName)                                                       \
    TestSuite* TestSuite::s_global = nullptr;                                      \
    template<size_t N>                                                             \
    constexpr size_t compiletime_lenof(const char(&)[N])                           \
    {                                                                              \
        return N - 1;                                                              \
    }                                                                              \
    int main(int argc, char** argv)                                                \
    {                                                                              \
        static_assert(compiletime_lenof(___str(SuiteName)) != 0, "Set SuiteName"); \
        TestSuite::the().main(___str(SuiteName), argc, argv);                      \
        TestSuite::release();                                                      \
    }

#define assertEqual(one, two)                                                                                                                                              \
    do {                                                                                                                                                                   \
        auto ___aev1 = one;                                                                                                                                                \
        auto ___aev2 = two;                                                                                                                                                \
        if (___aev1 != ___aev2) {                                                                                                                                          \
            dbg() << "\033[31;1mFAIL\033[0m: " __FILE__ ":" << __LINE__ << ": assertEqual(" ___str(one) ", " ___str(two) ") failed"; \
        }                                                                                                                                                                  \
    } while (0)

#define EXPECT_EQ(one, two) assertEqual(one, two)

#define EXPECT(one) assertEqual(one, true)
