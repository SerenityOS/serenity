/*
 * Copyright (c) 2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h>

__thread int one = 1;
__thread int two = 2;
[[gnu::tls_model("initial-exec")]] __thread int three = 3;
[[gnu::tls_model("initial-exec")]] __thread int four = 4;

void check_increment_worked();
void check_increment_worked()
{
    EXPECT_EQ(one, 2);
    EXPECT_EQ(two, 3);
    EXPECT_EQ(three, 4);
    EXPECT_EQ(four, 5);
}
