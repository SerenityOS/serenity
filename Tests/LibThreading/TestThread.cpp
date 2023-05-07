/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <LibThreading/Thread.h>
#include <unistd.h>

TEST_CASE(threads_can_detach)
{
    int should_be_42 = 0;

    auto thread = Threading::Thread::construct([&should_be_42]() {
        usleep(10 * 1000);
        should_be_42 = 42;
        return 0;
    });
    thread->start();
    thread->detach();
    usleep(20 * 1000);

    EXPECT(should_be_42 == 42);
}

TEST_CASE(joining_detached_thread_errors)
{
    Atomic<bool> should_exit { false };
    auto thread = Threading::Thread::construct([&]() {
        while (!should_exit.load())
            usleep(10 * 1000);
        return 0;
    });
    thread->start();
    thread->detach();

    // Because of how the crash test forks and removes the thread, we can't use that to verify that join() crashes. Instead, we check the join crash condition ourselves.
    EXPECT(!thread->needs_to_be_joined());

    // FIXME: Dropping a running thread crashes because of the Function destructor. For now, force the detached thread to exit.
    should_exit.store(true);
    usleep(20 * 1000);
}

TEST_CASE(join_dead_thread)
{
    auto thread = Threading::Thread::construct([&]() { return 0 /*nullptr*/; });
    thread->start();
    // The thread should have exited by then.
    usleep(40 * 1000);

    auto join_result = TRY_OR_FAIL(thread->join<int*>());
    EXPECT_EQ(join_result, static_cast<int*>(0));
}
