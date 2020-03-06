/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/Queue.h>

TEST_CASE(construct)
{
    EXPECT(Queue<int>().is_empty());
    EXPECT(Queue<int>().size() == 0);
}

TEST_CASE(populate_int)
{
    Queue<int> ints;
    ints.enqueue(1);
    ints.enqueue(2);
    ints.enqueue(3);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(ints.dequeue(), 1);
    EXPECT_EQ(ints.size(), 2u);
    EXPECT_EQ(ints.dequeue(), 2);
    EXPECT_EQ(ints.size(), 1u);
    EXPECT_EQ(ints.dequeue(), 3);
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(populate_string)
{
    Queue<String> strings;
    strings.enqueue("ABC");
    strings.enqueue("DEF");
    EXPECT_EQ(strings.size(), 2u);
    EXPECT_EQ(strings.dequeue(), "ABC");
    EXPECT_EQ(strings.dequeue(), "DEF");
    EXPECT(strings.is_empty());
}

TEST_CASE(order)
{
    Queue<String> strings;
    EXPECT(strings.is_empty());

    for (size_t i = 0; i < 10000; ++i) {
        strings.enqueue(String::number(i));
        EXPECT_EQ(strings.size(), i + 1);
    }

    for (int i = 0; i < 10000; ++i) {
        bool ok;
        EXPECT_EQ(strings.dequeue().to_int(ok), i);
    }

    EXPECT(strings.is_empty());
}

TEST_MAIN(Queue)
