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

#include <AK/MemMem.h>

TEST_CASE(bitap)
{
    Array<u8, 8> haystack { 1, 0, 1, 2, 3, 4, 5, 0 };
    Array<u8, 4> needle_0 { 2, 3, 4, 5 };
    Array<u8, 4> needle_1 { 1, 2, 3, 4 };
    Array<u8, 4> needle_2 { 3, 4, 5, 0 };
    Array<u8, 4> needle_3 { 3, 4, 5, 6 };

    auto result_0 = AK::memmem(haystack.data(), haystack.size(), needle_0.data(), needle_0.size());
    auto result_1 = AK::memmem(haystack.data(), haystack.size(), needle_1.data(), needle_1.size());
    auto result_2 = AK::memmem(haystack.data(), haystack.size(), needle_2.data(), needle_2.size());
    auto result_3 = AK::memmem(haystack.data(), haystack.size(), needle_3.data(), needle_3.size());

    EXPECT_EQ(result_0, &haystack[3]);
    EXPECT_EQ(result_1, &haystack[2]);
    EXPECT_EQ(result_2, &haystack[4]);
    EXPECT_EQ(result_3, nullptr);
}

TEST_CASE(kmp_one_chunk)
{
    Array<u8, 8> haystack { 1, 0, 1, 2, 3, 4, 5, 0 };
    Array<Array<u8, 8>, 1> haystack_arr { haystack };
    Array<u8, 4> needle_0 { 2, 3, 4, 5 };
    Array<u8, 4> needle_1 { 1, 2, 3, 4 };
    Array<u8, 4> needle_2 { 3, 4, 5, 0 };
    Array<u8, 4> needle_3 { 3, 4, 5, 6 };

    auto result_0 = AK::memmem(haystack_arr.begin(), haystack_arr.end(), needle_0);
    auto result_1 = AK::memmem(haystack_arr.begin(), haystack_arr.end(), needle_1);
    auto result_2 = AK::memmem(haystack_arr.begin(), haystack_arr.end(), needle_2);
    auto result_3 = AK::memmem(haystack_arr.begin(), haystack_arr.end(), needle_3);

    EXPECT_EQ(result_0.value_or(9), 3u);
    EXPECT_EQ(result_1.value_or(9), 2u);
    EXPECT_EQ(result_2.value_or(9), 4u);
    EXPECT(!result_3.has_value());
}

TEST_CASE(kmp_two_chunks)
{
    Array<u8, 4> haystack_first_half { 1, 0, 1, 2 }, haystack_second_half { 3, 4, 5, 0 };
    Array<Array<u8, 4>, 2> haystack { haystack_first_half, haystack_second_half };
    Array<u8, 4> needle_0 { 2, 3, 4, 5 };
    Array<u8, 4> needle_1 { 1, 2, 3, 4 };
    Array<u8, 4> needle_2 { 3, 4, 5, 0 };
    Array<u8, 4> needle_3 { 3, 4, 5, 6 };

    auto result_0 = AK::memmem(haystack.begin(), haystack.end(), needle_0);
    auto result_1 = AK::memmem(haystack.begin(), haystack.end(), needle_1);
    auto result_2 = AK::memmem(haystack.begin(), haystack.end(), needle_2);
    auto result_3 = AK::memmem(haystack.begin(), haystack.end(), needle_3);

    EXPECT_EQ(result_0.value_or(9), 3u);
    EXPECT_EQ(result_1.value_or(9), 2u);
    EXPECT_EQ(result_2.value_or(9), 4u);
    EXPECT(!result_3.has_value());
}

TEST_MAIN(MemMem)
