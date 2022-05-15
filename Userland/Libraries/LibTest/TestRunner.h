/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibTest/Results.h>
#include <fcntl.h>
#include <sys/time.h>

namespace Test {

class TestRunner {
public:
    static TestRunner* the()
    {
        return s_the;
    }

    enum class OutputFormat {
        UTF8,
        JSON,
        DetailedJSON,
        JUnit
    };

    TestRunner(String test_root, bool print_times, bool print_progress, OutputFormat output_format = OutputFormat::UTF8)
        : m_test_root(move(test_root))
        , m_print_times(print_times)
        , m_print_progress(print_progress)
        , m_output_format(output_format)
    {
        VERIFY(!s_the);
        s_the = this;
    }

    virtual ~TestRunner() { s_the = nullptr; };

    virtual void run(String test_glob);

    Test::Counts const& counts() const { return m_counts; }

    bool is_printing_progress() const { return m_print_progress; }

    bool needs_detailed_suites() const { return m_output_format == OutputFormat::DetailedJSON || m_output_format == OutputFormat::JUnit; }
    Vector<Test::Suite> const& suites() const { return *m_suites; }

    Vector<Test::Suite>& ensure_suites()
    {
        if (!m_suites.has_value())
            m_suites = Vector<Suite> {};
        return *m_suites;
    }

protected:
    static TestRunner* s_the;

    void print_test_results() const;
    void print_test_results_as_json() const;
    void print_test_results_as_junit() const;

    virtual Vector<String> get_test_paths() const = 0;
    virtual void do_run_single_test(String const&, size_t current_test_index, size_t num_tests) = 0;
    virtual Vector<String> const* get_failed_test_names() const { return nullptr; }

    String m_test_root;
    bool m_print_times;
    bool m_print_progress;

    OutputFormat m_output_format;

    double m_total_elapsed_time_in_ms { 0 };
    Test::Counts m_counts;
    Optional<Vector<Test::Suite>> m_suites;
};

inline void cleanup()
{
    // Clear the taskbar progress.
    if (TestRunner::the() && TestRunner::the()->is_printing_progress())
        warn("\033]9;-1;\033\\");
}

[[noreturn]] inline void cleanup_and_exit()
{
    cleanup();
    exit(1);
}

inline double get_time_in_ms()
{
    struct timeval tv1;
    auto return_code = gettimeofday(&tv1, nullptr);
    VERIFY(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

template<typename Callback>
inline void iterate_directory_recursively(String const& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto name = directory_iterator.next_path();
        struct stat st = {};
        if (fstatat(directory_iterator.fd(), name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0)
            continue;
        bool is_directory = S_ISDIR(st.st_mode);
        auto full_path = String::formatted("{}/{}", directory_path, name);
        if (is_directory && name != "/Fixtures"sv) {
            iterate_directory_recursively(full_path, callback);
        } else if (!is_directory) {
            callback(full_path);
        }
    }
}

inline void TestRunner::run(String test_glob)
{
    size_t progress_counter = 0;
    auto test_paths = get_test_paths();
    for (auto& path : test_paths) {
        if (!path.matches(test_glob))
            continue;
        ++progress_counter;
        do_run_single_test(path, progress_counter, test_paths.size());
        if (m_print_progress)
            warn("\033]9;{};{};\033\\", progress_counter, test_paths.size());
    }

    if (m_print_progress)
        warn("\033]9;-1;\033\\");

    switch (m_output_format) {
    case OutputFormat::UTF8:
        print_test_results();
        break;
    case OutputFormat::JSON:
    case OutputFormat::DetailedJSON:
        print_test_results_as_json();
        break;
    case OutputFormat::JUnit:
        print_test_results_as_junit();
        break;
    }
}

enum Modifier {
    BG_RED,
    BG_GREEN,
    FG_RED,
    FG_GREEN,
    FG_ORANGE,
    FG_GRAY,
    FG_BLACK,
    FG_BOLD,
    ITALIC,
    CLEAR,
};

inline void print_modifiers(Vector<Modifier> modifiers)
{
    for (auto& modifier : modifiers) {
        auto code = [&] {
            switch (modifier) {
            case BG_RED:
                return "\033[48;2;255;0;102m";
            case BG_GREEN:
                return "\033[48;2;102;255;0m";
            case FG_RED:
                return "\033[38;2;255;0;102m";
            case FG_GREEN:
                return "\033[38;2;102;255;0m";
            case FG_ORANGE:
                return "\033[38;2;255;102;0m";
            case FG_GRAY:
                return "\033[38;2;135;139;148m";
            case FG_BLACK:
                return "\033[30m";
            case FG_BOLD:
                return "\033[1m";
            case ITALIC:
                return "\033[3m";
            case CLEAR:
                return "\033[0m";
            }
            VERIFY_NOT_REACHED();
        }();
        out("{}", code);
    }
}

inline void TestRunner::print_test_results() const
{
    out("\nTest Suites: ");
    if (m_counts.suites_failed) {
        print_modifiers({ FG_RED });
        out("{} failed, ", m_counts.suites_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.suites_passed) {
        print_modifiers({ FG_GREEN });
        out("{} passed, ", m_counts.suites_passed);
        print_modifiers({ CLEAR });
    }
    outln("{} total", m_counts.suites_failed + m_counts.suites_passed);

    out("Tests:       ");
    if (m_counts.tests_failed) {
        print_modifiers({ FG_RED });
        out("{} failed, ", m_counts.tests_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_skipped) {
        print_modifiers({ FG_ORANGE });
        out("{} skipped, ", m_counts.tests_skipped);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_passed) {
        print_modifiers({ FG_GREEN });
        out("{} passed, ", m_counts.tests_passed);
        print_modifiers({ CLEAR });
    }
    outln("{} total", m_counts.tests_failed + m_counts.tests_skipped + m_counts.tests_passed);

    outln("Files:       {} total", m_counts.files_total);

    out("Time:        ");
    if (m_total_elapsed_time_in_ms < 1000.0) {
        outln("{}ms", static_cast<int>(m_total_elapsed_time_in_ms));
    } else {
        outln("{:>.3}s", m_total_elapsed_time_in_ms / 1000.0);
    }
    if (auto* failed_tests = get_failed_test_names(); failed_tests && !failed_tests->is_empty()) {
        outln("Failed tests: {}", *failed_tests);
    }
    outln();
}

inline void TestRunner::print_test_results_as_json() const
{
    JsonObject root;

    auto result_to_str = [](Result res) {
        switch (res) {
        case Result::Pass:
            return "PASSED"sv;
        case Result::Fail:
            return "FAILED"sv;
        case Result::Skip:
            return "SKIPPED"sv;
        case Result::Crashed:
            return "PROCESS_ERROR"sv;
        }
        return "FAILED"sv;
    };

    if (needs_detailed_suites()) {
        auto& suites = this->suites();
        u64 duration_us = 0;
        JsonObject tests;

        for (auto& suite : suites) {
            for (auto& case_ : suite.tests) {
                duration_us += case_.duration_us;
                StringView result_name = result_to_str(case_.result);

                auto name = suite.name;
                if (name == "__$$TOP_LEVEL$$__"sv)
                    name = String::empty();

                auto path = LexicalPath::relative_path(suite.path, m_test_root);

                tests.set(String::formatted("{}/{}::{}", path, name, case_.name), result_name);
            }
        }

        root.set("duration", static_cast<double>(duration_us) / 1000000.);
        root.set("results", move(tests));
    } else {
        JsonArray tests;
        for (auto const& suite : suites()) {
            for (auto const& case_ : suite.tests) {
                StringView result_name = result_to_str(case_.result);

                auto name = suite.name;
                if (name == "__$$TOP_LEVEL$$__"sv)
                    name = String::empty();

                auto path = LexicalPath::relative_path(suite.path, m_test_root);

                JsonObject current_test;
                current_test.set("name", String::formatted("{}/{}::{}", path, name, case_.name));
                current_test.set("type", "test");
                current_test.set("elapsed_ms", case_.duration_us / 1000);
                current_test.set("result", result_name);
                current_test.set("details", case_.details);
                tests.append(current_test);
            }
        }

        root.set("results", move(tests));

        root.set("name", ""); // FIXME: use current program name from somewhere
        root.set("total_ms", m_total_elapsed_time_in_ms);
        root.set("passed", m_counts.tests_passed);
        root.set("failed", m_counts.tests_failed);
    }

    if (StringView fd_str = getenv("LIBTEST_JSON_FD"); !fd_str.is_empty()) {
        auto fd = fd_str.to_int();
        if (fd.has_value()) {
            auto file = MUST(Core::Stream::File::adopt_fd(fd.release_value(), Core::Stream::OpenMode::Write));
            MUST(file->write(root.to_string().bytes()));
        } else {
            warnln("Unable to open fd {} for test output", fd_str);
        }
    } else {
        outln("{}", root.to_string());
    }
}

inline void write_junit_test_case_xml(FILE* out, ::Test::Case const& test, StringView suite_name)
{
    auto status = [](Test::Result res) {
        switch (res) {
        case Test::Result::Pass:
            return "run"sv;
        case Test::Result::Skip:
            return "disabled"sv;
        case Test::Result::Crashed:
            return "error"sv;
        case Test::Result::Fail:
            return "fail"sv;
        }
        VERIFY_NOT_REACHED();
    }(test.result);

    outln(out, R"~~~(<testcase name="{}" classname="{}" time="{}" status="{}">)~~~",
        test.name, suite_name, static_cast<double>(test.duration_us) / 1'000'000.0, status);

    // Get the control chars and XML reserved chars out of the test-reported details
    StringBuilder details_builder;
    details_builder.append_escaped_for_json(escape_html_entities(test.details));
    auto details = details_builder.to_string();

    switch (test.result) {
    case Test::Result::Fail:
        // Azure PublishTestResults pulls failure reason out of the message attribute
        outln(out, R"~~~(<failure message="{}"/>)~~~", details);
        outln(out, R"~~~(<system-err>{}</system-err>)~~~", details);
        break;
    case Test::Result::Crashed:
        outln(out, R"~~~(<error message="{}"/>)~~~", details);
        outln(out, R"~~~(<system-err>{}</system-err>)~~~", details);
        break;
    default:
        break;
    }
    outln(out, "</testcase>");
}

inline void TestRunner::print_test_results_as_junit() const
{
    // FIXME: Configure output filepath instead of always using ~/junit.xml
    //        Would be particularly useful for output from nested TestRunners like test-js or test-wasm
    auto* junit_out = fopen(String::formatted("{}/{}", Core::StandardPaths::home_directory(), "junit.xml").characters(), "+w");

    constexpr auto iso8601_format = "%Y-%m-dT%H:%M:%S%z"sv;

    String hostname = "localhost"sv;
    auto hostname_or_error = Core::System::gethostname();
    if (!hostname_or_error.is_error())
        hostname = hostname_or_error.release_value();
    String timestamp = Core::DateTime::now().to_string(iso8601_format);

    outln(junit_out, R"~~~(<?xml version="1.0" encoding="UTF-8"?>
<testsuites>)~~~");

    auto const& suites = this->suites();

    for (auto const& suite : suites) {
        size_t num_failures = 0;
        size_t num_skipped = 0;
        size_t num_crashed = 0;

        for (auto const& test : suite.tests) {
            if (test.result == Test::Result::Skip)
                ++num_skipped;
            else if (test.result == Test::Result::Fail) {
                ++num_failures;
            } else if (test.result == Test::Result::Crashed) {
                ++num_crashed;
            }
        }
        // Just one "test" in this case
        if (suite.tests.is_empty()) {
            if (suite.most_severe_test_result == Test::Result::Fail)
                ++num_failures;
            else if (suite.most_severe_test_result == Test::Result::Skip)
                ++num_skipped;
            if (suite.most_severe_test_result == Test::Result::Crashed)
                ++num_crashed;
        }
        String name = LexicalPath(suite.name).basename();
        if (name == "__$$TOP_LEVEL$$__"sv)
            name = String::empty();

        auto path = LexicalPath::relative_path(suite.path, m_test_root);

        outln(junit_out, R"~~~(<testsuite name="{}"
    tests="{}"
    failures="{}"
    disabled="0"
    skipped="{}"
    errors="{}"
    hostname="{}"
    time="{}"
    timestamp="{}"
    >)~~~",
            path,
            suite.tests.size() ?: 1,
            num_failures,
            num_skipped,
            num_crashed,
            hostname,
            static_cast<double>(suite.total_duration_us) / 1'000'000,
            timestamp);

        for (auto const& test : suite.tests) {
            write_junit_test_case_xml(junit_out, test, name);
        }
        // Just one "test" in this case
        if (suite.tests.is_empty()) {
            Case test = {
                .name = name,
                .result = suite.most_severe_test_result,
                .details = ""sv,
                .duration_us = suite.total_duration_us,
            };
            write_junit_test_case_xml(junit_out, test, name);
        }
        outln(junit_out, "</testsuite>");
    }

    outln(junit_out, "</testsuites>");
    (void)fclose(junit_out);
}
}
