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

    auto test1 = *binary_search(ints.span(), 1, AK::integral_compare<int>);
    auto test2 = *binary_search(ints.span(), 2, AK::integral_compare<int>);
    auto test3 = *binary_search(ints.span(), 3, AK::integral_compare<int>);
    EXPECT_EQ(test1, 1);
    EXPECT_EQ(test2, 2);
    EXPECT_EQ(test3, 3);
}

TEST_CASE(array_doubles)
{
    double doubles[] = { 1.1, 9.9, 33.33 };

    auto test1 = *binary_search(Span<double> { doubles, 3 }, 1.1, AK::integral_compare<double>);
    auto test2 = *binary_search(Span<double> { doubles, 3 }, 9.9, AK::integral_compare<double>);
    auto test3 = *binary_search(Span<double> { doubles, 3 }, 33.33, AK::integral_compare<double>);
    EXPECT_EQ(test1, 1.1);
    EXPECT_EQ(test2, 9.9);
    EXPECT_EQ(test3, 33.33);
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
    auto test1 = *binary_search(strings.span(), String("bat"), string_compare);
    auto test2 = *binary_search(strings.span(), String("cat"), string_compare);
    auto test3 = *binary_search(strings.span(), String("dog"), string_compare);
    EXPECT_EQ(test1, String("bat"));
    EXPECT_EQ(test2, String("cat"));
    EXPECT_EQ(test3, String("dog"));
}

TEST_CASE(single_element)
{
    Vector<int> ints;
    ints.append(1);

    auto test1 = *binary_search(ints.span(), 1, AK::integral_compare<int>);
    EXPECT_EQ(test1, 1);
}

TEST_CASE(not_found)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    auto test1 = binary_search(ints.span(), -1, AK::integral_compare<int>);
    auto test2 = binary_search(ints.span(), 0, AK::integral_compare<int>);
    auto test3 = binary_search(ints.span(), 4, AK::integral_compare<int>);
    EXPECT_EQ(test1, nullptr);
    EXPECT_EQ(test2, nullptr);
    EXPECT_EQ(test3, nullptr);
}

TEST_CASE(no_elements)
{
    Vector<int> ints;

    auto test1 = binary_search(ints.span(), 1, AK::integral_compare<int>);
    EXPECT_EQ(test1, nullptr);
}

TEST_CASE(huge_char_array)
{
    const size_t N = 2147483680;
    Bytes span { new (std::nothrow) u8[N], N };
    EXPECT(span.data() != nullptr);

    for (size_t i = 0; i < span.size(); ++i)
        span[i] = 'a';
    size_t index = N - 1;
    for (u8 c = 'z'; c > 'b'; --c)
        span[index--] = c;

    EXPECT_EQ(span[N - 1], 'z');

    const u8 a = 'a';
    auto where = binary_search(span, a, AK::integral_compare<u8>);
    EXPECT(where != nullptr);
    EXPECT_EQ(*where, 'a');

    const u8 z = 'z';
    where = binary_search(span, z, AK::integral_compare<u8>);
    EXPECT(where != nullptr);
    EXPECT_EQ(*where, 'z');

    size_t near = 0;
    const u8 tilde = '~';
    where = binary_search(span, tilde, AK::integral_compare<u8>, &near);
    EXPECT_EQ(where, nullptr);
    EXPECT_EQ(near, N - 1);

    const u8 b = 'b';
    where = binary_search(span, b, AK::integral_compare<u8>, &near);
    EXPECT_EQ(where, nullptr);
    EXPECT_EQ(near, N - ('z' - b + 1));

    delete[] span.data();
}

TEST_MAIN(BinarySearch)
