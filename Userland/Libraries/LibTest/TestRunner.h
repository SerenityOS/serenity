/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
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

    TestRunner(String test_root, bool print_times, bool print_progress, bool print_json)
        : m_test_root(move(test_root))
        , m_print_times(print_times)
        , m_print_progress(print_progress)
        , m_print_json(print_json)
    {
        VERIFY(!s_the);
        s_the = this;
    }

    virtual ~TestRunner() { s_the = nullptr; };

    virtual void run(String test_glob);

    const Test::Counts& counts() const { return m_counts; }

    bool is_printing_progress() const { return m_print_progress; }

protected:
    static TestRunner* s_the;

    void print_test_results() const;
    void print_test_results_as_json() const;

    virtual Vector<String> get_test_paths() const = 0;
    virtual void do_run_single_test(const String&, size_t current_test_index, size_t num_tests) = 0;
    virtual const Vector<String>* get_failed_test_names() const { return nullptr; }

    String m_test_root;
    bool m_print_times;
    bool m_print_progress;
    bool m_print_json;

    double m_total_elapsed_time_in_ms { 0 };
    Test::Counts m_counts;
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
inline void iterate_directory_recursively(const String& directory_path, Callback callback)
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

    if (!m_print_json)
        print_test_results();
    else
        print_test_results_as_json();
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
    JsonObject suites;
    suites.set("failed", m_counts.suites_failed);
    suites.set("passed", m_counts.suites_passed);
    suites.set("total", m_counts.suites_failed + m_counts.suites_passed);

    JsonObject tests;
    tests.set("failed", m_counts.tests_failed);
    tests.set("passed", m_counts.tests_passed);
    tests.set("skipped", m_counts.tests_skipped);
    tests.set("total", m_counts.tests_failed + m_counts.tests_passed + m_counts.tests_skipped);

    JsonObject results;
    results.set("suites", suites);
    results.set("tests", tests);

    JsonObject root;
    root.set("results", results);
    root.set("files_total", m_counts.files_total);
    root.set("duration", m_total_elapsed_time_in_ms / 1000.0);

    outln("{}", root.to_string());
}

}
