/*
 * Copyright (c) 2026, Ramji Purwar <ramji.purwar@iitgn.ac.in>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <errno.h>
#include <limits.h>
#include <stdlib.h>

TEST_CASE(strtol_hex_prefix)
{
    char const* input;
    char* endptr;

    // "0x" or "0X" may precede the digits if the base is 16.
    input = "0x1A";
    EXPECT_EQ(strtol(input, &endptr, 16), 26l);
    EXPECT_EQ(endptr, input + 4);

    input = "0X1a";
    EXPECT_EQ(strtoll(input, &endptr, 16), 26ll);
    EXPECT_EQ(endptr, input + 4);

    // The prefix comes after the sign.
    input = " -0x10";
    EXPECT_EQ(strtol(input, &endptr, 16), -16l);
    EXPECT_EQ(endptr, input + 6);
}

TEST_CASE(strtol_hex_prefix_without_digits)
{
    char const* input;
    char* endptr;

    // Without a hexadecimal digit after it, "0x" is the number 0 followed by an "x".
    input = "0xg";
    EXPECT_EQ(strtol(input, &endptr, 0), 0l);
    EXPECT_EQ(endptr, input + 1);

    input = "0xg";
    EXPECT_EQ(strtoul(input, &endptr, 16), 0ul);
    EXPECT_EQ(endptr, input + 1);
}

TEST_CASE(strtol_base_0)
{
    char const* input;
    char* endptr;

    // With base 0, a "0x" prefix selects base 16 and a leading "0" selects base 8.
    input = "0x1A";
    EXPECT_EQ(strtol(input, &endptr, 0), 26l);
    EXPECT_EQ(endptr, input + 4);

    input = "017";
    EXPECT_EQ(strtol(input, &endptr, 0), 15l);
    EXPECT_EQ(endptr, input + 3);
}

TEST_CASE(strtoul_sign)
{
    char const* input;
    char* endptr;

    input = "+42";
    EXPECT_EQ(strtoul(input, &endptr, 10), 42ul);
    EXPECT_EQ(endptr, input + 3);

    // A leading minus sign negates the result in the return type.
    input = "-1";
    EXPECT_EQ(strtoul(input, &endptr, 10), ULONG_MAX);
    EXPECT_EQ(endptr, input + 2);

    input = "  -0x10";
    EXPECT_EQ(strtoull(input, &endptr, 16), static_cast<unsigned long long>(-16));
    EXPECT_EQ(endptr, input + 7);
}

TEST_CASE(strtoull_overflow)
{
    char const* input;
    char* endptr;

    input = "18446744073709551615";
    errno = 0;
    EXPECT_EQ(strtoull(input, &endptr, 10), ULLONG_MAX);
    EXPECT_EQ(endptr, input + 20);
    EXPECT_EQ(errno, 0);

    input = "18446744073709551616";
    errno = 0;
    EXPECT_EQ(strtoull(input, &endptr, 10), ULLONG_MAX);
    EXPECT_EQ(endptr, input + 20);
    EXPECT_EQ(errno, ERANGE);

    input = "-18446744073709551616";
    errno = 0;
    EXPECT_EQ(strtoull(input, &endptr, 10), ULLONG_MAX);
    EXPECT_EQ(endptr, input + 21);
    EXPECT_EQ(errno, ERANGE);
}

TEST_CASE(strtoll_lower_limit)
{
    errno = 0;
    EXPECT_EQ(strtoll("-9223372036854775808", nullptr, 10), LLONG_MIN);
    EXPECT_EQ(errno, 0);

    errno = 0;
    EXPECT_EQ(strtoll("-9223372036854775809", nullptr, 10), LLONG_MIN);
    EXPECT_EQ(errno, ERANGE);
}
