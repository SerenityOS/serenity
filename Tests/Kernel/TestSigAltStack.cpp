/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>

#if defined(AK_COMPILER_CLANG)
#    pragma clang optimize off
#else
#    pragma GCC optimize("O0")
#endif

#include <LibTest/TestCase.h>
#include <signal.h>
#include <unistd.h>

static void signal_handler(int)
{
    // We execute this syscall in order to force the kernel to perform the syscall precondition validation which
    // checks that we have correctly set up the stack region to match our currently implemented protections.
    getuid();
    _exit(0);
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Winfinite-recursion"
static size_t infinite_recursion(size_t input)
{
    return infinite_recursion(input) + 1;
}
#pragma GCC diagnostic pop

// This test can only pass with sigaltstack correctly enabled, as otherwise the SIGSEGV signal handler itself would also fault due to the overflown stack.
TEST_CASE(success_case)
{
    static u8 alt_stack[SIGSTKSZ];
    stack_t ss = {
        .ss_sp = alt_stack,
        .ss_flags = 0,
        .ss_size = SIGSTKSZ,
    };
    auto res = sigaltstack(&ss, nullptr);
    EXPECT_EQ(res, 0);

    struct sigaction sa;
    sa.sa_handler = signal_handler;
    sa.sa_flags = SA_ONSTACK;
    res = sigfillset(&sa.sa_mask);
    EXPECT_EQ(res, 0);
    res = sigaction(SIGSEGV, &sa, 0);
    EXPECT_EQ(res, 0);

    (void)infinite_recursion(0);
    FAIL("Infinite recursion finished successfully");
}
