/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/Utf8View.h>

TEST_CASE(decode_ascii)
{
    Utf8View utf8 { "Hello World!11" };
    EXPECT(utf8.validate());

    u32 expected[] = { 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33, 49, 49 };
    size_t expected_size = sizeof(expected) / sizeof(expected[0]);

    size_t i = 0;
    for (u32 codepoint : utf8) {
        ASSERT(i < expected_size);
        EXPECT_EQ(codepoint, expected[i]);
        i++;
    }
    EXPECT_EQ(i, expected_size);
}

TEST_CASE(decode_utf8)
{
    Utf8View utf8 { "Привет, мир! 😀 γειά σου κόσμος こんにちは世界" };
    EXPECT(utf8.validate());

    u32 expected[] = { 1055, 1088, 1080, 1074, 1077, 1090, 44, 32, 1084, 1080, 1088, 33, 32, 128512, 32, 947, 949, 953, 940, 32, 963, 959, 965, 32, 954, 972, 963, 956, 959, 962, 32, 12371, 12435, 12395, 12385, 12399, 19990, 30028 };
    size_t expected_size = sizeof(expected) / sizeof(expected[0]);

    size_t i = 0;
    for (u32 codepoint : utf8) {
        ASSERT(i < expected_size);
        EXPECT_EQ(codepoint, expected[i]);
        i++;
    }
    EXPECT_EQ(i, expected_size);
}

TEST_CASE(validate_invalid_ut8)
{
    char invalid_utf8_1[] = { 42, 35, (char)182, 9, 0 };
    Utf8View utf8_1 { invalid_utf8_1 };
    EXPECT(!utf8_1.validate());

    char invalid_utf8_2[] = { 42, 35, (char)208, (char)208, 0 };
    Utf8View utf8_2 { invalid_utf8_2 };
    EXPECT(!utf8_2.validate());

    char invalid_utf8_3[] = { (char)208, 0 };
    Utf8View utf8_3 { invalid_utf8_3 };
    EXPECT(!utf8_3.validate());

    char invalid_utf8_4[] = { (char)208, 35, 0 };
    Utf8View utf8_4 { invalid_utf8_4 };
    EXPECT(!utf8_4.validate());
}

TEST_MAIN(UTF8)
