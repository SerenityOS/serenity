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

    auto buffer = ByteBuffer::create_uninitialized(1024);
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

    auto buffer = ByteBuffer::create_uninitialized(1024);
    OutputMemoryStream stream { buffer };

    Writer::CSV csv(stream, data, { "A", "B\"", "C" });

    auto expected_output = R"~(A,"B""",C
1,2,3
4,5,6
7,8,9
)~";

    EXPECT_EQ(StringView { stream.bytes() }, expected_output);
}

TEST_CASE(can_write_with_different_behaviours)
{
    Vector<Vector<String>> data = {
        { "Well", "Hello\"", "Friends" },
        { "We\"ll", "Hello,", "   Friends" },
    };

    auto buffer = ByteBuffer::create_uninitialized(1024);
    OutputMemoryStream stream { buffer };

    Writer::CSV csv(stream, data, { "A", "B\"", "C" }, Writer::WriterBehaviour::QuoteOnlyInFieldStart | Writer::WriterBehaviour::WriteHeaders);

    auto expected_output = R"~(A,B",C
Well,Hello",Friends
We"ll,"Hello,",   Friends
)~";

    EXPECT_EQ(StringView { stream.bytes() }, expected_output);
}

TEST_MAIN(XSV)
