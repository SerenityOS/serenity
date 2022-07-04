/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

static void signal_handler(int)
{
    VERIFY_NOT_REACHED();
}

TEST_CASE(default_handlers)
{
    struct sigaction current_action { };

    int rc = sigaction(SIGUSR2, nullptr, &current_action);

    EXPECT_EQ(rc, 0);
    EXPECT_EQ(current_action.sa_handler, SIG_DFL);
}

TEST_CASE(handlers_after_fork)
{
    struct sigaction new_action {
        { signal_handler }, 0, 0
    };
    int rc = sigaction(SIGUSR2, &new_action, nullptr);
    EXPECT_EQ(rc, 0);

    pid_t pid = fork();

    if (pid == 0) {
        struct sigaction current_action { };
        rc = sigaction(SIGUSR2, nullptr, &current_action);
        EXPECT_EQ(rc, 0);
        EXPECT_EQ(current_action.sa_handler, signal_handler);
        exit(rc == 0 && current_action.sa_handler == signal_handler ? EXIT_SUCCESS : EXIT_FAILURE);
    } else {
        int exit_status = 0;
        rc = waitpid(pid, &exit_status, 0);
        EXPECT_EQ(rc, pid);
        EXPECT(WIFEXITED(exit_status));
        EXPECT_EQ(WEXITSTATUS(exit_status), 0);
    }
}

TEST_CASE(handlers_after_exec)
{
    struct sigaction new_action {
        { signal_handler }, 0, 0
    };
    int rc = sigaction(SIGUSR2, &new_action, nullptr);
    EXPECT_EQ(rc, 0);

    pid_t pid = fork();

    if (pid == 0) {
        // Hide the confusing "Running 1 cases out of 3" output.
        freopen("/dev/null", "w", stdout);

        // This runs the 'default_handlers' test in this binary again, but after exec.
        execl("/proc/self/exe", "TestSigHandler", "default_handlers", nullptr);
        FAIL("Failed to exec.");
    } else {
        int exit_status = 0;
        rc = waitpid(pid, &exit_status, 0);
        EXPECT_EQ(rc, pid);
        EXPECT(WIFEXITED(exit_status));
        EXPECT_EQ(WEXITSTATUS(exit_status), 0);
    }
}
