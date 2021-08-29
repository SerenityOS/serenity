/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h> // intentionally first -- we redefine VERIFY and friends in here

#include <AK/Function.h>
#include <LibCore/ArgsParser.h>
#include <LibTest/TestSuite.h>
#include <stdlib.h>
#include <sys/time.h>

namespace Test {

TestSuite* TestSuite::s_global = nullptr;

class TestElapsedTimer {
public:
    TestElapsedTimer() { restart(); }

    void restart() { gettimeofday(&m_started, nullptr); }

    u64 elapsed_milliseconds()
    {
        struct timeval now = {};
        gettimeofday(&now, nullptr);

        struct timeval delta = {};
        timersub(&now, &m_started, &delta);

        return delta.tv_sec * 1000 + delta.tv_usec / 1000;
    }

private:
    struct timeval m_started = {};
};

// Declared in Macros.h
void current_test_case_did_fail()
{
    TestSuite::the().current_test_case_did_fail();
}

// Declared in TestCase.h
void add_test_case_to_suite(const NonnullRefPtr<TestCase>& test_case)
{
    TestSuite::the().add_case(test_case);
}

// Declared in TestCase.h
void set_suite_setup_function(Function<void()> setup)
{
    TestSuite::the().set_suite_setup(move(setup));
}

int TestSuite::main(const String& suite_name, int argc, char** argv)
{
    m_suite_name = suite_name;

    Core::ArgsParser args_parser;

    bool do_tests_only = getenv("TESTS_ONLY") != nullptr;
    bool do_benchmarks_only = false;
    bool do_list_cases = false;
    const char* search_string = "*";

    args_parser.add_option(do_tests_only, "Only run tests.", "tests", 0);
    args_parser.add_option(do_benchmarks_only, "Only run benchmarks.", "bench", 0);
    args_parser.add_option(do_list_cases, "List available test cases.", "list", 0);
    args_parser.add_positional_argument(search_string, "Only run matching cases.", "pattern", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (m_setup)
        m_setup();

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
