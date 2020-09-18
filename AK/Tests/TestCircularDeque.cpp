/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
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

#include <AK/CircularDeque.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>

TEST_CASE(enqueue_begin)
{
    CircularDeque<int, 3> ints;

    ints.enqueue_begin(0);
    EXPECT_EQ(ints.size(), 1u);
    EXPECT_EQ(ints.first(), 0);

    ints.enqueue_begin(1);
    EXPECT_EQ(ints.size(), 2u);
    EXPECT_EQ(ints.first(), 1);
    EXPECT_EQ(ints.last(), 0);

    ints.enqueue_begin(2);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(ints.first(), 2);
    EXPECT_EQ(ints.last(), 0);

    ints.enqueue_begin(3);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(ints.first(), 3);
    EXPECT_EQ(ints.at(1), 2);
    EXPECT_EQ(ints.last(), 1);
}

TEST_CASE(enqueue_begin_being_moved_from)
{
    CircularDeque<String, 2> strings;

    String str { "test" };
    strings.enqueue_begin(move(str));
    EXPECT(str.is_null());
}

TEST_CASE(deque_end)
{
    CircularDeque<int, 3> ints;
    ints.enqueue(0);
    ints.enqueue(1);
    ints.enqueue(2);
    EXPECT_EQ(ints.size(), 3u);

    EXPECT_EQ(ints.dequeue_end(), 2);
    EXPECT_EQ(ints.size(), 2u);

    EXPECT_EQ(ints.dequeue_end(), 1);
    EXPECT_EQ(ints.size(), 1u);

    EXPECT_EQ(ints.dequeue_end(), 0);
    EXPECT(ints.is_empty());
}

TEST_MAIN(CircularDeque)
