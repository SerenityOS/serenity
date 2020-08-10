/*
 * Copyright (c) 2020, Sahan Fernando <sahan.h.fernando@gmail.com>
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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
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

static String build_header_string(const Vector<const char*>& command, const struct timeval& interval)
{
    StringBuilder builder;
    builder.appendf("Every %d", interval.tv_sec);
    builder.appendf(".%ds: \x1b[1m", interval.tv_usec / 100000);
    builder.join(' ', command);
    builder.append("\x1b[0m");
    return builder.build();
}

static struct timeval get_current_time()
{
    struct timespec ts;
    struct timeval tv;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    timespec_to_timeval(ts, tv);
    return tv;
}

static int64_t usecs_from(const struct timeval& start, const struct timeval& end)
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

static int run_command(const Vector<const char*>& command)
{
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
    ASSERT(exited_pid == child_pid);
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

int main(int argc, char** argv)
{
    signal(SIGINT, handle_signal);
    if (pledge("stdio proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> command;
    Core::ArgsParser args_parser;
    args_parser.add_option(opt_interval, "Amount of time between updates", "interval", 'n', "seconds");
    args_parser.add_option(flag_noheader, "Turn off the header describing the command and interval", "no-title", 't');
    args_parser.add_option(flag_beep_on_fail, "Beep if the command has a non-zero exit code", "beep", 'b');
    args_parser.add_positional_argument(command, "Command to run", "command");
    args_parser.parse(argc, argv);

    struct timeval interval;
    if (opt_interval <= 0) {
        interval = { 0, 100000 };
    } else {
        interval = { opt_interval, 0 };
    }

    auto header = build_header_string(command, interval);
    command.append(nullptr);

    auto now = get_current_time();
    auto next_run_time = now;
    while (true) {
        int usecs_to_sleep = usecs_from(now, next_run_time);
        while (usecs_to_sleep > 0) {
            usleep(usecs_to_sleep);
            now = get_current_time();
            usecs_to_sleep = usecs_from(now, next_run_time);
        }
        // Clear the screen, then reset the cursor position to the top left.
        fprintf(stderr, "\033[H\033[2J");
        // Print the header.
        if (!flag_noheader) {
            fprintf(stderr, "%s\n\n", header.characters());
        } else {
            fflush(stderr);
        }
        if (run_command(command) != 0) {
            exit_code = 1;
            if (flag_beep_on_fail) {
                fprintf(stderr, "\a");
                fflush(stderr);
            }
        }
        now = get_current_time();
        timeval_add(next_run_time, interval, next_run_time);
        if (usecs_from(now, next_run_time) < 0) {
            // The next execution is overdue, so we set next_run_time to now to prevent drift.
            next_run_time = now;
        }
    }
}
