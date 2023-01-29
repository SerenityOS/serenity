/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/MemoryStream.h>

TEST_CASE(read_an_integer)
{
    u32 expected = 0x01020304, actual;

    InputMemoryStream stream { { &expected, sizeof(expected) } };
    stream >> actual;

    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(read_a_bool)
{
    bool expected = true, actual;

    InputMemoryStream stream { { &expected, sizeof(expected) } };
    stream >> actual;

    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(read_a_double)
{
    double expected = 3.141592653589793, actual;

    InputMemoryStream stream { { &expected, sizeof(expected) } };
    stream >> actual;

    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(recoverable_error)
{
    u32 expected = 0x01020304, actual = 0;
    u64 to_large_value = 0;

    InputMemoryStream stream { { &expected, sizeof(expected) } };

    EXPECT(!stream.has_any_error() && !stream.eof());
    stream >> to_large_value;
    EXPECT(stream.has_recoverable_error() && !stream.eof());

    EXPECT(stream.handle_recoverable_error());
    EXPECT(!stream.has_any_error() && !stream.eof());

    stream >> actual;
    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(chain_stream_operator)
{
    Array<u8, 4> const expected { 0, 1, 2, 3 };
    Array<u8, 4> actual;

    InputMemoryStream stream { expected };

    stream >> actual[0] >> actual[1] >> actual[2] >> actual[3];
    EXPECT(!stream.has_any_error() && stream.eof());

    EXPECT_EQ(expected, actual);
}

TEST_CASE(seeking_slicing_offset)
{
    Array<u8, 8> const input { 0, 1, 2, 3, 4, 5, 6, 7 };
    Array<u8, 4> const expected0 { 0, 1, 2, 3 };
    Array<u8, 4> const expected1 { 4, 5, 6, 7 };
    Array<u8, 4> const expected2 { 1, 2, 3, 4 };

    Array<u8, 4> actual0 {}, actual1 {}, actual2 {};

    InputMemoryStream stream { input };

    stream >> actual0;
    EXPECT(!stream.has_any_error() && !stream.eof());
    EXPECT_EQ(expected0, actual0);

    stream.seek(4);
    stream >> actual1;
    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT_EQ(expected1, actual1);

    stream.seek(1);
    stream >> actual2;
    EXPECT(!stream.has_any_error() && !stream.eof());
    EXPECT_EQ(expected2, actual2);
}

TEST_CASE(read_endian_values)
{
    Array<u8, 8> const input { 0, 1, 2, 3, 4, 5, 6, 7 };
    InputMemoryStream stream { input };

    LittleEndian<u32> value1;
    BigEndian<u32> value2;
    stream >> value1 >> value2;

    EXPECT_EQ(value1, 0x03020100u);
    EXPECT_EQ(value2, 0x04050607u);
}

TEST_CASE(new_output_memory_stream)
{
    Array<u8, 16> buffer;
    OutputMemoryStream stream { buffer };

    EXPECT_EQ(stream.size(), 0u);
    EXPECT_EQ(stream.remaining(), 16u);

    stream << LittleEndian<u16>(0x12'87);

    EXPECT_EQ(stream.size(), 2u);
    EXPECT_EQ(stream.remaining(), 14u);

    stream << buffer;

    EXPECT(stream.handle_recoverable_error());
    EXPECT_EQ(stream.size(), 2u);
    EXPECT_EQ(stream.remaining(), 14u);

    EXPECT_EQ(stream.bytes().data(), buffer.data());
    EXPECT_EQ(stream.bytes().size(), 2u);
}
