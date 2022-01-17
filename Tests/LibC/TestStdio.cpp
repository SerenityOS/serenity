/*
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

// Test whether file writes are flushed to disk at program termination,
// even if we do not close the files or call fflush().
TEST_CASE(flush_on_exit)
{
    static constexpr auto test_str = "peekaboo";
    auto pid = fork();
    VERIFY(pid >= 0);

    if (pid == 0) {
        auto* fp = fopen("/tmp/flushtest", "w");
        VERIFY(fp != nullptr);
        auto nwritten = fwrite(test_str, 1, strlen(test_str), fp);
        VERIFY(nwritten != 0);
        // We intentionally leak `fp` here.
        exit(0);
    } else {
        int wstatus;
        auto rc = waitpid(pid, &wstatus, 0);
        VERIFY(rc >= 0);

        char buf[256];
        auto* fp = fopen("/tmp/flushtest", "r");
        VERIFY(fp != nullptr);
        auto* sp = fgets(buf, sizeof(buf), fp);
        VERIFY(sp != nullptr);
        fclose(fp);
        unlink("/tmp/flushtest");

        EXPECT_EQ(strcmp(test_str, buf), 0);
    }
}
