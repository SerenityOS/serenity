/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <pthread.h>
#include <unistd.h>

#define TEST_CASE_IN_PTHREAD(x)                                                 \
    static void* __TESTCASE_FUNC(x##__inner)(void*);                            \
    TEST_CASE(x)                                                                \
    {                                                                           \
        pthread_t thread;                                                       \
        pthread_create(&thread, nullptr, __TESTCASE_FUNC(x##__inner), nullptr); \
        pthread_join(thread, nullptr);                                          \
    }                                                                           \
    static void* __TESTCASE_FUNC(x##__inner)(void*)

TEST_CASE_IN_PTHREAD(cancel_state_valid)
{
    int old_state = 0;

    // Ensure that we return the default state correctly.
    EXPECT_EQ(pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &old_state), 0);
    EXPECT_EQ(old_state, PTHREAD_CANCEL_ENABLE);

    // Make sure that PTHREAD_CANCEL_DISABLE sticks.
    EXPECT_EQ(pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, &old_state), 0);
    EXPECT_EQ(old_state, PTHREAD_CANCEL_DISABLE);

    return nullptr;
}

TEST_CASE_IN_PTHREAD(cancel_state_invalid)
{
    constexpr int lower_invalid_state = min(PTHREAD_CANCEL_ENABLE, PTHREAD_CANCEL_DISABLE) - 1;
    constexpr int upper_invalid_state = max(PTHREAD_CANCEL_ENABLE, PTHREAD_CANCEL_DISABLE) + 1;

    int old_state = 0;

    // Check that both invalid states are rejected and don't change the old state.
    EXPECT_EQ(pthread_setcancelstate(lower_invalid_state, &old_state), EINVAL);
    EXPECT_EQ(old_state, 0);
    EXPECT_EQ(pthread_setcancelstate(upper_invalid_state, &old_state), EINVAL);
    EXPECT_EQ(old_state, 0);

    // Ensure that we are still in the default state afterwards.
    EXPECT_EQ(pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, &old_state), 0);
    EXPECT_EQ(old_state, PTHREAD_CANCEL_ENABLE);

    return nullptr;
}

TEST_CASE_IN_PTHREAD(cancel_type_valid)
{
    int old_type = 0;

    // Ensure that we return the default type correctly.
    EXPECT_EQ(pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, &old_type), 0);
    EXPECT_EQ(old_type, PTHREAD_CANCEL_DEFERRED);

    // Make sure that PTHREAD_CANCEL_ASYNCHRONOUS sticks (not that it should ever be used).
    EXPECT_EQ(pthread_setcanceltype(PTHREAD_CANCEL_DEFERRED, &old_type), 0);
    EXPECT_EQ(old_type, PTHREAD_CANCEL_ASYNCHRONOUS);

    return nullptr;
}

TEST_CASE_IN_PTHREAD(cancel_type_invalid)
{
    constexpr int lower_invalid_type = min(PTHREAD_CANCEL_DEFERRED, PTHREAD_CANCEL_ASYNCHRONOUS) - 1;
    constexpr int upper_invalid_type = max(PTHREAD_CANCEL_DEFERRED, PTHREAD_CANCEL_ASYNCHRONOUS) + 1;

    int old_type = 0;

    // Check that both invalid types are rejected and don't change the old type.
    EXPECT_EQ(pthread_setcanceltype(lower_invalid_type, &old_type), EINVAL);
    EXPECT_EQ(old_type, 0);
    EXPECT_EQ(pthread_setcanceltype(upper_invalid_type, &old_type), EINVAL);
    EXPECT_EQ(old_type, 0);

    // Ensure that we are still in the default state afterwards.
    EXPECT_EQ(pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, &old_type), 0);
    EXPECT_EQ(old_type, PTHREAD_CANCEL_DEFERRED);

    return nullptr;
}

static void cancel_clenaup_handler(void* data)
{
    (*static_cast<bool*>(data)) = true;
}

static void* cancel_inner(void* data)
{
    pthread_cleanup_push(cancel_clenaup_handler, data);

    pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, nullptr);
    pthread_setcanceltype(PTHREAD_CANCEL_DEFERRED, nullptr);

    // Sleep for a second until the other side sets up their end of the check,
    // then do a call to write, which should be a cancellation point.
    sleep(1);
    write(STDOUT_FILENO, nullptr, 0);

    pthread_exit(nullptr);
}

TEST_CASE(cancel)
{
    pthread_t thread;

    bool called_cleanup_handler = false;
    pthread_create(&thread, nullptr, cancel_inner, &called_cleanup_handler);

    int rc = pthread_cancel(thread);

    void* exit_code;
    pthread_join(thread, &exit_code);

    EXPECT_EQ(rc, 0);
    EXPECT_EQ(called_cleanup_handler, true);
    EXPECT_EQ(exit_code, PTHREAD_CANCELED);
}
