/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <Kernel/API/Pledge.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <unistd.h>

using namespace Kernel::PledgeBits;

TEST_CASE(test_nonexistent_pledge)
{
    auto res = pledge((u8)Kernel::PledgeMode::Both, 1 << (Kernel::pledge_promise_count + 2), 0);
    if (res >= 0)
        FAIL("Pledging on existent promises should fail.");
}

TEST_CASE(test_pledge_argument_validation)
{
    // Pledge bit that's out of range.
    constexpr auto fake = 1 << (Kernel::pledge_promise_count + 3);

    auto res = pledge((u8)Kernel::PledgeMode::Both, fake, stdio);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);

    res = pledge((u8)Kernel::PledgeMode::Both, stdio, fake);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);
}

TEST_CASE(test_pledge_failures)
{
    auto res = pledge((u8)Kernel::PledgeMode::Both, stdio | unix | rpath, stdio);
    if (res < 0)
        FAIL("Initial pledge is expected to work.");

    res = pledge((u8)Kernel::PledgeMode::Both, stdio | unix, stdio | unix);
    if (res >= 0)
        FAIL("Additional execpromise \"unix\" should have failed");

    res = pledge((u8)Kernel::PledgeMode::Both, stdio, stdio);
    if (res < 0)
        FAIL("Reducing promises is expected to work.");
}
