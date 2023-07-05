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
