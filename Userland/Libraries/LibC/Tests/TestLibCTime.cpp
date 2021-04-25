/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibTest/TestCase.h>
#include <time.h>

const auto expected_epoch = "Thu Jan  1 00:00:00 1970\n"sv;

TEST_CASE(asctime)
{
    time_t epoch = 0;
    auto result = asctime(localtime(&epoch));
    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(asctime_r)
{
    char buffer[26] {};
    time_t epoch = 0;
    auto result = asctime_r(localtime(&epoch), buffer);
    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(ctime)
{
    time_t epoch = 0;
    auto result = ctime(&epoch);

    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(ctime_r)
{
    char buffer[26] {};
    time_t epoch = 0;
    auto result = ctime_r(&epoch, buffer);

    EXPECT_EQ(expected_epoch, StringView(result));
}
