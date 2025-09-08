/*
 * Copyright (c) 2025, Jake Knoth <jakek1406@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

static bool volatile g_interrupted = false;
static void handle_sigint(int)
{
    g_interrupted = true;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    double secs;
    Vector<StringView> command_and_args;
    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_positional_argument(secs, "Time limit in seconds", "secs", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(command_and_args, "Command and arguments to be run",
        "command", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);
    Vector<ByteString> argv_storage;
    argv_storage.ensure_capacity(command_and_args.size());
    for (auto sv : command_and_args) {
        argv_storage.append(sv.to_byte_string());
    }
    Vector<char*> argv_ptrs;
    argv_ptrs.ensure_capacity(argv_storage.size() + 1);
    for (auto& bs : argv_storage) {
        argv_ptrs.append(const_cast<char*>(bs.characters()));
    }
    argv_ptrs.append(nullptr);
    if (argv_ptrs.size() < 2) {
        // error: no command to run
        return 125;
    }
    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);

    if (secs < 0) {
        perror("negative timeout");
        return 125;
    }
    TRY(Core::System::pledge("stdio proc exec sigaction"));
    double whole_seconds = static_cast<time_t>(secs);
    double fraction = secs - whole_seconds;
    timespec requested_timeout {
        .tv_sec = static_cast<time_t>(whole_seconds),
        .tv_nsec = static_cast<long>(fraction * (double)1000000000),
    };
    timespec tmp {};
    clock_gettime(CLOCK_MONOTONIC, &tmp);
    timespec deadline = {
        .tv_sec = tmp.tv_sec + requested_timeout.tv_sec,
        .tv_nsec = tmp.tv_nsec + requested_timeout.tv_nsec,
    };
    if (deadline.tv_nsec >= 1'000'000'000) {
        deadline.tv_sec++;
        deadline.tv_nsec -= 1'000'000'000;
    }

    pid_t child_pid = fork();
    if (child_pid < 0) {
        perror("fork");
        return 125;
    }
    if (child_pid == 0) {
        setpgid(0, 0);
        execvp(argv_ptrs[0], argv_ptrs.data());
        perror("execvp");
        if (errno == ENOENT) {
            _exit(127);
        }
        _exit(126);
    } else {
        int status = 0;
        bool timed_out = false;
        auto timespec_compare = [](timespec const& a, timespec const& b) {
            return (a.tv_sec > b.tv_sec) || (a.tv_sec == b.tv_sec && a.tv_nsec >= b.tv_nsec);
        };
        setpgid(child_pid, child_pid);
        while (true) {
            pid_t w = waitpid(child_pid, &status, WNOHANG);
            if (w == child_pid) {
                break;
            }
            if (w < 0 && errno != EINTR) {
                perror("waitpid");
                // try to clean up
                kill(-child_pid, SIGKILL);
                waitpid(child_pid, &status, 0);
                return 125;
            }
            timespec now {};
            clock_gettime(CLOCK_MONOTONIC, &now);
            if (timespec_compare(now, deadline)) {
                timed_out = true;
                kill(-child_pid, SIGTERM);
                timespec grace { 1, 0 };
                nanosleep(&grace, nullptr);
                w = waitpid(child_pid, &status, WNOHANG);
                if (w == 0) {
                    kill(-child_pid, SIGKILL);
                    waitpid(child_pid, &status, 0);
                }
                break;
            }
            if (g_interrupted) {
                kill(-child_pid, SIGINT);
                waitpid(child_pid, &status, 0);
                TRY(Core::System::signal(SIGINT, SIG_DFL));
                raise(SIGINT);
                __builtin_unreachable();
            }
            timespec spin { 0, 25'000'000 };
            nanosleep(&spin, nullptr);
        }
        if (timed_out)
            return 124;
        if (WIFEXITED(status))
            return WEXITSTATUS(status);
        if (WIFSIGNALED(status))
            return 128 + WTERMSIG(status);
        return 125;
    }
    return 0;
}
