/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <signal.h>

TEST_CASE(signal_string_mapping)
{
    char signal_name[SIG2STR_MAX] = { 0 };
    for (int i = 1; i < NSIG; ++i) {
        if (sig2str(i, signal_name) != 0)
            continue;
        size_t name_length = strlen(signal_name);
        EXPECT(name_length < SIG2STR_MAX);
        EXPECT(name_length > 0);
    }
}

TEST_CASE(negative_sig2str)
{
    char signal_name[SIG2STR_MAX] = { 0 };
    for (int i = -10; i < 0; ++i) {
        EXPECT_EQ(sig2str(i, signal_name), -1);
        EXPECT_EQ(signal_name[0], 0);
    }
}

// Tests the following requirement for str2sig (from POSIX):
// "If str points to a string returned by a previous successful call to
// sig2str(signum,str), the value stored in the location pointed to by pnum
// shall be equal to signum."
TEST_CASE(signal_string_identity)
{
    char mappings[NSIG][SIG2STR_MAX] = {};
    bool success[NSIG] = { 0 };
    // Includes signal #0 for the sake of testing.
    for (int i = 0; i < NSIG; ++i) {
        success[i] = (sig2str(i, mappings[i]) == 0);
    }

    for (int i = 0; i < NSIG; ++i) {
        if (!success[i])
            continue;
        int signal = 0;
        EXPECT_EQ(str2sig(mappings[i], &signal), 0);
        EXPECT_EQ(signal, i);
    }
}
