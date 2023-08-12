/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FixedPoint.h>
#include <AK/NumericLimits.h>

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
    EXPECT_EQ(Type(0.125) * Type(3.75),
        Type(0.125 * 3.75));
    EXPECT_EQ(Type(0.125) * Type(-3.75),
        Type(0.125 * -3.75));

    EXPECT_EQ(
        Type((int)1) / Type(0.5),
        Type(2));
}

TEST_CASE(rounding)
{
    EXPECT_EQ(Type(0.5).rint(), Type(0));
    EXPECT_EQ(Type(0.5).floor(), Type(0));
    EXPECT_EQ(Type(0.5).ceil(), Type(1));
    EXPECT_EQ(Type(0.75).trunc(), Type(0));

    EXPECT_EQ(Type(1.5).rint(), Type(2));
    EXPECT_EQ(Type(1.5).floor(), Type(1));
    EXPECT_EQ(Type(1.5).ceil(), Type(2));
    EXPECT_EQ(Type(1.25).trunc(), Type(1));

    EXPECT_EQ(Type(-0.5).rint(), Type(0));
    EXPECT_EQ(Type(-0.5).floor(), Type(-1));
    EXPECT_EQ(Type(-0.5).ceil(), Type(0));
    EXPECT_EQ(Type(-0.75).trunc(), Type(0));

    EXPECT_EQ(Type(-1.5).rint(), Type(-2));
    EXPECT_EQ(Type(-1.5).floor(), Type(-2));
    EXPECT_EQ(Type(-1.5).ceil(), Type(-1));
    EXPECT_EQ(Type(-1.25).trunc(), Type(-1));

    EXPECT_EQ(Type(2.75).rint(), Type(3));
    EXPECT_EQ(Type(-1.25).rint(), Type(-1));

    EXPECT_EQ(Type(0.5).lrint(), 0);
    EXPECT_EQ(Type(0.5).lfloor(), 0);
    EXPECT_EQ(Type(0.5).lceil(), 1);
    EXPECT_EQ(Type(0.5).ltrunc(), 0);

    EXPECT_EQ(Type(1.5).lrint(), 2);
    EXPECT_EQ(Type(1.5).lfloor(), 1);
    EXPECT_EQ(Type(1.5).lceil(), 2);
    EXPECT_EQ(Type(1.5).ltrunc(), 1);

    EXPECT_EQ(Type(-0.5).lrint(), 0);
    EXPECT_EQ(Type(-0.5).lfloor(), -1);
    EXPECT_EQ(Type(-0.5).lceil(), 0);
    EXPECT_EQ(Type(-0.5).ltrunc(), 0);

    EXPECT_EQ(Type(-1.5).lrint(), -2);
    EXPECT_EQ(Type(-1.5).lfloor(), -2);
    EXPECT_EQ(Type(-1.5).lceil(), -1);
    EXPECT_EQ(Type(-1.5).ltrunc(), -1);

    EXPECT_EQ(Type(-1.6).rint(), -2);
    EXPECT_EQ(Type(-1.4).rint(), -1);
    EXPECT_EQ(Type(1.6).rint(), 2);
    EXPECT_EQ(Type(1.4).rint(), 1);

    // Check that sRGB TRC curve parameters match the s15fixed16 values stored in Gimp's built-in profile.
    // (This only requires that the FixedPoint<> constructor rounds before truncating to the fixed-point value,
    // as it should anyways.)
    using S15Fixed16 = FixedPoint<16, i32>;
    EXPECT_EQ(S15Fixed16(2.4).raw(), 0x26666);
    EXPECT_EQ(S15Fixed16(1 / 1.055).raw(), 0xf2a7);
    EXPECT_EQ(S15Fixed16(0.055 / 1.055).raw(), 0xd59);
    EXPECT_EQ(S15Fixed16(1 / 12.92).raw(), 0x13d0);
    EXPECT_EQ(S15Fixed16(0.04045).raw(), 0xa5b);
}

TEST_CASE(logarithm)
{
    EXPECT_EQ(Type(0).log2().raw(), NumericLimits<int>::min());
    EXPECT_EQ(Type(1).log2(), Type(0));
    EXPECT_EQ(Type(2).log2(), Type(1));
    EXPECT_EQ(Type(8).log2(), Type(3));
    EXPECT_EQ(Type(0.5).log2(), Type(-1));

    EXPECT_EQ(Type(22.627416997969520780827019587355).log2(), Type(4.4375));
    EXPECT_EQ(Type(3088).log2(), Type(11.592457037268080419637304576833));
}

TEST_CASE(comparison)
{
    EXPECT(Type(0) < 1);
    EXPECT(Type(0) <= 1);
    EXPECT(Type(0) <= 0);
    EXPECT(Type(-10) <= -10);

    EXPECT(Type(4.25) > 4);
    EXPECT(Type(4.25) >= 4);
    EXPECT(Type(4.25) <= 5);
    EXPECT(Type(4.25) < 5);
    EXPECT(Type(1.5) > 1);

    EXPECT(!(FixedPoint<4, u8>(2) > 128));
    EXPECT(!(FixedPoint<4, u8>(2) >= 128));

    EXPECT(Type(-6.25) < -6);
    EXPECT(Type(-6.25) <= -6);
    EXPECT(Type(-6.75) > -7);
    EXPECT(Type(-6.75) >= -7);

    EXPECT(Type(17) == 17);
    EXPECT(Type(-8) != -9);
}

TEST_CASE(cast)
{
    FixedPoint<16, u32> downcast_value1(FixedPoint<32, u64>(123.4567));
    EXPECT((double)downcast_value1 >= 123.4566 && (double)downcast_value1 <= 123.4568);
    static FixedPoint<32, u64> const value1(321.7654);
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
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(123.456)), "123.455993"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(-123.456)), "-123.455994"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<4>(123.456)), "123.4375"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<4>(-123.456)), "-123.4375"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16> {}), "0"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(0.1)), "0.100006"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(0.02)), "0.020004"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(0.003)), "0.003005"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(0.0004)), "0.000396"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(0.0000000005)), "0"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(-0.1)), "-0.100007"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(-0.02)), "-0.020005"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", FixedPoint<16>(-0.0000000005)), "0"sv);

    EXPECT_EQ(DeprecatedString::formatted("{}", Type(-1)), "-1"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", Type(-2)), "-2"sv);
    EXPECT_EQ(DeprecatedString::formatted("{}", Type(-3)), "-3"sv);
}
