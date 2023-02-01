/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <errno.h>
#include <mallocdefs.h>
#include <stdlib.h>

TEST_CASE(malloc_limits)
{
    EXPECT_NO_CRASH("Allocation of 0 size should succeed at allocation and release", [] {
        errno = 0;
        void* ptr = malloc(0);
        EXPECT_EQ(errno, 0);
        free(ptr);
        return Test::Crash::Failure::DidNotCrash;
    });

    EXPECT_NO_CRASH("Allocation of the maximum `size_t` value should fails with `ENOMEM`", [] {
        errno = 0;
        void* ptr = malloc(NumericLimits<size_t>::max());
        EXPECT_EQ(errno, ENOMEM);
        EXPECT_EQ(ptr, nullptr);
        free(ptr);
        return Test::Crash::Failure::DidNotCrash;
    });

    EXPECT_NO_CRASH("Allocation of the maximum `size_t` value that does not overflow should fails with `ENOMEM`", [] {
        errno = 0;
        void* ptr = malloc(NumericLimits<size_t>::max() - ChunkedBlock::block_size - sizeof(BigAllocationBlock));
        EXPECT_EQ(errno, ENOMEM);
        EXPECT_EQ(ptr, nullptr);
        free(ptr);
        return Test::Crash::Failure::DidNotCrash;
    });
}
