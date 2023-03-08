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
#include <math.h>
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
void add_test_case_to_suite(NonnullRefPtr<TestCase> const& test_case)
{
    TestSuite::the().add_case(test_case);
}

// Declared in TestCase.h
void set_suite_setup_function(Function<void()> setup)
{
    TestSuite::the().set_suite_setup(move(setup));
}

int TestSuite::main(DeprecatedString const& suite_name, Span<StringView> arguments)
{
    m_suite_name = suite_name;

    Core::ArgsParser args_parser;

    bool do_tests_only = getenv("TESTS_ONLY") != nullptr;
    bool do_benchmarks_only = false;
    bool do_list_cases = false;
    StringView search_string = "*"sv;

    args_parser.add_option(do_tests_only, "Only run tests.", "tests", 0);
    args_parser.add_option(do_benchmarks_only, "Only run benchmarks.", "bench", 0);
    args_parser.add_option(m_benchmark_repetitions, "Number of times to repeat each benchmark (default 1)", "benchmark_repetitions", 0, "N");
    args_parser.add_option(do_list_cases, "List available test cases.", "list", 0);
    args_parser.add_positional_argument(search_string, "Only run matching cases.", "pattern", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (m_setup)
        m_setup();

    auto const& matching_tests = find_cases(search_string, !do_benchmarks_only, !do_tests_only);

    if (do_list_cases) {
        outln("Available cases for {}:", suite_name);
        for (auto const& test : matching_tests) {
            outln("    {}", test->name());
        }
        return 0;
    }

    outln("Running {} cases out of {}.", matching_tests.size(), m_cases.size());

    return run(matching_tests);
}

Vector<NonnullRefPtr<TestCase>> TestSuite::find_cases(DeprecatedString const& search, bool find_tests, bool find_benchmarks)
{
    Vector<NonnullRefPtr<TestCase>> matches;
    for (auto& t : m_cases) {
        if (!search.is_empty() && !t->name().matches(search, CaseSensitivity::CaseInsensitive)) {
            continue;
        }

        if (!find_tests && !t->is_benchmark()) {
            continue;
        }
        if (!find_benchmarks && t->is_benchmark()) {
            continue;
        }

        matches.append(t);
    }
    return matches;
}

int TestSuite::run(Vector<NonnullRefPtr<TestCase>> const& tests)
{
    size_t test_count = 0;
    size_t test_failed_count = 0;
    size_t benchmark_count = 0;
    TestElapsedTimer global_timer;

    for (auto const& t : tests) {
        auto const test_type = t->is_benchmark() ? "benchmark" : "test";
        auto const repetitions = t->is_benchmark() ? m_benchmark_repetitions : 1;

        warnln("Running {} '{}'.", test_type, t->name());
        m_current_test_case_passed = true;

        u64 total_time = 0;
        u64 sum_of_squared_times = 0;
        u64 min_time = NumericLimits<u64>::max();
        u64 max_time = 0;

        for (u64 i = 0; i < repetitions; ++i) {
            TestElapsedTimer timer;
            t->func()();
            auto const iteration_time = timer.elapsed_milliseconds();
            total_time += iteration_time;
            sum_of_squared_times += iteration_time * iteration_time;
            min_time = min(min_time, iteration_time);
            max_time = max(max_time, iteration_time);
        }

        if (repetitions != 1) {
            double average = total_time / double(repetitions);
            double average_squared = average * average;
            double standard_deviation = sqrt((sum_of_squared_times + repetitions * average_squared - 2 * total_time * average) / (repetitions - 1));

            dbgln("{} {} '{}' on average in {:.1f}±{:.1f}ms (min={}ms, max={}ms, total={}ms)",
                m_current_test_case_passed ? "Completed" : "Failed", test_type, t->name(),
                average, standard_deviation, min_time, max_time, total_time);
        } else {
            dbgln("{} {} '{}' in {}ms", m_current_test_case_passed ? "Completed" : "Failed", test_type, t->name(), total_time);
        }

        if (t->is_benchmark()) {
            m_benchtime += total_time;
            benchmark_count++;
        } else {
            m_testtime += total_time;
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
