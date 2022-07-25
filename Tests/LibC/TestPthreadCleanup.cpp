/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <pthread.h>

static size_t exit_count = 0;

static void exit_count_test_handler(void* data)
{
    EXPECT_EQ(exit_count, reinterpret_cast<size_t>(data));
    exit_count++;
}

static void immediate_fail_handler(void*)
{
    FAIL("Called a cleanup handler");
}

static void* cleanup_pthread_exit_inner(void*)
{
    // Push handlers in reverse order as they are taken from the top of the stack on cleanup.
    pthread_cleanup_push(exit_count_test_handler, reinterpret_cast<void*>(2));
    pthread_cleanup_push(exit_count_test_handler, reinterpret_cast<void*>(1));
    pthread_cleanup_push(exit_count_test_handler, reinterpret_cast<void*>(0));

    pthread_exit(nullptr);
}

TEST_CASE(cleanup_pthread_exit)
{
    pthread_t thread;
    exit_count = 0;

    pthread_create(&thread, nullptr, cleanup_pthread_exit_inner, nullptr);

    pthread_join(thread, nullptr);

    // Ensure that all exit handlers have been called.
    EXPECT_EQ(exit_count, 3ul);
}

static void* cleanup_return_inner(void*)
{
    // Returning from the main function should not call any cleanup handlers.
    pthread_cleanup_push(immediate_fail_handler, nullptr);
    return nullptr;
}

TEST_CASE(cleanup_return)
{
    pthread_t thread;
    pthread_create(&thread, nullptr, cleanup_return_inner, nullptr);
    pthread_join(thread, nullptr);
}

static void* cleanup_pop_inner(void*)
{
    pthread_cleanup_push(exit_count_test_handler, reinterpret_cast<void*>(1));
    pthread_cleanup_push(immediate_fail_handler, nullptr);
    pthread_cleanup_push(exit_count_test_handler, reinterpret_cast<void*>(0));
    pthread_cleanup_push(immediate_fail_handler, nullptr);

    // Popping a cleanup handler shouldn't run the callback unless `execute` is given.
    pthread_cleanup_pop(0);
    pthread_cleanup_pop(1);
    pthread_cleanup_pop(0);
    pthread_cleanup_pop(1);

    return nullptr;
}

TEST_CASE(cleanup_pop)
{
    pthread_t thread;
    exit_count = 0;

    pthread_create(&thread, nullptr, cleanup_pop_inner, nullptr);
    pthread_join(thread, nullptr);

    // Ensure that all exit handlers have been called.
    EXPECT_EQ(exit_count, 2ul);
}
