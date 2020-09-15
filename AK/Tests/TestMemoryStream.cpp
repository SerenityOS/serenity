/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include <AK/Array.h>
#include <AK/MemoryStream.h>

static bool compare(ReadonlyBytes lhs, ReadonlyBytes rhs)
{
    if (lhs.size() != rhs.size())
        return false;

    for (size_t idx = 0; idx < lhs.size(); ++idx) {
        if (lhs[idx] != rhs[idx])
            return false;
    }

    return true;
}

TEST_CASE(read_an_integer)
{
    u32 expected = 0x01020304, actual;

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
    u8 expected[] { 0, 1, 2, 3 }, actual[4];

    InputMemoryStream stream { { expected, sizeof(expected) } };

    stream >> actual[0] >> actual[1] >> actual[2] >> actual[3];
    EXPECT(!stream.has_any_error() && stream.eof());

    EXPECT(compare({ expected, sizeof(expected) }, { actual, sizeof(actual) }));
}

TEST_CASE(seeking_slicing_offset)
{
    u8 input[] { 0, 1, 2, 3, 4, 5, 6, 7 },
        expected0[] { 0, 1, 2, 3 },
        expected1[] { 4, 5, 6, 7 },
        expected2[] { 1, 2, 3, 4 },
        actual0[4], actual1[4], actual2[4];

    InputMemoryStream stream { { input, sizeof(input) } };

    stream >> Bytes { actual0, sizeof(actual0) };
    EXPECT(!stream.has_any_error() && !stream.eof());
    EXPECT(compare({ expected0, sizeof(expected0) }, { actual0, sizeof(actual0) }));

    stream.seek(4);
    stream >> Bytes { actual1, sizeof(actual1) };
    EXPECT(!stream.has_any_error() && stream.eof());
    EXPECT(compare({ expected1, sizeof(expected1) }, { actual1, sizeof(actual1) }));

    stream.seek(1);
    stream >> Bytes { actual2, sizeof(actual2) };
    EXPECT(!stream.has_any_error() && !stream.eof());
    EXPECT(compare({ expected2, sizeof(expected2) }, { actual2, sizeof(actual2) }));
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
    const u8 input[] { 0, 1, 2, 3, 4, 5, 6, 7 };
    InputMemoryStream stream { { input, sizeof(input) } };

    LittleEndian<u32> value1;
    BigEndian<u32> value2;
    stream >> value1 >> value2;

    EXPECT_EQ(value1, 0x03020100u);
    EXPECT_EQ(value2, 0x04050607u);
}

TEST_CASE(write_endian_values)
{
    const u8 expected[] { 4, 3, 2, 1, 1, 2, 3, 4 };

    DuplexMemoryStream stream;
    stream << LittleEndian<u32> { 0x01020304 } << BigEndian<u32> { 0x01020304 };

    EXPECT_EQ(stream.size(), 8u);
    EXPECT(compare({ expected, sizeof(expected) }, stream.copy_into_contiguous_buffer()));
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

TEST_MAIN(MemoryStream)
