/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <AK/BinaryHeap.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    BinaryHeap<int, int, 5> empty;
    EXPECT(empty.is_empty());
    EXPECT(empty.size() == 0);
}

TEST_CASE(construct_from_existing)
{
    int keys[] = { 3, 2, 1 };
    char values[] = { 'c', 'b', 'a' };
    BinaryHeap<int, char, 5> from_existing(keys, values, 3);
    EXPECT(from_existing.size() == 3);
    EXPECT_EQ(from_existing.pop_min(), 'a');
    EXPECT_EQ(from_existing.pop_min(), 'b');
    EXPECT_EQ(from_existing.pop_min(), 'c');
}

TEST_CASE(populate_int)
{
    BinaryHeap<int, int, 5> ints;
    ints.insert(1, 10);
    ints.insert(3, 20);
    ints.insert(2, 30);
    EXPECT_EQ(ints.size(), 3u);
    EXPECT_EQ(ints.pop_min(), 10);
    EXPECT_EQ(ints.size(), 2u);
    EXPECT_EQ(ints.pop_min(), 30);
    EXPECT_EQ(ints.size(), 1u);
    EXPECT_EQ(ints.pop_min(), 20);
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(populate_string)
{
    BinaryHeap<int, String, 5> strings;
    strings.insert(1, "ABC");
    strings.insert(2, "DEF");
    EXPECT_EQ(strings.size(), 2u);
    EXPECT_EQ(strings.pop_min(), "ABC");
    EXPECT_EQ(strings.pop_min(), "DEF");
    EXPECT(strings.is_empty());
}

TEST_CASE(large_populate_reverse)
{
    BinaryHeap<int, int, 1024> ints;
    for (int i = 1023; i >= 0; i--) {
        ints.insert(i, i);
    }
    EXPECT_EQ(ints.size(), 1024u);
    for (int i = 0; i < 1024; i++) {
        EXPECT_EQ(ints.peek_min(), i);
        EXPECT_EQ(ints.pop_min(), i);
    }
}

TEST_MAIN(BinaryHeap)
