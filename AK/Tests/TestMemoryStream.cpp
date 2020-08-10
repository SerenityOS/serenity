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

#include <AK/Stream.h>

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

    EXPECT(!stream.error() && !stream.fatal() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(recoverable_error)
{
    u32 expected = 0x01020304, actual = 0;
    u64 to_large_value = 0;

    InputMemoryStream stream { { &expected, sizeof(expected) } };

    EXPECT(!stream.error() && !stream.fatal() && !stream.eof());
    stream >> to_large_value;
    EXPECT(stream.error() && !stream.fatal() && !stream.eof());

    EXPECT(stream.handle_error());
    EXPECT(!stream.error() && !stream.fatal() && !stream.eof());

    stream >> actual;
    EXPECT(!stream.error() && !stream.fatal() && stream.eof());
    EXPECT_EQ(expected, actual);
}

TEST_CASE(chain_stream_operator)
{
    u8 expected[] { 0, 1, 2, 3 }, actual[4];

    InputMemoryStream stream { { expected, sizeof(expected) } };

    stream >> actual[0] >> actual[1] >> actual[2] >> actual[3];
    EXPECT(!stream.error() && !stream.fatal() && stream.eof());

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
    EXPECT(!stream.error() && !stream.fatal() && !stream.eof());
    EXPECT(compare({ expected0, sizeof(expected0) }, { actual0, sizeof(actual0) }));

    stream.seek(4);
    stream >> Bytes { actual1, sizeof(actual1) };
    EXPECT(!stream.error() && !stream.fatal() && stream.eof());
    EXPECT(compare({ expected1, sizeof(expected1) }, { actual1, sizeof(actual1) }));

    stream.seek(1);
    stream >> Bytes { actual2, sizeof(actual2) };
    EXPECT(!stream.error() && !stream.fatal() && !stream.eof());
    EXPECT(compare({ expected2, sizeof(expected2) }, { actual2, sizeof(actual2) }));
}

TEST_MAIN(MemoryStream)
