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

#include <AK/BinarySearch.h>
#include <AK/Span.h>
#include <cstring>
#include <new>

TEST_CASE(vector_ints)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    auto test1 = *binary_search(ints, 1);
    auto test2 = *binary_search(ints, 2);
    auto test3 = *binary_search(ints, 3);
    EXPECT_EQ(test1, 1);
    EXPECT_EQ(test2, 2);
    EXPECT_EQ(test3, 3);
}

TEST_CASE(span_rvalue_reference)
{
    Array<long, 3> array { 1, 2, 3 };

    size_t nearby_index = 0;
    auto* pointer = binary_search(array.span(), 2, &nearby_index);

    EXPECT_EQ(nearby_index, 1u);
    EXPECT_EQ(pointer, &array[1]);
}

TEST_CASE(array_doubles)
{
    Array<double, 3> array { 1.1, 9.9, 33.33 };

    EXPECT_EQ(binary_search(array, 1.1), &array[0]);
    EXPECT_EQ(binary_search(array, 33.33), &array[2]);
    EXPECT_EQ(binary_search(array, 9.9), &array[1]);
}

TEST_CASE(vector_strings)
{
    Vector<String> strings;
    strings.append("bat");
    strings.append("cat");
    strings.append("dog");

    auto string_compare = [](const String& a, const String& b) -> int {
        return strcmp(a.characters(), b.characters());
    };
    auto test1 = *binary_search(strings, String("bat"), nullptr, string_compare);
    auto test2 = *binary_search(strings, String("cat"), nullptr, string_compare);
    auto test3 = *binary_search(strings, String("dog"), nullptr, string_compare);
    EXPECT_EQ(test1, String("bat"));
    EXPECT_EQ(test2, String("cat"));
    EXPECT_EQ(test3, String("dog"));
}

TEST_CASE(single_element)
{
    Vector<int> ints;
    ints.append(1);

    auto test1 = *binary_search(ints, 1);
    EXPECT_EQ(test1, 1);
}

TEST_CASE(not_found)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    auto test1 = binary_search(ints, -1);
    auto test2 = binary_search(ints, 0);
    auto test3 = binary_search(ints, 4);
    EXPECT_EQ(test1, nullptr);
    EXPECT_EQ(test2, nullptr);
    EXPECT_EQ(test3, nullptr);
}

TEST_CASE(no_elements)
{
    Vector<int> ints;

    auto test1 = binary_search(ints, 1);
    EXPECT_EQ(test1, nullptr);
}

TEST_CASE(constexpr_array_search)
{
    constexpr Array<int, 3> array = { 1, 17, 42 };

    static_assert(binary_search(array, 42) == &array[2]);
    static_assert(binary_search(array, 17) == &array[1]);
    static_assert(binary_search(array, 3) == nullptr);
}

TEST_CASE(unsigned_to_signed_regression)
{
    const Array<u32, 5> input { 0, 1, 2, 3, 4 };

    // The algorithm computes 1 - input[2] = -1, and if this is (incorrectly) cast
    // to an unsigned then it will look in the wrong direction and miss the 1.

    size_t nearby_index = 1;
    EXPECT_EQ(binary_search(input, 1u, &nearby_index), &input[1]);
    EXPECT_EQ(nearby_index, 1u);
}

TEST_MAIN(BinarySearch)
