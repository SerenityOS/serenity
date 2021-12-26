/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>
#include <LibTest/TestRunner.h>
#include <signal.h>
#include <spawn.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Test {
TestRunner* TestRunner::s_the = nullptr;
}
using enum Test::Modifier;
using Test::get_time_in_ms;
using Test::print_modifiers;

struct FileResult {
    LexicalPath file_path;
    double time_taken { 0 };
    Test::Result result { Test::Result::Pass };
    int stdout_err_fd { -1 };
};

String g_currently_running_test;

class TestRunner : public ::Test::TestRunner {
public:
    TestRunner(String test_root, Regex<PosixExtended> exclude_regex, NonnullRefPtr<Core::ConfigFile> config, bool print_progress, bool print_json, bool print_all_output, bool print_times = true)
        : ::Test::TestRunner(move(test_root), print_times, print_progress, print_json)
        , m_exclude_regex(move(exclude_regex))
        , m_config(move(config))
        , m_print_all_output(print_all_output)
    {
        m_skip_directories = m_config->read_entry("Global", "SkipDirectories", "").split(' ');
        m_skip_files = m_config->read_entry("Global", "SkipTests", "").split(' ');
    }

    virtual ~TestRunner() = default;

protected:
    virtual void do_run_single_test(const String& test_path) override;
    virtual Vector<String> get_test_paths() const override;

    virtual FileResult run_test_file(const String& test_path);

    bool should_skip_test(const LexicalPath& test_path);

    Regex<PosixExtended> m_exclude_regex;
    NonnullRefPtr<Core::ConfigFile> m_config;
    Vector<String> m_skip_directories;
    Vector<String> m_skip_files;
    bool m_print_all_output { false };
};

Vector<String> TestRunner::get_test_paths() const
{
    Vector<String> paths;
    Test::iterate_directory_recursively(m_test_root, [&](const String& file_path) {
        if (access(file_path.characters(), R_OK | X_OK) != 0)
            return;
        auto result = m_exclude_regex.match(file_path, PosixFlags::Global);
        if (!result.success) // must NOT match the regex to be a valid test file
            paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

bool TestRunner::should_skip_test(const LexicalPath& test_path)
{
    for (const String& dir : m_skip_directories) {
        if (test_path.dirname().contains(dir))
            return true;
    }
    for (const String& file : m_skip_files) {
        if (test_path.basename().contains(file))
            return true;
    }
    return false;
}

void TestRunner::do_run_single_test(const String& test_path)
{
    g_currently_running_test = test_path;

    auto test_result = run_test_file(test_path);

    switch (test_result.result) {
    case Test::Result::Pass:
        ++m_counts.tests_passed;
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
        print_modifiers({ BG_RED, FG_BLACK, FG_BOLD });
        out("{}", test_result.result == Test::Result::Fail ? " FAIL  " : "CRASHED");
        print_modifiers({ CLEAR });
    } else {
        print_modifiers({ BG_GREEN, FG_BLACK, FG_BOLD });
        out(" PASS ");
        print_modifiers({ CLEAR });
    }

    out(" {}", LexicalPath::relative_path(test_path, m_test_root));

    print_modifiers({ CLEAR, ITALIC, FG_GRAY });
    if (test_result.time_taken < 1000) {
        outln(" ({}ms)", static_cast<int>(test_result.time_taken));
    } else {
        outln(" ({:3}s)", test_result.time_taken / 1000.0);
    }
    print_modifiers({ CLEAR });

    if (test_result.result != Test::Result::Pass) {
        print_modifiers({ FG_GRAY, FG_BOLD });
        out("         Test:   ");
        if (crashed_or_failed) {
            print_modifiers({ CLEAR, FG_RED });
            outln("{} ({})", test_result.file_path.basename(), test_result.result == Test::Result::Fail ? "failed" : "crashed");
        } else {
            print_modifiers({ CLEAR, FG_ORANGE });
            outln("{} (skipped)", test_result.file_path.basename());
        }
        print_modifiers({ CLEAR });
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

FileResult TestRunner::run_test_file(const String& test_path)
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

    int ret = unlink(child_out_err_path);
    VERIFY(ret == 0);

    (void)posix_spawn_file_actions_adddup2(&file_actions, child_out_err_file, STDOUT_FILENO);
    (void)posix_spawn_file_actions_adddup2(&file_actions, child_out_err_file, STDERR_FILENO);
    (void)posix_spawn_file_actions_addchdir(&file_actions, path_for_test.dirname().characters());

    Vector<const char*, 4> argv;
    argv.append(path_for_test.basename().characters());
    auto extra_args = m_config->read_entry(path_for_test.basename(), "Arguments", "").split(' ');
    for (auto& arg : extra_args)
        argv.append(arg.characters());
    argv.append(nullptr);

    pid_t child_pid = -1;
    // FIXME: Do we really want to copy test runner's entire env?
    ret = posix_spawn(&child_pid, test_path.characters(), &file_actions, nullptr, const_cast<char* const*>(argv.data()), environ);
    VERIFY(ret == 0);
    VERIFY(child_pid > 0);

    int wstatus;

    Test::Result test_result = Test::Result::Fail;
    for (size_t num_waits = 0; num_waits < 2; ++num_waits) {
        ret = waitpid(child_pid, &wstatus, 0); // intentionally not setting WCONTINUED
        if (ret != child_pid)
            break; // we'll end up with a failure

        if (WIFEXITED(wstatus)) {
            if (wstatus == 0) {
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
    return FileResult { move(path_for_test), get_time_in_ms() - start_time, test_result, child_out_err_file };
}

int main(int argc, char** argv)
{
    auto program_name = LexicalPath { argv[0] }.basename();

#ifdef SIGINFO
    signal(SIGINFO, [](int) {
        static char buffer[4096];
        auto& counts = ::Test::TestRunner::the()->counts();
        int len = snprintf(buffer, sizeof(buffer), "Pass: %d, Fail: %d, Skip: %d\nCurrent test: %s\n", counts.tests_passed, counts.tests_failed, counts.tests_skipped, g_currently_running_test.characters());
        write(STDOUT_FILENO, buffer, len);
    });
#endif

    bool print_progress =
#ifdef __serenity__
        true; // Use OSC 9 to print progress
#else
        false;
#endif
    bool print_json = false;
    bool print_all_output = false;
    const char* specified_test_root = nullptr;
    String test_glob;
    String exclude_pattern;
    String config_file;

    Core::ArgsParser args_parser;
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Show progress with OSC 9 (true, false)",
        .long_name = "show-progress",
        .short_name = 'p',
        .accept_value = [&](auto* str) {
            if (StringView { "true" } == str)
                print_progress = true;
            else if (StringView { "false" } == str)
                print_progress = false;
            else
                return false;
            return true;
        },
    });
    args_parser.add_option(print_json, "Show results as JSON", "json", 'j');
    args_parser.add_option(print_all_output, "Show all test output", "verbose", 'v');
    args_parser.add_option(test_glob, "Only run tests matching the given glob", "filter", 'f', "glob");
    args_parser.add_option(exclude_pattern, "Regular expression to use to exclude paths from being considered tests", "exclude-pattern", 'e', "pattern");
    args_parser.add_option(config_file, "Configuration file to use", "config-file", 'c', "filename");
    args_parser.add_positional_argument(specified_test_root, "Tests root directory", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    test_glob = String::formatted("*{}*", test_glob);

    if (getenv("DISABLE_DBG_OUTPUT")) {
        AK::set_debug_enabled(false);
    }

    String test_root;

    if (specified_test_root) {
        test_root = String { specified_test_root };
    } else {
        test_root = "/usr/Tests";
    }
    if (!Core::File::is_directory(test_root)) {
        warnln("Test root is not a directory: {}", test_root);
        return 1;
    }

    test_root = Core::File::real_path_for(test_root);

    if (chdir(test_root.characters()) < 0) {
        auto saved_errno = errno;
        warnln("chdir failed: {}", strerror(saved_errno));
        return 1;
    }

    auto config = config_file.is_empty() ? Core::ConfigFile::get_for_app("Tests") : Core::ConfigFile::open(config_file);
    if (config->num_groups() == 0)
        warnln("Empty configuration file ({}) loaded!", config_file.is_empty() ? "User config for Tests" : config_file.characters());

    if (exclude_pattern.is_empty())
        exclude_pattern = config->read_entry("Global", "NotTestsPattern", "$^"); // default is match nothing (aka match end then beginning)

    Regex<PosixExtended> exclude_regex(exclude_pattern, {});
    if (exclude_regex.parser_result.error != Error::NoError) {
        warnln("Exclude pattern \"{}\" is invalid", exclude_pattern);
        return 1;
    }

    TestRunner test_runner(test_root, move(exclude_regex), move(config), print_progress, print_json, print_all_output);
    test_runner.run(test_glob);

    return test_runner.counts().tests_failed;
}
