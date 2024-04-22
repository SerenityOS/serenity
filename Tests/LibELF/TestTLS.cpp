/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

// When linking an executable, TLS relaxations might be relaxed to different
// access modes than intended. Hence, the actual logic has been moved to a
// shared library, and this executable just calls it.
extern void run_test();
TEST_CASE(basic)
{
    run_test();
}

TEST_CASE(local_exec)
{
    [[gnu::tls_model("local-exec")]] static volatile __thread char test1[4096 * 4 + 10];

    for (size_t i = 0; i < sizeof(test1); i++) {
        test1[i] = static_cast<char>(i);
        AK::taint_for_optimizer(test1[i]);
    }

    for (size_t i = 0; i < sizeof(test1); i++) {
        AK::taint_for_optimizer(test1[i]);
        EXPECT_EQ(test1[i], static_cast<char>(i));
    }

    [[gnu::tls_model("local-exec")]] static volatile __thread u16 test2[] = { 0x1234, 0x5678, 0xabcd };
    AK::taint_for_optimizer(test2[0]);
    EXPECT_EQ(test2[0], 0x1234);
    AK::taint_for_optimizer(test2[1]);
    EXPECT_EQ(test2[1], 0x5678);
    AK::taint_for_optimizer(test2[2]);
    EXPECT_EQ(test2[2], 0xabcd);
}
