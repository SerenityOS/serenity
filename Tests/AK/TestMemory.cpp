/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteString.h>
#include <AK/MemMem.h>
#include <AK/Memory.h>

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

    auto haystack_string = "Main function must return c_int\n"sv;
    auto needle_string = "Main function must return c_int"sv;

    auto result = AK::Detail::bitap_bitwise(haystack_string.characters_without_null_termination(), haystack_string.length(), needle_string.characters_without_null_termination(), needle_string.length());

    EXPECT_NE(result, nullptr);
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

TEST_CASE(kmp_reverse_one_chunk)
{
    Array<u8, 8> haystack { 1, 0, 1, 2, 3, 4, 5, 0 };
    Array<Array<u8, 8>, 1> haystack_arr { haystack };
    Array<u8, 4> needle_0 { 2, 3, 4, 5 };
    Array<u8, 4> needle_1 { 1, 2, 3, 4 };
    Array<u8, 4> needle_2 { 3, 4, 5, 0 };
    Array<u8, 4> needle_3 { 3, 4, 5, 6 };

    auto result_0 = AK::memmem_reverse(haystack_arr.begin(), haystack_arr.end(), needle_0);
    auto result_1 = AK::memmem_reverse(haystack_arr.begin(), haystack_arr.end(), needle_1);
    auto result_2 = AK::memmem_reverse(haystack_arr.begin(), haystack_arr.end(), needle_2);
    auto result_3 = AK::memmem_reverse(haystack_arr.begin(), haystack_arr.end(), needle_3);

    EXPECT_EQ(result_0.value_or(9), 5u);
    EXPECT_EQ(result_1.value_or(9), 6u);
    EXPECT_EQ(result_2.value_or(9), 4u);
    EXPECT(!result_3.has_value());
}

TEST_CASE(kmp_reverse_two_chunks)
{
    Array<u8, 4> haystack_first_half { 1, 0, 1, 2 }, haystack_second_half { 3, 4, 5, 0 };
    Array<Array<u8, 4>, 2> haystack { haystack_second_half, haystack_first_half };
    Array<u8, 4> needle_0 { 2, 3, 4, 5 };
    Array<u8, 4> needle_1 { 1, 2, 3, 4 };
    Array<u8, 4> needle_2 { 3, 4, 5, 0 };
    Array<u8, 4> needle_3 { 3, 4, 5, 6 };

    auto result_0 = AK::memmem_reverse(haystack.begin(), haystack.end(), needle_0);
    auto result_1 = AK::memmem_reverse(haystack.begin(), haystack.end(), needle_1);
    auto result_2 = AK::memmem_reverse(haystack.begin(), haystack.end(), needle_2);
    auto result_3 = AK::memmem_reverse(haystack.begin(), haystack.end(), needle_3);

    EXPECT_EQ(result_0.value_or(9), 5u);
    EXPECT_EQ(result_1.value_or(9), 6u);
    EXPECT_EQ(result_2.value_or(9), 4u);
    EXPECT(!result_3.has_value());
}

TEST_CASE(kmp_match_order)
{
    Array<u8, 4> haystack_first_half { 1, 0, 1, 2 }, haystack_second_half { 3, 4, 5, 0 };
    Array<Array<u8, 4>, 2> haystack_f { haystack_first_half, haystack_second_half };
    Array<Array<u8, 4>, 2> haystack_b { haystack_second_half, haystack_first_half };

    Array<u8, 1> needle_0 { 0 };
    auto result_0_f = AK::memmem(haystack_f.begin(), haystack_f.end(), needle_0);
    auto result_0_b = AK::memmem_reverse(haystack_b.begin(), haystack_b.end(), needle_0);
    EXPECT_EQ(result_0_f.value_or(9), 1u);
    EXPECT_EQ(result_0_b.value_or(9), 1u);

    Array<u8, 1> needle_1 { 1 };
    auto result_1_f = AK::memmem(haystack_f.begin(), haystack_f.end(), needle_1);
    auto result_1_b = AK::memmem_reverse(haystack_b.begin(), haystack_b.end(), needle_1);
    EXPECT_EQ(result_1_f.value_or(9), 0u);
    EXPECT_EQ(result_1_b.value_or(9), 6u);
}

TEST_CASE(timing_safe_compare)
{
    ByteString data_set = "abcdefghijklmnopqrstuvwxyz123456789";
    EXPECT_EQ(true, AK::timing_safe_compare(data_set.characters(), data_set.characters(), data_set.length()));

    ByteString reversed = data_set.reverse();
    EXPECT_EQ(false, AK::timing_safe_compare(data_set.characters(), reversed.characters(), reversed.length()));
}
