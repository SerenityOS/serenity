/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <sys/ptrace.h>
#include <unistd.h>

static Atomic<bool> s_exit_thread = false;

static void* thread_entry(void*)
{
    while (!s_exit_thread)
        continue;

    return nullptr;
}

TEST_CASE(ptrace_self_attach_fail)
{
    pthread_t thread = 0;
    VERIFY(pthread_create(&thread, nullptr, thread_entry, nullptr) == 0);
    VERIFY(thread > 0);

    int ptrace_return = ptrace(PT_ATTACH, thread, 0, 0);
    int error = errno;
    EXPECT_EQ(ptrace_return, -1);
    EXPECT_EQ(error, EPERM);

    s_exit_thread = true;
    VERIFY(pthread_join(thread, nullptr) == 0);
}
