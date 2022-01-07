/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibTest/TestCase.h>

// Note: Needs to be 'noinline' so stack canary isn't optimized out.
static void __attribute__((noinline)) smasher(char* string)
{
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Warray-bounds"
    for (int i = 0; i < 256; i++) {
        string[i] = 'A';
    }
#pragma GCC diagnostic pop
}

// Note: Needs to be 'noinline' so stack canary isn't optimized out.
static void __attribute__((noinline)) stack_to_smash()
{
    char string[8] = {};
    smasher(string);
}

TEST_CASE(stack_smash)
{
    EXPECT_CRASH("Smash the stack and trigger __stack_chk_fail", [] {
        outln("[+] Starting the stack smash...");
        stack_to_smash();
        outln("[+] Stack smash wasn't detected!");
        return Test::Crash::Failure::DidNotCrash;
    });
}
