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
    const Array<u8, 4> expected { 0, 1, 2, 3 };
    Array<u8, 4> actual;

    InputMemoryStream stream { expected };

    stream >> actual[0] >> actual[1] >> actual[2] >> actual[3];
    EXPECT(!stream.has_any_error() && stream.eof());

    EXPECT_EQ(expected, actual);
}

TEST_CASE(seeking_slicing_offset)
{
    const Array<u8, 8> input { 0, 1, 2, 3, 4, 5, 6, 7 };
    const Array<u8, 4> expected0 { 0, 1, 2, 3 };
    const Array<u8, 4> expected1 { 4, 5, 6, 7 };
    const Array<u8, 4> expected2 { 1, 2, 3, 4 };

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

TEST_CASE(duplex_simple)
{
    DuplexMemoryStream stream;

    EXPECT(stream.eof());
    stream << 42;
    EXPECT(!stream.eof());

    int value;
    stream >> value;
    EXPECT_EQ(value, 42);
    EXPECT(stream.eof());
}

TEST_CASE(duplex_large_buffer)
{
    DuplexMemoryStream stream;

    Array<u8, 1024> one_kibibyte;

    EXPECT_EQ(stream.size(), 0ul);

    for (size_t idx = 0; idx < 256; ++idx)
        stream << one_kibibyte;

    EXPECT_EQ(stream.size(), 256 * 1024ul);

    for (size_t idx = 0; idx < 128; ++idx)
        stream >> one_kibibyte;

    EXPECT_EQ(stream.size(), 128 * 1024ul);

    for (size_t idx = 0; idx < 128; ++idx)
        stream >> one_kibibyte;

    EXPECT(stream.eof());
}

TEST_CASE(read_endian_values)
{
    const Array<u8, 8> input { 0, 1, 2, 3, 4, 5, 6, 7 };
    InputMemoryStream stream { input };

    LittleEndian<u32> value1;
    BigEndian<u32> value2;
    stream >> value1 >> value2;

    EXPECT_EQ(value1, 0x03020100u);
    EXPECT_EQ(value2, 0x04050607u);
}

TEST_CASE(write_endian_values)
{
    const Array<u8, 8> expected { 4, 3, 2, 1, 1, 2, 3, 4 };

    DuplexMemoryStream stream;
    stream << LittleEndian<u32> { 0x01020304 } << BigEndian<u32> { 0x01020304 };

    EXPECT_EQ(stream.size(), 8u);
    EXPECT(expected.span() == stream.copy_into_contiguous_buffer().span());
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

TEST_CASE(offset_of_out_of_bounds)
{
    Array<u8, 4> target { 0xff, 0xff, 0xff, 0xff };

    Array<u8, DuplexMemoryStream::chunk_size> whole_chunk;
    whole_chunk.span().fill(0);

    DuplexMemoryStream stream;

    stream << whole_chunk;

    EXPECT(!stream.offset_of(target).has_value());
}

TEST_CASE(unsigned_integer_underflow_regression)
{
    Array<u8, DuplexMemoryStream::chunk_size + 1> buffer;

    DuplexMemoryStream stream;
    stream << buffer;
}

TEST_CASE(offset_calculation_error_regression)
{
    Array<u8, DuplexMemoryStream::chunk_size> input, output;
    input.span().fill(0xff);

    DuplexMemoryStream stream;
    stream << 0x00000000 << input << 0x00000000;

    stream.discard_or_error(sizeof(int));
    stream.read(output);

    EXPECT_EQ(input, output);
}
