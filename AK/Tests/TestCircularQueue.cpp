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

#include <AK/CircularQueue.h>
#include <AK/String.h>

TEST_CASE(basic)
{
    CircularQueue<int, 3> ints;
    EXPECT(ints.is_empty());
    ints.enqueue(1);
    ints.enqueue(2);
    ints.enqueue(3);
    EXPECT_EQ(ints.size(), 3u);

    ints.enqueue(4);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(ints.dequeue(), 2);
    EXPECT_EQ(ints.dequeue(), 3);
    EXPECT_EQ(ints.dequeue(), 4);
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(complex_type)
{
    CircularQueue<String, 2> strings;

    strings.enqueue("ABC");
    strings.enqueue("DEF");

    EXPECT_EQ(strings.size(), 2u);

    strings.enqueue("abc");
    strings.enqueue("def");

    EXPECT_EQ(strings.dequeue(), "abc");
    EXPECT_EQ(strings.dequeue(), "def");
}

TEST_CASE(complex_type_clear)
{
    CircularQueue<String, 5> strings;
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    EXPECT_EQ(strings.size(), 5u);
    strings.clear();
    EXPECT_EQ(strings.size(), 0u);
}

TEST_MAIN(CircularQueue)
