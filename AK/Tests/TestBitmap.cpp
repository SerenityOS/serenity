/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Bitmap.h>

TEST_CASE(construct_empty)
{
    Bitmap bitmap;
    EXPECT_EQ(bitmap.size(), 0u);
}

TEST_CASE(find_first_set)
{
    Bitmap bitmap(128, false);
    bitmap.set(69, true);
    EXPECT_EQ(bitmap.find_first_set().value(), 69u);
}

TEST_CASE(find_first_unset)
{
    Bitmap bitmap(128, true);
    bitmap.set(51, false);
    EXPECT_EQ(bitmap.find_first_unset().value(), 51u);
}

TEST_CASE(find_one_anywhere_set)
{
    {
        Bitmap bitmap(168, false);
        bitmap.set(34, true);
        bitmap.set(97, true);
        EXPECT_EQ(bitmap.find_one_anywhere_set(0).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(31).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(32).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(34).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(36).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(63).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(64).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(96).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(96).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(97).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(127).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(128).value(), 34u);
    }
    {
        Bitmap bitmap(128 + 24, false);
        bitmap.set(34, true);
        bitmap.set(126, true);
        EXPECT_EQ(bitmap.find_one_anywhere_set(0).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(63).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_set(64).value(), 126u);
    }
    {
        Bitmap bitmap(32, false);
        bitmap.set(12, true);
        bitmap.set(24, true);
        auto got = bitmap.find_one_anywhere_set(0).value();
        EXPECT(got == 12 || got == 24);
    }
}

TEST_CASE(find_one_anywhere_unset)
{
    {
        Bitmap bitmap(168, true);
        bitmap.set(34, false);
        bitmap.set(97, false);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(0).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(31).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(32).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(34).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(36).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(63).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(64).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(96).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(96).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(97).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(127).value(), 97u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(128).value(), 34u);
    }
    {
        Bitmap bitmap(128 + 24, true);
        bitmap.set(34, false);
        bitmap.set(126, false);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(0).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(63).value(), 34u);
        EXPECT_EQ(bitmap.find_one_anywhere_unset(64).value(), 126u);
    }
    {
        Bitmap bitmap(32, true);
        bitmap.set(12, false);
        bitmap.set(24, false);
        auto got = bitmap.find_one_anywhere_unset(0).value();
        EXPECT(got == 12 || got == 24);
    }
}

TEST_CASE(find_first_range)
{
    Bitmap bitmap(128, true);
    bitmap.set(47, false);
    bitmap.set(48, false);
    bitmap.set(49, false);
    bitmap.set(50, false);
    bitmap.set(51, false);
    size_t found_range_size = 0;
    auto result = bitmap.find_longest_range_of_unset_bits(5, found_range_size);
    EXPECT_EQ(result.has_value(), true);
    EXPECT_EQ(found_range_size, 5u);
    EXPECT_EQ(result.value(), 47u);
}

TEST_CASE(set_range)
{
    {
        Bitmap bitmap(128, false);
        bitmap.set_range(41, 10, true);
        EXPECT_EQ(bitmap.get(40), false);
        EXPECT_EQ(bitmap.get(41), true);
        EXPECT_EQ(bitmap.get(42), true);
        EXPECT_EQ(bitmap.get(43), true);
        EXPECT_EQ(bitmap.get(44), true);
        EXPECT_EQ(bitmap.get(45), true);
        EXPECT_EQ(bitmap.get(46), true);
        EXPECT_EQ(bitmap.get(47), true);
        EXPECT_EQ(bitmap.get(48), true);
        EXPECT_EQ(bitmap.get(49), true);
        EXPECT_EQ(bitmap.get(50), true);
        EXPECT_EQ(bitmap.get(51), false);
    }
    {
        Bitmap bitmap(288, false);
        bitmap.set_range(48, 32, true);
        bitmap.set_range(94, 39, true);
        bitmap.set_range(190, 71, true);
        bitmap.set_range(190 + 71 - 7, 21, false); // slightly overlapping clear
        for (size_t i = 0; i < bitmap.size(); i++) {
            bool should_be_set = (i >= 48 && i < 48 + 32)
                || (i >= 94 && i < 94 + 39)
                || ((i >= 190 && i < 190 + 71) && !(i >= 190 + 71 - 7 && i < 190 + 71 - 7 + 21));
            EXPECT_EQ(bitmap.get(i), should_be_set);
        }
        EXPECT_EQ(bitmap.count_slow(true), 32u + 39u + 71u - 7u);
    }
}

TEST_CASE(find_first_fit)
{
    {
        Bitmap bitmap(32, true);
        auto fit = bitmap.find_first_fit(1);
        EXPECT_EQ(fit.has_value(), false);
    }
    {
        Bitmap bitmap(32, true);
        bitmap.set(31, false);
        auto fit = bitmap.find_first_fit(1);
        EXPECT_EQ(fit.has_value(), true);
        EXPECT_EQ(fit.value(), 31u);
    }

    for (size_t i = 0; i < 128; ++i) {
        Bitmap bitmap(128, true);
        bitmap.set(i, false);
        auto fit = bitmap.find_first_fit(1);
        EXPECT_EQ(fit.has_value(), true);
        EXPECT_EQ(fit.value(), i);
    }

    for (size_t i = 0; i < 127; ++i) {
        Bitmap bitmap(128, true);
        bitmap.set(i, false);
        bitmap.set(i + 1, false);
        auto fit = bitmap.find_first_fit(2);
        EXPECT_EQ(fit.has_value(), true);
        EXPECT_EQ(fit.value(), i);
    }

    size_t bitmap_size = 1024;
    for (size_t chunk_size = 1; chunk_size < 64; ++chunk_size) {
        for (size_t i = 0; i < bitmap_size - chunk_size; ++i) {
            Bitmap bitmap(bitmap_size, true);
            for (size_t c = 0; c < chunk_size; ++c)
                bitmap.set(i + c, false);
            auto fit = bitmap.find_first_fit(chunk_size);
            EXPECT_EQ(fit.has_value(), true);
            EXPECT_EQ(fit.value(), i);
        }
    }
}

TEST_CASE(find_longest_range_of_unset_bits_edge)
{
    Bitmap bitmap(36, true);
    bitmap.set_range(32, 4, false);
    size_t found_range_size = 0;
    auto result = bitmap.find_longest_range_of_unset_bits(1, found_range_size);
    EXPECT_EQ(result.has_value(), true);
    EXPECT_EQ(result.value(), 32u);
}

TEST_CASE(count_in_range)
{
    Bitmap bitmap(256, false);
    bitmap.set(14, true);
    bitmap.set(17, true);
    bitmap.set(19, true);
    bitmap.set(20, true);
    for (size_t i = 34; i < 250; i++) {
        if (i < 130 || i > 183)
            bitmap.set(i, true);
    }

    auto count_bits_slow = [](const Bitmap& b, size_t start, size_t len, bool value) -> size_t {
        size_t count = 0;
        for (size_t i = start; i < start + len; i++) {
            if (b.get(i) == value)
                count++;
        }
        return count;
    };
    auto test_with_value = [&](bool value) {
        auto do_test = [&](size_t start, size_t len) {
            EXPECT_EQ(bitmap.count_in_range(start, len, value), count_bits_slow(bitmap, start, len, value));
        };
        do_test(16, 2);
        do_test(16, 3);
        do_test(16, 4);

        for (size_t start = 8; start < 24; start++) {
            for (size_t end = 9; end < 25; end++) {
                if (start >= end)
                    continue;
                do_test(start, end - start);
            }
        }

        for (size_t start = 1; start <= 9; start++) {
            for (size_t i = start + 1; i < bitmap.size() - start + 1; i++)
                do_test(start, i - start);
        }
    };
    test_with_value(true);
    test_with_value(false);
}

TEST_MAIN(Bitmap)
