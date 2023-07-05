/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h>
#include <LibThreading/Thread.h>

// Defined in TLSDef.cpp
extern __thread int one;
extern __thread int two;
extern __thread int three;
[[gnu::tls_model("initial-exec")]] extern __thread int four;
extern void check_increment_worked();

static void check_initial()
{
    EXPECT_EQ(one, 1);
    EXPECT_EQ(two, 2);
    EXPECT_EQ(three, 3);
    EXPECT_EQ(four, 4);
}

// This checks the basic functionality of thread-local variables:
// - TLS variables with a static initializer have the correct value on program startup
// - TLS variables are set to their initial values in a new thread
// - relocations refer to the correct variables
// - accessing an initial-exec variable from a DSO works even if
//   it's not declared as initial-exec at the use site
// FIXME: Test C++11 thread_local variables with dynamic initializers
void run_test();
void run_test()
{
    check_initial();
    ++one;
    ++two;
    ++three;
    ++four;
    check_increment_worked();

    auto second_thread = Threading::Thread::construct([] {
        check_initial();
        return 0;
    });
    second_thread->start();
    (void)second_thread->join();
}
