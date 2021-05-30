/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LEB128.h>
#include <AK/MemoryStream.h>
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
