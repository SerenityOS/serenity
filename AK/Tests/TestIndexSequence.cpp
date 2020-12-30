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

#include <AK/StdLibExtras.h>
#include <AK/TestSuite.h>
#include <AK/TypeList.h>

template<typename F, typename... Args>
F for_each_argument(F f, Args&&... args)
{
    (f(forward<Args>(args)), ...);
    return f;
}

template<typename T, T... ints>
void verify_sequence(IntegerSequence<T, ints...> seq, std::initializer_list<T> expected)
{
    EXPECT_EQ(seq.size(), expected.size());
    for_each_argument([idx = expected.begin()](T t) mutable { EXPECT_EQ(t, *(idx++)); }, ints...);
}

TEST_CASE(TestIndexSequence)
{
    constexpr auto integer_seq1 = IntegerSequence<int, 0, 1, 2, 3, 4> {};
    constexpr auto integer_seq2 = MakeIntegerSequence<int, 5> {};
    static_assert(IsSame<decltype(integer_seq1), decltype(integer_seq2)>::value, "");

    static_assert(integer_seq1.size() == 5, "");
    static_assert(integer_seq2.size() == 5, "");

    constexpr auto index_seq1 = IndexSequence<0, 1, 2> {};
    constexpr auto index_seq2 = MakeIndexSequence<3> {};
    static_assert(IsSame<decltype(index_seq1), decltype(index_seq2)>::value, "");

    verify_sequence(MakeIndexSequence<10> {}, std::initializer_list<unsigned> { 0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U, 8U, 9U });
    verify_sequence(MakeIntegerSequence<long, 16> {}, std::initializer_list<long> { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 });
}

TEST_CASE(TypeList)
{
    using MyTypes = TypeList<int, bool, char>;
    static_assert(IsSame<MyTypes::Type<0>, int>::value, "");
    static_assert(IsSame<MyTypes::Type<1>, bool>::value, "");
    static_assert(IsSame<MyTypes::Type<2>, char>::value, "");
}

TEST_MAIN(IndexSequence);
