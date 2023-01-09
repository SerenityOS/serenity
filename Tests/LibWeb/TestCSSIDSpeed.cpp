/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibWeb/CSS/ValueID.h>

TEST_CASE(basic)
{
    EXPECT_EQ(Web::CSS::value_id_from_string("italic"sv), Web::CSS::ValueID::Italic);
    EXPECT_EQ(Web::CSS::value_id_from_string("inline"sv), Web::CSS::ValueID::Inline);
    EXPECT_EQ(Web::CSS::value_id_from_string("small"sv), Web::CSS::ValueID::Small);
    EXPECT_EQ(Web::CSS::value_id_from_string("smalL"sv), Web::CSS::ValueID::Small);
    EXPECT_EQ(Web::CSS::value_id_from_string("SMALL"sv), Web::CSS::ValueID::Small);
    EXPECT_EQ(Web::CSS::value_id_from_string("Small"sv), Web::CSS::ValueID::Small);
    EXPECT_EQ(Web::CSS::value_id_from_string("smALl"sv), Web::CSS::ValueID::Small);
}

BENCHMARK_CASE(value_id_from_string)
{
    for (size_t i = 0; i < 10'000'000; ++i) {
        EXPECT_EQ(Web::CSS::value_id_from_string("inline"sv), Web::CSS::ValueID::Inline);
    }
}
