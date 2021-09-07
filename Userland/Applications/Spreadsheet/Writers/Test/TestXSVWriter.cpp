/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include "../CSV.h"
#include "../XSV.h"
#include <AK/MemoryStream.h>

TEST_CASE(can_write)
{
    Vector<Vector<int>> data = {
        { 1, 2, 3 },
        { 4, 5, 6 },
        { 7, 8, 9 },
    };

    auto buffer = ByteBuffer::create_uninitialized(1024).release_value();
    OutputMemoryStream stream { buffer };

    Writer::CSV csv(stream, data);

    auto expected_output = R"~(1,2,3
4,5,6
7,8,9
)~";

    EXPECT_EQ(StringView { stream.bytes() }, expected_output);
}

TEST_CASE(can_write_with_header)
{
    Vector<Vector<int>> data = {
        { 1, 2, 3 },
        { 4, 5, 6 },
        { 7, 8, 9 },
    };

    auto buffer = ByteBuffer::create_uninitialized(1024).release_value();
    OutputMemoryStream stream { buffer };

    Writer::CSV csv(stream, data, { "A", "B\"", "C" });

    auto expected_output = R"~(A,"B""",C
1,2,3
4,5,6
7,8,9
)~";

    EXPECT_EQ(StringView { stream.bytes() }, expected_output);
}

TEST_CASE(can_write_with_different_behaviors)
{
    Vector<Vector<String>> data = {
        { "Well", "Hello\"", "Friends" },
        { "We\"ll", "Hello,", "   Friends" },
    };

    auto buffer = ByteBuffer::create_uninitialized(1024).release_value();
    OutputMemoryStream stream { buffer };

    Writer::CSV csv(stream, data, { "A", "B\"", "C" }, Writer::WriterBehavior::QuoteOnlyInFieldStart | Writer::WriterBehavior::WriteHeaders);

    auto expected_output = R"~(A,B",C
Well,Hello",Friends
We"ll,"Hello,",   Friends
)~";

    EXPECT_EQ(StringView { stream.bytes() }, expected_output);
}
