/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <LibThreading/Thread.h>
#include <unistd.h>

// FIXME: Enable these tests once they work reliably.

#if 0
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
    auto thread = Threading::Thread::construct([]() { return 0; });
    thread->start();
    thread->detach();

    EXPECT(thread->join().is_error());
}
#endif
