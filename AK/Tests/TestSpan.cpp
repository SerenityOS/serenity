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

#include <AK/Checked.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <string.h>

TEST_CASE(constexpr_default_constructor_is_empty)
{
    constexpr Span<int> span;
    static_assert(span.is_empty(), "Default constructed span should be empty.");
}

TEST_CASE(implicit_converson_to_const)
{
    constexpr Bytes bytes0;
    [[maybe_unused]] constexpr ReadonlyBytes bytes2 = bytes0;
    [[maybe_unused]] constexpr ReadonlyBytes bytes3 = static_cast<ReadonlyBytes>(bytes2);
}

TEST_CASE(span_works_with_constant_types)
{
    static constexpr u8 buffer[4] { 1, 2, 3, 4 };
    constexpr ReadonlyBytes bytes { buffer, 4 };

    static_assert(IsConst<AK::RemoveReference<decltype(bytes[1])>::Type>::value);
    static_assert(bytes[2] == 3);
}

TEST_CASE(span_works_with_mutable_types)
{
    u8 buffer[4] { 1, 2, 3, 4 };
    Bytes bytes { buffer, 4 };

    EXPECT_EQ(bytes[2], 3);
    ++bytes[2];
    EXPECT_EQ(bytes[2], 4);
}

TEST_CASE(iterator_behaves_like_loop)
{
    u8 buffer[256];
    for (int idx = 0; idx < 256; ++idx) {
        buffer[idx] = static_cast<u8>(idx);
    }

    Bytes bytes { buffer, 256 };
    size_t idx = 0;
    for (auto iter = bytes.begin(); iter < bytes.end(); ++iter) {
        EXPECT_EQ(*iter, buffer[idx]);

        ++idx;
    }
}

TEST_CASE(modifying_is_possible)
{
    int values_before[8] = { 1, 2, 3, 4, 5, 6, 7, 8 };
    int values_after[8] = { 7, 6, 5, 4, 3, 2, 1, 0 };

    Span<int> span { values_before, 8 };
    for (auto& value : span) {
        value = 8 - value;
    }

    for (int idx = 0; idx < 8; ++idx) {
        EXPECT_EQ(values_before[idx], values_after[idx]);
    }
}

TEST_CASE(at_and_index_operator_return_same_value)
{
    u8 buffer[256];
    for (int idx = 0; idx < 256; ++idx) {
        buffer[idx] = static_cast<u8>(idx);
    }

    Bytes bytes { buffer, 256 };
    for (int idx = 0; idx < 256; ++idx) {
        EXPECT_EQ(buffer[idx], bytes[idx]);
        EXPECT_EQ(bytes[idx], bytes.at(idx));
    }
}

TEST_CASE(can_subspan_whole_span)
{
    static constexpr u8 buffer[16] {};
    constexpr ReadonlyBytes bytes { buffer, 16 };

    constexpr auto slice = bytes.slice(0, 16);

    static_assert(slice.data() == buffer);
    static_assert(slice.size() == 16u);
}

TEST_CASE(can_subspan_as_intended)
{
    static constexpr u16 buffer[8] { 1, 2, 3, 4, 5, 6, 7, 8 };

    constexpr Span<const u16> span { buffer, 8 };
    constexpr auto slice = span.slice(3, 2);

    static_assert(slice.size() == 2u);
    static_assert(slice[0] == 4);
    static_assert(slice[1] == 5);
}

TEST_CASE(span_from_void_pointer)
{
    int value = 0;
    [[maybe_unused]] Bytes bytes0 { reinterpret_cast<void*>(value), 4 };
    [[maybe_unused]] ReadonlyBytes bytes1 { reinterpret_cast<const void*>(value), 4 };
}

TEST_CASE(span_from_c_string)
{
    const char* str = "Serenity";
    [[maybe_unused]] ReadonlyBytes bytes { str, strlen(str) };
}

TEST_MAIN(Span)
