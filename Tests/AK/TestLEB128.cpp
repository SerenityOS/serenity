/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LEB128.h>
#include <AK/MemoryStream.h>
#include <AK/NumericLimits.h>
#include <LibTest/TestCase.h>

TEST_CASE(single_byte)
{
    u32 output = {};
    i32 output_signed = {};
    u8 buf[] = { 0x00 };
    InputMemoryStream stream({ buf, sizeof(buf) });

    // less than/eq 0b0011_1111, signed == unsigned == raw byte
    for (u8 i = 0u; i <= 0x3F; ++i) {
        buf[0] = i;

        stream.seek(0);
        EXPECT(LEB128::read_unsigned(stream, output));
        EXPECT_EQ(output, i);
        EXPECT(!stream.handle_any_error());

        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, output_signed));
        EXPECT_EQ(output_signed, i);
        EXPECT(!stream.handle_any_error());
    }

    // 0b0100_0000 to 0b0111_1111 unsigned == byte, signed = {{ 26'b(-1), 6'b(byte) }}
    for (u8 i = 0x40u; i < 0x80; ++i) {
        buf[0] = i;

        stream.seek(0);
        EXPECT(LEB128::read_unsigned(stream, output));
        EXPECT_EQ(output, i);
        EXPECT(!stream.handle_any_error());

        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, output_signed));
        EXPECT_EQ(output_signed, (i | (-1 & (~0x3F))));
        EXPECT(!stream.handle_any_error());
    }
    // MSB set, but input too short
    for (u16 i = 0x80; i <= 0xFF; ++i) {
        buf[0] = static_cast<u8>(i);

        stream.seek(0);
        EXPECT(!LEB128::read_unsigned(stream, output));
        EXPECT(stream.handle_any_error());

        stream.seek(0);
        EXPECT(!LEB128::read_signed(stream, output_signed));
        EXPECT(stream.handle_any_error());
    }
}

TEST_CASE(two_bytes)
{
    u32 output = {};
    i32 output_signed = {};
    u8 buf[] = { 0x00, 0x1 };
    InputMemoryStream stream({ buf, sizeof(buf) });

    // Only test with first byte expecting more, otherwise equivalent to single byte case
    for (u16 i = 0x80; i <= 0xFF; ++i) {
        buf[0] = static_cast<u8>(i);

        // less than/eq 0b0011_1111: signed == unsigned == (j << 7) + (7 MSB of i)
        for (u8 j = 0u; j <= 0x3F; ++j) {
            buf[1] = j;

            stream.seek(0);
            EXPECT(LEB128::read_unsigned(stream, output));
            EXPECT_EQ(output, (static_cast<u32>(j) << 7) + (i & 0x7F));
            EXPECT(!stream.handle_any_error());

            stream.seek(0);
            EXPECT(LEB128::read_signed(stream, output_signed));
            EXPECT_EQ(output_signed, (static_cast<i32>(j) << 7) + (i & 0x7F));
            EXPECT(!stream.handle_any_error());
        }

        // 0b0100_0000 to 0b0111_1111: unsigned == (j << 7) + (7 MSB of i), signed == {{ 19'b(-1), 6'b(j), 7'b(i) }}
        for (u8 j = 0x40u; j < 0x80; ++j) {
            buf[1] = j;

            stream.seek(0);
            EXPECT(LEB128::read_unsigned(stream, output));
            EXPECT_EQ(output, (static_cast<u32>(j) << 7) + (i & 0x7F));
            EXPECT(!stream.handle_any_error());

            stream.seek(0);
            EXPECT(LEB128::read_signed(stream, output_signed));
            EXPECT_EQ(output_signed, ((static_cast<i32>(j) << 7) + (i & 0x7F)) | (-1 & (~0x3FFF)));
            EXPECT(!stream.handle_any_error());
        }

        // MSB set on last byte, but input too short
        for (u16 j = 0x80; j <= 0xFF; ++j) {
            buf[1] = static_cast<u8>(j);

            stream.seek(0);
            EXPECT(!LEB128::read_unsigned(stream, output));
            EXPECT(stream.handle_any_error());

            stream.seek(0);
            EXPECT(!LEB128::read_signed(stream, output_signed));
            EXPECT(stream.handle_any_error());
        }
    }
}

TEST_CASE(overflow_sizeof_output_unsigned)
{
    u8 u32_max_plus_one[] = { 0x80, 0x80, 0x80, 0x80, 0x10 };
    {
        u32 out = 0;
        InputMemoryStream stream({ u32_max_plus_one, sizeof(u32_max_plus_one) });
        EXPECT(!LEB128::read_unsigned(stream, out));
        EXPECT_EQ(out, 0u);
        EXPECT(!stream.handle_any_error());

        u64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_unsigned(stream, out64));
        EXPECT_EQ(out64, static_cast<u64>(NumericLimits<u32>::max()) + 1);
        EXPECT(!stream.handle_any_error());
    }

    u8 u32_max[] = { 0xFF, 0xFF, 0xFF, 0xFF, 0x0F };
    {
        u32 out = 0;
        InputMemoryStream stream({ u32_max, sizeof(u32_max) });
        EXPECT(LEB128::read_unsigned(stream, out));
        EXPECT_EQ(out, NumericLimits<u32>::max());
        EXPECT(!stream.handle_any_error());

        u64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_unsigned(stream, out64));
        EXPECT_EQ(out64, NumericLimits<u32>::max());
        EXPECT(!stream.handle_any_error());
    }
}

TEST_CASE(overflow_sizeof_output_signed)
{
    u8 i32_max_plus_one[] = { 0x80, 0x80, 0x80, 0x80, 0x08 };
    {
        i32 out = 0;
        InputMemoryStream stream({ i32_max_plus_one, sizeof(i32_max_plus_one) });
        EXPECT(!LEB128::read_signed(stream, out));
        EXPECT_EQ(out, 0);
        EXPECT(!stream.handle_any_error());

        i64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, out64));
        EXPECT_EQ(out64, static_cast<i64>(NumericLimits<i32>::max()) + 1);
        EXPECT(!stream.handle_any_error());
    }

    u8 i32_max[] = { 0xFF, 0xFF, 0xFF, 0xFF, 0x07 };
    {
        i32 out = 0;
        InputMemoryStream stream({ i32_max, sizeof(i32_max) });
        EXPECT(LEB128::read_signed(stream, out));
        EXPECT_EQ(out, NumericLimits<i32>::max());
        EXPECT(!stream.handle_any_error());

        i64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, out64));
        EXPECT_EQ(out64, NumericLimits<i32>::max());
        EXPECT(!stream.handle_any_error());
    }

    u8 i32_min_minus_one[] = { 0xFF, 0xFF, 0xFF, 0xFF, 0x77 };
    {
        i32 out = 0;
        InputMemoryStream stream({ i32_min_minus_one, sizeof(i32_min_minus_one) });
        EXPECT(!LEB128::read_signed(stream, out));
        EXPECT_EQ(out, 0);
        EXPECT(!stream.handle_any_error());

        i64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, out64));
        EXPECT_EQ(out64, static_cast<i64>(NumericLimits<i32>::min()) - 1);
        EXPECT(!stream.handle_any_error());
    }

    u8 i32_min[] = { 0x80, 0x80, 0x80, 0x80, 0x78 };
    {
        i32 out = 0;
        InputMemoryStream stream({ i32_min, sizeof(i32_min) });
        EXPECT(LEB128::read_signed(stream, out));
        EXPECT_EQ(out, NumericLimits<i32>::min());
        EXPECT(!stream.handle_any_error());

        i64 out64 = 0;
        stream.seek(0);
        EXPECT(LEB128::read_signed(stream, out64));
        EXPECT_EQ(out64, NumericLimits<i32>::min());
        EXPECT(!stream.handle_any_error());
    }
}
