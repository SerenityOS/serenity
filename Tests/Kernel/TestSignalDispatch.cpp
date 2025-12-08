/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibTest/TestCase.h>
#include <signal.h>
#include <sys/mman.h>
#include <unistd.h>

// These will eventually point to shared mmaps which will be used as a crude form of IPC.
static bool volatile* s_received_sigint = nullptr;
static bool volatile* s_ready_to_receive_signal = nullptr;

static void handle_sigint(int)
{
    *s_received_sigint = true;
}

TEST_CASE(signal_dispatch_to_spinning_thread)
{
    s_received_sigint = reinterpret_cast<bool*>(mmap(nullptr, sizeof(bool), PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0));
    s_ready_to_receive_signal = reinterpret_cast<bool*>(mmap(nullptr, sizeof(bool), PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0));

    VERIFY(s_received_sigint);
    VERIFY(s_ready_to_receive_signal);

    pid_t pid = fork();
    VERIFY(pid != -1);

    if (pid == 0) {
        VERIFY(signal(SIGINT, handle_sigint) != SIG_ERR);
        *s_ready_to_receive_signal = true;
        while (1)
            continue;
        VERIFY_NOT_REACHED();
    }

    auto start_time = MonotonicTime::now();
    while (*s_ready_to_receive_signal == false) {
        if ((MonotonicTime::now() - start_time).to_truncated_seconds() >= 5) {
            FAIL("Timed out while waiting for signal handler creation");
            return;
        }
    }

    VERIFY(kill(pid, SIGINT) == 0);

    start_time = MonotonicTime::now();
    while (*s_received_sigint == false) {
        if ((MonotonicTime::now() - start_time).to_truncated_seconds() >= 5) {
            FAIL("Timed out while waiting for SIGINT");
            return;
        }
    }

    // Normally this should be done in a ScopeGuard, but that's kind of moot here
    // because this signal likely won't be handled properly anyway if this test failed.
    VERIFY(kill(pid, SIGKILL) == 0);
}
