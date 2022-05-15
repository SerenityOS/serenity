/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h> // intentionally first -- we redefine VERIFY and friends in here

#include <AK/Function.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibTest/TestSuite.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

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

// Declared in TestCase.h
void vreport_test_details(StringView fmtstr, AK::TypeErasedFormatParams& params)
{
    TestSuite::the().vreport_test_details(fmtstr, params);
}

void TestSuite::vreport_test_details(StringView fmtstr, AK::TypeErasedFormatParams& params)
{
    MUST(vformat(m_current_test_case_details, fmtstr, params));
}

int TestSuite::main(String const& suite_name, int argc, char** argv)
{
    m_suite_name = LexicalPath(suite_name).basename();

    Core::ArgsParser args_parser;

    String json_fd_str = getenv("LIBTEST_JSON_FD");

    bool do_tests_only = getenv("TESTS_ONLY") != nullptr;
    bool do_benchmarks_only = false;
    bool do_list_cases = false;
    bool do_output_json = !json_fd_str.is_empty();
    char const* search_string = "*";

    args_parser.add_option(do_tests_only, "Only run tests.", "tests", '\0');
    args_parser.add_option(do_benchmarks_only, "Only run benchmarks.", "bench", '\0');
    args_parser.add_option(do_list_cases, "List available test cases.", "list", '\0');
    args_parser.add_option(do_output_json, "Dump results as JSON", "json", '\0');
    args_parser.add_positional_argument(search_string, "Only run matching cases.", "pattern", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (m_setup)
        m_setup();

    auto const& matching_tests = find_cases(search_string, !do_benchmarks_only, !do_tests_only);

    if (do_list_cases) {
        outln("Available cases for {}:", m_suite_name);
        for (auto const& test : matching_tests) {
            outln("    {}", test.name());
        }
        return 0;
    }

    outln("Running {} cases out of {}.", matching_tests.size(), m_cases.size());

    RefPtr<Core::File> json_file;
    if (!json_fd_str.is_empty()) {
        auto maybe_fd = json_fd_str.to_int();
        if (!maybe_fd.has_value()) {
            warnln("Invalid FD {} passed to Test Suite!", json_fd_str);
            do_output_json = false;
        } else {
            json_file = Core::File::construct();
            bool opened = json_file->open(maybe_fd.release_value(), Core::OpenMode::WriteOnly, Core::File::ShouldCloseFileDescriptor::Yes);
            VERIFY(opened);
        }
    }
    if (do_output_json && !json_file) {
        auto json_filename = String::formatted("./{}.json", m_suite_name);
        auto maybe_file = Core::File::open(json_filename, Core::OpenMode::WriteOnly);
        if (maybe_file.is_error())
            warnln("Unable to open {} for json output", json_filename);
        else
            json_file = maybe_file.release_value();
    }

    return run(matching_tests, move(json_file));
}

NonnullRefPtrVector<TestCase> TestSuite::find_cases(String const& search, bool find_tests, bool find_benchmarks)
{
    NonnullRefPtrVector<TestCase> matches;
    for (auto const& t : m_cases) {
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

int TestSuite::run(NonnullRefPtrVector<TestCase> const& tests, RefPtr<Core::File> json_output)
{
    size_t test_count = 0;
    size_t test_failed_count = 0;
    size_t benchmark_count = 0;
    JsonArray test_array;
    TestElapsedTimer global_timer;

    for (auto const& t : tests) {
        auto const test_type = t.is_benchmark() ? "benchmark"sv : "test"sv;
        warnln("Running {} '{}'.", test_type, t.name());
        m_current_test_case_passed = true;
        m_current_test_case_details.clear();

        TestElapsedTimer timer;
        t.func()();
        auto const time = timer.elapsed_milliseconds();

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

        if (json_output) {
            JsonObject current_test;
            current_test.set("name", t.name());
            current_test.set("type", test_type);
            current_test.set("elapsed_ms", time);
            current_test.set("result", m_current_test_case_passed ? "PASSED" : "FAILED");
            current_test.set("details", m_current_test_case_details.to_string());
            test_array.append(current_test);
        }
    }

    auto total_ms = global_timer.elapsed_milliseconds();
    dbgln("Finished {} tests and {} benchmarks in {}ms ({}ms tests, {}ms benchmarks, {}ms other).",
        test_count,
        benchmark_count,
        total_ms,
        m_testtime,
        m_benchtime,
        total_ms - (m_testtime + m_benchtime));
    dbgln("Out of {} tests, {} passed and {} failed.", test_count, test_count - test_failed_count, test_failed_count);

    if (json_output) {
        JsonObject root;
        root.set("name", m_suite_name);
        root.set("total_ms", total_ms);
        root.set("passed", test_count - test_failed_count);
        root.set("failed", test_failed_count);
        root.set("results", test_array);
        json_output->write(root.to_string());
    }

    return (int)test_failed_count;
}
}
