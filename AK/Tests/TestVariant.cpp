/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenity.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestSuite.h>

#include <AK/Variant.h>

TEST_CASE(basic)
{
    Variant<int, String> the_value { 42 };
    EXPECT(the_value.has<int>());
    EXPECT_EQ(the_value.get<int>(), 42);
    the_value = String("42");
    EXPECT(the_value.has<String>());
    EXPECT_EQ(the_value.get<String>(), "42");
}

TEST_CASE(visit)
{
    bool correct = false;
    Variant<int, String, float> the_value { 42.0f };
    the_value.visit(
        [&](const int&) { correct = false; },
        [&](const String&) { correct = false; },
        [&](const float&) { correct = true; });
    EXPECT(correct);
}
