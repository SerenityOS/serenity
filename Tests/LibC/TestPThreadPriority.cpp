/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <pthread.h>
#include <sched.h>
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

TEST_CASE_IN_PTHREAD(basic_priority)
{
    auto min_priority = sched_get_priority_min(0);
    auto max_priority = sched_get_priority_max(0);
    sched_param const min_priority_parameter { .sched_priority = min_priority };
    sched_param const max_priority_parameter { .sched_priority = max_priority };

    auto rc = pthread_setschedparam(0, 0, &min_priority_parameter);
    EXPECT_EQ(rc, 0);
    sched_param output_parameter;
    rc = pthread_getschedparam(0, 0, &output_parameter);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(output_parameter.sched_priority, min_priority);

    rc = pthread_setschedparam(0, 0, &max_priority_parameter);
    EXPECT_EQ(rc, 0);
    rc = pthread_getschedparam(0, 0, &output_parameter);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(output_parameter.sched_priority, max_priority);

    rc = pthread_setschedparam(0, 0, &max_priority_parameter);
    EXPECT_EQ(rc, 0);
    return 0;
}

TEST_CASE_IN_PTHREAD(invalid_arguments)
{
    auto min_priority = sched_get_priority_min(0);
    auto max_priority = sched_get_priority_max(0);
    sched_param const under_priority_parameter { .sched_priority = min_priority - 1 };
    sched_param const over_priority_parameter { .sched_priority = max_priority + 1 };
    sched_param const min_priority_parameter { .sched_priority = min_priority };

    // Set too high or too low priorities.
    auto rc = pthread_setschedparam(0, 0, &over_priority_parameter);
    EXPECT_EQ(rc, EINVAL);
    rc = pthread_setschedparam(0, 0, &under_priority_parameter);
    EXPECT_EQ(rc, EINVAL);

    // Get and set a thread that doesn't exist.
    rc = pthread_setschedparam(-42069, 0, &min_priority_parameter);
    EXPECT_EQ(rc, ESRCH);
    sched_param output_parameter;
    rc = pthread_getschedparam(-42069, 0, &output_parameter);
    EXPECT_EQ(rc, ESRCH);
    return 0;
}
