/*
 * Copyright (c) 2021, the SerenityOS developers.
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
#include <AK/Find.h>
#include <AK/Vector.h>

TEST_CASE(should_return_end_if_not_in_container)
{
    constexpr Array<int, 10> a {};

    static_assert(a.end() == AK::find(a.begin(), a.end(), 1));

    EXPECT(a.end() == AK::find(a.begin(), a.end(), 1));
}

TEST_CASE(should_return_iterator_to_first_matching_value_in_container)
{
    static constexpr Array<int, 10> a { 1, 2, 3, 4, 0, 6, 7, 8, 0, 0 };

    constexpr auto expected = a.begin() + 4;

    static_assert(expected == AK::find(a.begin(), a.end(), 0));

    EXPECT(expected == AK::find(a.begin(), a.end(), 0));
}

TEST_CASE(should_return_iterator_to_first_predicate_matching_value_in_container)
{
    static constexpr Array<int, 10> a { 1, 2, 3, 4, 0, 6, 7, 8, 0, 0 };

    constexpr auto expected = a.begin() + 4;

    static_assert(expected == AK::find_if(a.begin(), a.end(), [](auto v) { return v == 0; }));

    EXPECT(expected == AK::find_if(a.begin(), a.end(), [](auto v) { return v == 0; }));

    auto find_me = 8;
    EXPECT(find_me == *AK::find_if(a.begin(), a.end(), [&](auto v) { return v == find_me; }));
}

TEST_CASE(should_return_index_to_first_predicate_matching_value_in_container)
{
    static constexpr Array<int, 10> a { 1, 2, 3, 4, 0, 6, 7, 8, 0, 0 };

    static_assert(4 == AK::find_index(a.begin(), a.end(), 0));

    EXPECT(4 == AK::find_index(a.begin(), a.end(), 0));
}

TEST_MAIN(Find)
