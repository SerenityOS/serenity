/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FixedPoint.h>

using Type = FixedPoint<4>;

TEST_CASE(arithmetic)
{
    EXPECT_EQ(
        Type(0.5) + Type(0.5),
        Type(1));
    EXPECT_EQ(
        Type(1) + Type(0.5),
        Type(1.5));
    EXPECT_EQ(
        (float)(Type(1) + Type(0.5)),
        1.5f);
    // FIXME: Test for rounded multiply
    EXPECT_EQ(
        Type((int)1) * Type(0.5),
        Type(0.5));
    EXPECT_EQ(
        Type((int)1) / Type(0.5),
        Type(2));
}

TEST_CASE(rounding)
{
    EXPECT_EQ(Type(0.5).round(), Type(0));
    EXPECT_EQ(Type(0.5).floor(), Type(0));
    EXPECT_EQ(Type(0.5).ceil(), Type(1));
    EXPECT_EQ(Type(0.75).trunk(), Type(0));

    EXPECT_EQ(Type(1.5).round(), Type(2));
    EXPECT_EQ(Type(1.5).floor(), Type(1));
    EXPECT_EQ(Type(1.5).ceil(), Type(2));
    EXPECT_EQ(Type(1.25).trunk(), Type(1));

    EXPECT_EQ(Type(-0.5).round(), Type(0));
    EXPECT_EQ(Type(-0.5).floor(), Type(-1));
    EXPECT_EQ(Type(-0.5).ceil(), Type(0));
    EXPECT_EQ(Type(-0.75).trunk(), Type(0));

    EXPECT_EQ(Type(-1.5).round(), Type(-2));
    EXPECT_EQ(Type(-1.5).floor(), Type(-2));
    EXPECT_EQ(Type(-1.5).ceil(), Type(-1));
    EXPECT_EQ(Type(-1.25).trunk(), Type(-1));

    EXPECT_EQ(Type(0.5).lround(), 0);
    EXPECT_EQ(Type(0.5).lfloor(), 0);
    EXPECT_EQ(Type(0.5).lceil(), 1);
    EXPECT_EQ(Type(0.5).ltrunk(), 0);

    EXPECT_EQ(Type(1.5).lround(), 2);
    EXPECT_EQ(Type(1.5).lfloor(), 1);
    EXPECT_EQ(Type(1.5).lceil(), 2);
    EXPECT_EQ(Type(1.5).ltrunk(), 1);

    EXPECT_EQ(Type(-0.5).lround(), 0);
    EXPECT_EQ(Type(-0.5).lfloor(), -1);
    EXPECT_EQ(Type(-0.5).lceil(), 0);
    EXPECT_EQ(Type(-0.5).ltrunk(), 0);

    EXPECT_EQ(Type(-1.5).lround(), -2);
    EXPECT_EQ(Type(-1.5).lfloor(), -2);
    EXPECT_EQ(Type(-1.5).lceil(), -1);
    EXPECT_EQ(Type(-1.5).ltrunk(), -1);
}

TEST_CASE(cast)
{
    FixedPoint<16, u32> downcast_value1(FixedPoint<32, u64>(123.4567));
    EXPECT((double)downcast_value1 >= 123.4566 && (double)downcast_value1 <= 123.4568);
    static constexpr FixedPoint<32, u64> value1(321.7654);
    downcast_value1 = value1;
    EXPECT((double)downcast_value1 >= 321.7653 && (double)downcast_value1 <= 321.7655);
    FixedPoint<6, u32> downcast_value2(FixedPoint<32, u64>(4567.123456));
    EXPECT((double)downcast_value2 >= 4567.1 && (double)downcast_value2 <= 4567.2);
    downcast_value2 = FixedPoint<32, u64>(7654.654321);
    EXPECT((double)downcast_value2 >= 7654.64 && (double)downcast_value2 <= 7654.66);

    EXPECT((double)downcast_value2 >= 7654.64 && (double)downcast_value2 <= 7654.66);
    FixedPoint<6, u32> downcast_value3(FixedPoint<32, u64>(4567.987654));
    EXPECT((double)downcast_value3 >= 4567.9 && (double)downcast_value3 <= 4567.99);
    downcast_value3 = FixedPoint<32, u64>(7654.456789);
    EXPECT((double)downcast_value3 >= 7654.45 && (double)downcast_value3 <= 7654.46);

    FixedPoint<32, u64> upcast_value1(FixedPoint<16, u32>(123.4567));
    EXPECT((double)upcast_value1 >= 123.4566 && (double)upcast_value1 <= 123.4568);
    upcast_value1 = FixedPoint<16, u32>(321.7654);
    EXPECT((double)upcast_value1 >= 321.7653 && (double)upcast_value1 <= 321.7655);
    FixedPoint<32, u64> upcast_value2(FixedPoint<6, u32>(4567.123456));
    EXPECT((double)upcast_value2 >= 4567.1 && (double)upcast_value2 <= 4567.2);
    upcast_value2 = FixedPoint<6, u32>(7654.654321);
    EXPECT((double)upcast_value2 >= 7654.64 && (double)upcast_value2 <= 7654.66);
    FixedPoint<32, u64> upcast_value3(FixedPoint<6, u32>(4567.987654));
    EXPECT((double)upcast_value3 >= 4567.9 && (double)upcast_value3 <= 4567.99);
    upcast_value3 = FixedPoint<6, u32>(7654.456789);
    EXPECT((double)upcast_value3 >= 7654.45 && (double)upcast_value3 <= 7654.46);
}

TEST_CASE(formatter)
{
    EXPECT_EQ(String::formatted("{}", FixedPoint<16>(123.456)), "123.455993"sv);
    EXPECT_EQ(String::formatted("{}", FixedPoint<16>(-123.456)), "-123.455994"sv);
    EXPECT_EQ(String::formatted("{}", FixedPoint<4>(123.456)), "123.4375"sv);
    EXPECT_EQ(String::formatted("{}", FixedPoint<4>(-123.456)), "-123.4375"sv);
    EXPECT_EQ(String::formatted("{}", FixedPoint<16> {}), "0"sv);
}
