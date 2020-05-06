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

TEST_MAIN(Bitmap)
