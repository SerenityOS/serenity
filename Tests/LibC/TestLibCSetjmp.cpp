/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <setjmp.h>
#include <signal.h>
#include <string.h>

TEST_CASE(setjmp)
{
    jmp_buf env;
    int volatile set = 1;

    if (setjmp(env)) {
        EXPECT_EQ(set, 0);
        return;
    }

    EXPECT_EQ(set, 1);
    set = 0;
    longjmp(env, 1);

    VERIFY_NOT_REACHED();
}

TEST_CASE(setjmp_zero)
{
    jmp_buf env;
    int volatile set = 1;

    switch (setjmp(env)) {
    case 0:
        EXPECT_EQ(set, 1);
        set = 0;
        longjmp(env, 0);
        VERIFY_NOT_REACHED();

    case 1:
        EXPECT_EQ(set, 0);
        break;

    default:
        VERIFY_NOT_REACHED();
    };
}

TEST_CASE(setjmp_value)
{
    jmp_buf env;
    int volatile set = 1;

    switch (setjmp(env)) {
    case 0:
        EXPECT_EQ(set, 1);
        set = 0;
        longjmp(env, 0x789ABCDE);
        VERIFY_NOT_REACHED();

    case 0x789ABCDE:
        EXPECT_EQ(set, 0);
        break;

    default:
        VERIFY_NOT_REACHED();
    };
}

TEST_CASE(sigsetjmp)
{
    sigjmp_buf env;
    int volatile set = 1;

    if (sigsetjmp(env, 0)) {
        EXPECT_EQ(set, 0);
        return;
    }

    EXPECT_EQ(set, 1);
    set = 0;
    siglongjmp(env, 1);

    VERIFY_NOT_REACHED();
}

TEST_CASE(sigsetjmp_zero)
{
    sigjmp_buf env;
    int volatile set = 1;

    switch (sigsetjmp(env, 0)) {
    case 0:
        EXPECT_EQ(set, 1);
        set = 0;
        siglongjmp(env, 0);
        VERIFY_NOT_REACHED();

    case 1:
        EXPECT_EQ(set, 0);
        break;

    default:
        VERIFY_NOT_REACHED();
    };
}

TEST_CASE(sigsetjmp_value)
{
    sigjmp_buf env;
    int volatile set = 1;

    switch (sigsetjmp(env, 0)) {
    case 0:
        EXPECT_EQ(set, 1);
        set = 0;
        siglongjmp(env, 0x789ABCDE);
        VERIFY_NOT_REACHED();

    case 0x789ABCDE:
        EXPECT_EQ(set, 0);
        break;

    default:
        VERIFY_NOT_REACHED();
    };
}

TEST_CASE(sigsetjmp_signal_mask)
{
    sigjmp_buf env;

    sigset_t alternative_sigset;
    sigfillset(&alternative_sigset);

    sigset_t initial_sigset;
    sigprocmask(SIG_SETMASK, nullptr, &initial_sigset);

    EXPECT_NE(memcmp(&alternative_sigset, &initial_sigset, sizeof(sigset_t)), 0);

    if (sigsetjmp(env, 1)) {
        sigprocmask(SIG_SETMASK, nullptr, &alternative_sigset);
        EXPECT_EQ(memcmp(&alternative_sigset, &initial_sigset, sizeof(sigset_t)), 0);
        return;
    }

    sigprocmask(SIG_SETMASK, &alternative_sigset, nullptr);
    siglongjmp(env, 1);
    VERIFY_NOT_REACHED();
}
