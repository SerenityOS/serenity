/*
 * Copyright (c) 2020, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

static int opt_interval = 2;
static bool flag_noheader = false;
static bool flag_beep_on_fail = false;
static volatile int exit_code = 0;
static volatile pid_t child_pid = -1;

static String build_header_string(Vector<char const*> const& command, struct timeval const& interval)
{
    StringBuilder builder;
    builder.appendff("Every {}.{}s: \x1b[1m", interval.tv_sec, interval.tv_usec / 100000);
    builder.join(' ', command);
    builder.append("\x1b[0m");
    return builder.build();
}

static String build_header_string(Vector<char const*> const& command, Vector<String> const& filenames)
{
    StringBuilder builder;
    builder.appendff("Every time any of {} changes: \x1b[1m", filenames);
    builder.join(' ', command);
    builder.append("\x1b[0m");
    return builder.build();
}

static struct timeval get_current_time()
{
    struct timespec ts;
    struct timeval tv;
    clock_gettime(CLOCK_MONOTONIC_COARSE, &ts);
    timespec_to_timeval(ts, tv);
    return tv;
}

static int64_t usecs_from(struct timeval const& start, struct timeval const& end)
{
    struct timeval diff;
    timeval_sub(end, start, diff);
    return 1000000 * diff.tv_sec + diff.tv_usec;
}

static void handle_signal(int signal)
{
    if (child_pid > 0) {
        if (kill(child_pid, signal) < 0) {
            perror("kill");
        }
        int status;
        if (waitpid(child_pid, &status, 0) < 0) {
            perror("waitpid");
        } else if (!WIFEXITED(status) || WEXITSTATUS(status) != 0) {
            exit_code = 1;
        }
    }
    exit(exit_code);
}

static int run_command(Vector<char const*> const& command)
{
    VERIFY(command[command.size() - 1] == nullptr);

    if ((errno = posix_spawnp(const_cast<pid_t*>(&child_pid), command[0], nullptr, nullptr, const_cast<char**>(command.data()), environ))) {
        exit_code = 1;
        perror("posix_spawn");
        return errno;
    }

    // Wait for the child to terminate, then return its exit code.
    int status;
    pid_t exited_pid;
    do {
        exited_pid = waitpid(child_pid, &status, 0);
    } while (exited_pid < 0 && errno == EINTR);
    VERIFY(exited_pid == child_pid);
    child_pid = -1;
    if (exited_pid < 0) {
        perror("waitpid");
        return 1;
    }
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else {
        return 1;
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::signal(SIGINT, handle_signal));
    TRY(Core::System::pledge("stdio proc exec rpath", nullptr));

    Vector<String> files_to_watch;
    Vector<char const*> command;
    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.set_general_help("Execute a command repeatedly, and watch its output over time.");
    args_parser.add_option(opt_interval, "Amount of time between updates", "interval", 'n', "seconds");
    args_parser.add_option(flag_noheader, "Turn off the header describing the command and interval", "no-title", 't');
    args_parser.add_option(flag_beep_on_fail, "Beep if the command has a non-zero exit code", "beep", 'b');
    Core::ArgsParser::Option file_arg {
        .requires_argument = true,
        .help_string = "Run command whenever this file changes. Can be used multiple times.",
        .long_name = "file",
        .short_name = 'f',
        .value_name = "file",
        .accept_value = [&files_to_watch](auto filename) {
            files_to_watch.append(filename);
            return true;
        }
    };
    args_parser.add_option(move(file_arg));
    args_parser.add_positional_argument(command, "Command to run", "command");
    args_parser.parse(arguments);

    command.append(nullptr);

    String header;

    auto watch_callback = [&] {
        // Clear the screen, then reset the cursor position to the top left.
        warn("\033[H\033[2J");
        // Print the header.
        if (!flag_noheader) {
            warnln("{}", header);
            warnln();
        } else {
            fflush(stderr);
        }
        if (run_command(command) != 0) {
            exit_code = 1;
            if (flag_beep_on_fail) {
                warnln("\a");
                fflush(stderr);
            }
        }
    };

    if (!files_to_watch.is_empty()) {
        header = build_header_string(command, files_to_watch);

        auto file_watcher = Core::BlockingFileWatcher();
        for (auto const& file : files_to_watch) {
            if (!Core::File::exists(file)) {
                warnln("Cannot watch '{}', it does not exist.", file);
                return 1;
            }
            if (!file_watcher.is_watching(file)) {
                auto could_add_to_watch = TRY(file_watcher.add_watch(file, Core::FileWatcherEvent::Type::MetadataModified));
                if (!could_add_to_watch) {
                    warnln("Could not add '{}' to watch list.", file);
                    return 1;
                }
            }
        }

        watch_callback();
        while (true) {
            auto maybe_event = file_watcher.wait_for_event();
            if (maybe_event.has_value()) {
                watch_callback();
            }
        }
    } else {
        TRY(Core::System::pledge("stdio proc exec", nullptr));

        struct timeval interval;
        if (opt_interval <= 0) {
            interval = { 0, 100000 };
        } else {
            interval = { opt_interval, 0 };
        }

        auto now = get_current_time();
        auto next_run_time = now;
        header = build_header_string(command, interval);
        while (true) {
            int usecs_to_sleep = usecs_from(now, next_run_time);
            while (usecs_to_sleep > 0) {
                usleep(usecs_to_sleep);
                now = get_current_time();
                usecs_to_sleep = usecs_from(now, next_run_time);
            }

            watch_callback();

            now = get_current_time();
            timeval_add(next_run_time, interval, next_run_time);
            if (usecs_from(now, next_run_time) < 0) {
                // The next execution is overdue, so we set next_run_time to now to prevent drift.
                next_run_time = now;
            }
        }
    }
}
