/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>

static bool volatile s_received_sigchld = false;

static void sigchld_handler(int)
{
    s_received_sigchld = true;
}

TEST_CASE(wait)
{
    signal(SIGCHLD, sigchld_handler);
    pid_t pid = fork();
    if (pid == 0) {
        exit(0);
        VERIFY_NOT_REACHED();
    }
    pid_t result = wait(NULL);
    EXPECT_EQ(result, pid);
    EXPECT_EQ(s_received_sigchld, true);
}
