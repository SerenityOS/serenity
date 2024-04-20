/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Environment.h>
#include <LibCore/System.h>
#include <LibCoredump/Backtrace.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>
#include <LibTest/TestRunner.h>
#include <signal.h>
#include <spawn.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Test {
TestRunner* TestRunner::s_the = nullptr;
}
using Test::get_time_in_ms;
using Test::print_modifiers;

struct FileResult {
    LexicalPath file_path;
    double time_taken { 0 };
    Test::Result result { Test::Result::Pass };
    int stdout_err_fd { -1 };
    pid_t child_pid { 0 };
};

ByteString g_currently_running_test;

class TestRunner : public ::Test::TestRunner {
public:
    TestRunner(ByteString test_root, Regex<PosixExtended> exclude_regex, NonnullRefPtr<Core::ConfigFile> config, Regex<PosixExtended> skip_regex, bool run_skipped_tests, bool print_progress, bool print_json, bool print_all_output, bool unlink_coredumps, bool print_times = true)
        : ::Test::TestRunner(move(test_root), print_times, print_progress, print_json)
        , m_exclude_regex(move(exclude_regex))
        , m_config(move(config))
        , m_skip_regex(move(skip_regex))
        , m_run_skipped_tests(run_skipped_tests)
        , m_print_all_output(print_all_output)
        , m_unlink_coredumps(unlink_coredumps)
    {
        if (!run_skipped_tests) {
            m_skip_directories = m_config->read_entry("Global", "SkipDirectories", "").split(' ');
            m_skip_files = m_config->read_entry("Global", "SkipTests", "").split(' ');
        }
    }

    virtual ~TestRunner() = default;

protected:
    virtual void do_run_single_test(ByteString const& test_path, size_t current_text_index, size_t num_tests) override;
    virtual Vector<ByteString> get_test_paths() const override;
    virtual Vector<ByteString> const* get_failed_test_names() const override { return &m_failed_test_names; }

    virtual FileResult run_test_file(ByteString const& test_path);

    bool should_skip_test(LexicalPath const& test_path);

    Regex<PosixExtended> m_exclude_regex;
    NonnullRefPtr<Core::ConfigFile> m_config;
    Vector<ByteString> m_skip_directories;
    Vector<ByteString> m_skip_files;
    Vector<ByteString> m_failed_test_names;
    Regex<PosixExtended> m_skip_regex;
    bool m_run_skipped_tests { false };
    bool m_print_all_output { false };
    bool m_unlink_coredumps { false };
};

Vector<ByteString> TestRunner::get_test_paths() const
{
    Vector<ByteString> paths;
    Test::iterate_directory_recursively(m_test_root, [&](ByteString const& file_path) {
        if (access(file_path.characters(), R_OK | X_OK) != 0)
            return;
        auto result = m_exclude_regex.match(file_path, PosixFlags::Global);
        if (!result.success) // must NOT match the regex to be a valid test file
            paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

bool TestRunner::should_skip_test(LexicalPath const& test_path)
{
    if (m_run_skipped_tests)
        return false;

    for (ByteString const& dir : m_skip_directories) {
        if (test_path.dirname().contains(dir))
            return true;
    }
    for (ByteString const& file : m_skip_files) {
        if (test_path.basename().contains(file))
            return true;
    }
    auto result = m_skip_regex.match(test_path.basename(), PosixFlags::Global);
    if (result.success)
        return true;

    return false;
}

void TestRunner::do_run_single_test(ByteString const& test_path, size_t current_test_index, size_t num_tests)
{
    g_currently_running_test = test_path;
    auto test_relative_path = LexicalPath::relative_path(test_path, m_test_root);
    outln(" START  {} ({}/{})", test_relative_path, current_test_index, num_tests);
    fflush(stdout); // we really want to see the start text in case the test hangs
    auto test_result = run_test_file(test_path);

    switch (test_result.result) {
    case Test::Result::Pass:
        ++m_counts.tests_passed;
        break;
    case Test::Result::ExpectedFail:
        ++m_counts.tests_expected_failed;
        break;
    case Test::Result::Skip:
        ++m_counts.tests_skipped;
        break;
    case Test::Result::Fail:
        ++m_counts.tests_failed;
        break;
    case Test::Result::Crashed:
        ++m_counts.tests_failed; // FIXME: tests_crashed
        break;
    }
    if (test_result.result != Test::Result::Skip)
        ++m_counts.files_total;

    m_total_elapsed_time_in_ms += test_result.time_taken;

    bool crashed_or_failed = test_result.result == Test::Result::Fail || test_result.result == Test::Result::Crashed;
    bool print_stdout_stderr = crashed_or_failed || m_print_all_output;
    if (crashed_or_failed) {
        m_failed_test_names.append(test_path);
        print_modifiers({ Test::BG_RED, Test::FG_BOLD });
        out("{}", test_result.result == Test::Result::Fail ? " FAIL  " : "CRASHED");
        print_modifiers({ Test::CLEAR });
        if (test_result.result == Test::Result::Crashed) {
            auto pid_search_string = ByteString::formatted("_{}_", test_result.child_pid);
            Optional<ByteString> coredump_path;
            Core::DirIterator iterator("/tmp/coredump"sv);
            if (!iterator.has_error()) {
                while (iterator.has_next()) {
                    auto path = iterator.next_full_path();
                    if (!path.contains(pid_search_string))
                        continue;

                    coredump_path = path;
                    auto reader = Coredump::Reader::create(path);
                    if (!reader)
                        break;

                    dbgln("Last crash backtrace for {} (was pid {}):", test_path, test_result.child_pid);
                    reader->for_each_thread_info([&](auto thread_info) {
                        Coredump::Backtrace thread_backtrace(*reader, thread_info);
                        auto tid = thread_info.tid; // Note: Yoinking this out of the struct because we can't pass a reference to it (as it's a misaligned field in a packed struct)
                        dbgln("Thread {}", tid);
                        for (auto const& entry : thread_backtrace.entries())
                            dbgln("- {}", entry.to_byte_string(true));
                        return IterationDecision::Continue;
                    });
                    break;
                }
            }
            if (m_unlink_coredumps && coredump_path.has_value())
                (void)Core::System::unlink(coredump_path.value());
        }
    } else {
        print_modifiers({ Test::BG_GREEN, Test::FG_BLACK, Test::FG_BOLD });
        out(" PASS  ");
        print_modifiers({ Test::CLEAR });
    }

    out(" {}", test_relative_path);

    print_modifiers({ Test::CLEAR, Test::ITALIC, Test::FG_GRAY });
    if (test_result.time_taken < 1000) {
        outln(" ({}ms)", static_cast<int>(test_result.time_taken));
    } else {
        outln(" ({:3}s)", test_result.time_taken / 1000.0);
    }
    print_modifiers({ Test::CLEAR });

    if (test_result.result != Test::Result::Pass) {
        print_modifiers({ Test::FG_GRAY, Test::FG_BOLD });
        out("         Test:   ");
        if (crashed_or_failed) {
            print_modifiers({ Test::CLEAR, Test::FG_RED });
            outln("{} ({})", test_result.file_path.basename(), test_result.result == Test::Result::Fail ? "failed" : "crashed");
        } else {
            print_modifiers({ Test::CLEAR, Test::FG_ORANGE });
            auto const status = test_result.result == Test::Result::Skip ? "skipped"sv : "expected fail"sv;
            outln("{} ({})", test_result.file_path.basename(), status);
        }
        print_modifiers({ Test::CLEAR });
    }

    // Make sure our clear modifiers goes through before we dump file output via write(2)
    fflush(stdout);

    if (print_stdout_stderr && test_result.stdout_err_fd > 0) {
        int ret = lseek(test_result.stdout_err_fd, 0, SEEK_SET);
        VERIFY(ret == 0);
        for (;;) {
            char buf[32768];
            ssize_t nread = read(test_result.stdout_err_fd, buf, sizeof(buf));
            if (nread == 0)
                break;
            if (nread < 0) {
                perror("read");
                break;
            }
            size_t already_written = 0;
            while (already_written < (size_t)nread) {
                ssize_t nwritten = write(STDOUT_FILENO, buf + already_written, nread - already_written);
                if (nwritten < 0) {
                    perror("write");
                    break;
                }
                already_written += nwritten;
            }
        }
    }

    close(test_result.stdout_err_fd);
}

FileResult TestRunner::run_test_file(ByteString const& test_path)
{
    double start_time = get_time_in_ms();

    auto path_for_test = LexicalPath(test_path);
    if (should_skip_test(path_for_test)) {
        return FileResult { move(path_for_test), 0.0, Test::Result::Skip, -1 };
    }

    // FIXME: actual error handling, mark test as :yaksplode: if any are bad instead of VERIFY
    posix_spawn_file_actions_t file_actions;
    posix_spawn_file_actions_init(&file_actions);
    char child_out_err_path[] = "/tmp/run-tests.XXXXXX";
    int child_out_err_file = mkstemp(child_out_err_path);
    VERIFY(child_out_err_file >= 0);

    ByteString dirname = path_for_test.dirname();
    ByteString basename = path_for_test.basename();

    (void)posix_spawn_file_actions_adddup2(&file_actions, child_out_err_file, STDOUT_FILENO);
    (void)posix_spawn_file_actions_adddup2(&file_actions, child_out_err_file, STDERR_FILENO);
    (void)posix_spawn_file_actions_addchdir(&file_actions, dirname.characters());

    Vector<char const*, 4> argv;
    argv.append(basename.characters());
    auto extra_args = m_config->read_entry(path_for_test.basename(), "Arguments", "").split(' ');
    for (auto& arg : extra_args)
        argv.append(arg.characters());
    argv.append(nullptr);

    pid_t child_pid = -1;
    // FIXME: Do we really want to copy test runner's entire env?
    int ret = posix_spawn(&child_pid, test_path.characters(), &file_actions, nullptr, const_cast<char* const*>(argv.data()), environ);
    VERIFY(ret == 0);
    VERIFY(child_pid > 0);

    int wstatus;

    Test::Result test_result = Test::Result::Fail;
    for (size_t num_waits = 0; num_waits < 2; ++num_waits) {
        ret = waitpid(child_pid, &wstatus, 0); // intentionally not setting WCONTINUED
        if (ret != child_pid)
            break; // we'll end up with a failure

        if (WIFEXITED(wstatus)) {
            if (WEXITSTATUS(wstatus) == 0) {
                test_result = Test::Result::Pass;
            }
            break;
        } else if (WIFSIGNALED(wstatus)) {
            test_result = Test::Result::Crashed;
            break;
        } else if (WIFSTOPPED(wstatus)) {
            outln("{} was stopped unexpectedly, sending SIGCONT", test_path);
            kill(child_pid, SIGCONT);
        }
    }

    // Remove the child's stdout from /tmp. This does cause the temp file to be observable
    // while the test is executing, but if it hangs that might even be a bonus :)
    ret = unlink(child_out_err_path);
    VERIFY(ret == 0);

    return FileResult { move(path_for_test), get_time_in_ms() - start_time, test_result, child_out_err_file, child_pid };
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{

    auto program_name = LexicalPath::basename(arguments.strings[0]);

#ifdef SIGINFO
    TRY(Core::System::signal(SIGINFO, [](int) {
        static char buffer[4096];
        auto& counts = ::Test::TestRunner::the()->counts();
        int len = snprintf(buffer, sizeof(buffer), "Pass: %d, Fail: %d, Skip: %d\nCurrent test: %s\n", counts.tests_passed, counts.tests_failed, counts.tests_skipped, g_currently_running_test.characters());
        write(STDOUT_FILENO, buffer, len);
    }));
#endif

    bool print_progress =
#ifdef AK_OS_SERENITY
        true; // Use OSC 9 to print progress
#else
        false;
#endif
    bool print_json = false;
    bool print_all_output = false;
    bool run_benchmarks = false;
    bool run_skipped_tests = false;
    bool unlink_coredumps = false;
    StringView specified_test_root;
    ByteString test_glob;
    ByteString exclude_pattern;
    ByteString config_file;

    Core::ArgsParser args_parser;
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Show progress with OSC 9 (true, false)",
        .long_name = "show-progress",
        .short_name = 'p',
        .accept_value = [&](StringView str) {
            if ("true"sv == str)
                print_progress = true;
            else if ("false"sv == str)
                print_progress = false;
            else
                return false;
            return true;
        },
    });
    args_parser.add_option(print_json, "Show results as JSON", "json", 'j');
    args_parser.add_option(print_all_output, "Show all test output", "verbose", 'v');
    args_parser.add_option(run_benchmarks, "Run benchmarks as well", "benchmarks", 'b');
    args_parser.add_option(run_skipped_tests, "Run all matching tests, even those marked as 'skip'", "all", 'a');
    args_parser.add_option(unlink_coredumps, "Unlink coredumps after printing backtraces", "unlink-coredumps");
    args_parser.add_option(test_glob, "Only run tests matching the given glob", "filter", 'f', "glob");
    args_parser.add_option(exclude_pattern, "Regular expression to use to exclude paths from being considered tests", "exclude-pattern", 'e', "pattern");
    args_parser.add_option(config_file, "Configuration file to use", "config-file", 'c', "filename");
    args_parser.add_positional_argument(specified_test_root, "Tests root directory", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    test_glob = ByteString::formatted("*{}*", test_glob);

    if (Core::Environment::has("DISABLE_DBG_OUTPUT"sv))
        AK::set_debug_enabled(false);

    // Make UBSAN deadly for all tests we run by default.
    TRY(Core::Environment::set("UBSAN_OPTIONS"sv, "halt_on_error=1"sv, Core::Environment::Overwrite::Yes));

    if (!run_benchmarks)
        TRY(Core::Environment::set("TESTS_ONLY"sv, "1"sv, Core::Environment::Overwrite::Yes));

    ByteString test_root;

    if (!specified_test_root.is_empty()) {
        test_root = ByteString { specified_test_root };
    } else {
        test_root = "/usr/Tests";
    }
    if (!FileSystem::is_directory(test_root)) {
        warnln("Test root is not a directory: {}", test_root);
        return 1;
    }

    test_root = TRY(FileSystem::real_path(test_root));

    auto void_or_error = Core::System::chdir(test_root);
    if (void_or_error.is_error()) {
        warnln("chdir failed: {}", void_or_error.error());
        return void_or_error.release_error();
    }

    auto config_or_error = config_file.is_empty() ? Core::ConfigFile::open_for_app("Tests") : Core::ConfigFile::open(config_file);
    if (config_or_error.is_error()) {
        warnln("Failed to open configuration file ({}): {}", config_file.is_empty() ? "User config for Tests" : config_file.characters(), config_or_error.error());
        return config_or_error.release_error();
    }
    auto config = config_or_error.release_value();

    if (config->num_groups() == 0)
        warnln("Empty configuration file ({}) loaded!", config_file.is_empty() ? "User config for Tests" : config_file.characters());

    if (exclude_pattern.is_empty())
        exclude_pattern = config->read_entry("Global", "NotTestsPattern", "$^"); // default is match nothing (aka match end then beginning)

    Regex<PosixExtended> exclude_regex(exclude_pattern, {});
    if (exclude_regex.parser_result.error != regex::Error::NoError) {
        warnln("Exclude pattern \"{}\" is invalid", exclude_pattern);
        return 1;
    }

    // we need to preconfigure this, because we can't autoinitialize Regex types
    // in the Testrunner
    auto skip_regex_pattern = config->read_entry("Global", "SkipRegex", "$^");
    Regex<PosixExtended> skip_regex { skip_regex_pattern, {} };
    if (skip_regex.parser_result.error != regex::Error::NoError) {
        warnln("SkipRegex pattern \"{}\" is invalid", skip_regex_pattern);
        return 1;
    }

    TestRunner test_runner(test_root, move(exclude_regex), move(config), move(skip_regex), run_skipped_tests, print_progress, print_json, print_all_output, unlink_coredumps);
    test_runner.run(test_glob);

    return test_runner.counts().tests_failed;
}
