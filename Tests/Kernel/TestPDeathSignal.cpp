/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibTest/TestCase.h>
#include <signal.h>
#include <stdint.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <unistd.h>

struct SharedState {
    volatile sig_atomic_t child_ready;
    volatile sig_atomic_t setup_succeeded;
    volatile sig_atomic_t received_signal;
    volatile sig_atomic_t child_finished;
    volatile pid_t child_pid;
};

static volatile sig_atomic_t* s_received_signal;

static void handle_pdeath_signal(int signal)
{
    *s_received_signal = signal;
}

static bool wait_for_flag(volatile sig_atomic_t const& flag)
{
    auto start_time = MonotonicTime::now();
    while (flag == 0) {
        if ((MonotonicTime::now() - start_time).to_truncated_seconds() >= 5)
            return false;
    }
    return true;
}

TEST_CASE(parent_death_signal_is_delivered)
{
    auto* state = static_cast<SharedState*>(mmap(nullptr, sizeof(SharedState), PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0));
    VERIFY(state != MAP_FAILED);

    state->child_ready = 0;
    state->setup_succeeded = 0;
    state->received_signal = 0;
    state->child_finished = 0;
    state->child_pid = -1;
    s_received_signal = &state->received_signal;

    pid_t intermediate_parent_pid = fork();
    VERIFY(intermediate_parent_pid != -1);

    if (intermediate_parent_pid == 0) {
        pid_t child_pid = fork();
        if (child_pid == -1) {
            state->child_ready = 1;
            _exit(1);
        }

        if (child_pid == 0) {
            state->child_pid = getpid();
            auto expected_parent_pid = getppid();

            bool setup_succeeded = signal(SIGUSR1, handle_pdeath_signal) != SIG_ERR;
            setup_succeeded &= prctl(PR_SET_PDEATHSIG, static_cast<uintptr_t>(SIGUSR1), uintptr_t { 0 }, uintptr_t { 0 }) == 0;

            int configured_signal = 0;
            setup_succeeded &= prctl(PR_GET_PDEATHSIG, reinterpret_cast<uintptr_t>(&configured_signal), uintptr_t { 0 }, uintptr_t { 0 }) == 0;
            setup_succeeded &= configured_signal == SIGUSR1;
            setup_succeeded &= getppid() == expected_parent_pid;

            state->setup_succeeded = setup_succeeded;
            state->child_ready = 1;
            if (!setup_succeeded)
                _exit(1);

            while (state->received_signal == 0)
                pause();

            state->child_finished = 1;
            _exit(0);
        }

        if (!wait_for_flag(state->child_ready))
            _exit(2);
        _exit(0);
    }

    int intermediate_parent_status = 0;
    pid_t waited_pid = waitpid(intermediate_parent_pid, &intermediate_parent_status, 0);
    if (waited_pid != intermediate_parent_pid || !WIFEXITED(intermediate_parent_status) || WEXITSTATUS(intermediate_parent_status) != 0) {
        if (state->child_pid > 0)
            (void)kill(state->child_pid, SIGKILL);
        FAIL("Intermediate parent did not exit cleanly");
        return;
    }

    if (!state->setup_succeeded) {
        if (state->child_pid > 0)
            (void)kill(state->child_pid, SIGKILL);
        FAIL("Child failed to configure its parent-death signal");
        return;
    }

    if (!wait_for_flag(state->received_signal)) {
        if (state->child_pid > 0)
            (void)kill(state->child_pid, SIGKILL);
        FAIL("Child did not receive SIGUSR1 when its parent exited");
        return;
    }

    EXPECT_EQ(state->received_signal, SIGUSR1);
    if (!wait_for_flag(state->child_finished)) {
        if (state->child_pid > 0)
            (void)kill(state->child_pid, SIGKILL);
        FAIL("Child did not finish after handling its parent-death signal");
        return;
    }

    EXPECT_EQ(munmap(state, sizeof(SharedState)), 0);
}
