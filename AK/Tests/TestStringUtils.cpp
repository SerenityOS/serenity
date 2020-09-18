/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
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

#include <AK/StringUtils.h>
#include <AK/TestSuite.h>

TEST_CASE(matches_null)
{
    EXPECT(AK::StringUtils::matches(StringView(), StringView()));

    EXPECT(!AK::StringUtils::matches(StringView(), ""));
    EXPECT(!AK::StringUtils::matches(StringView(), "*"));
    EXPECT(!AK::StringUtils::matches(StringView(), "?"));
    EXPECT(!AK::StringUtils::matches(StringView(), "a"));

    EXPECT(!AK::StringUtils::matches("", StringView()));
    EXPECT(!AK::StringUtils::matches("a", StringView()));
}

TEST_CASE(matches_empty)
{
    EXPECT(AK::StringUtils::matches("", ""));

    EXPECT(AK::StringUtils::matches("", "*"));
    EXPECT(!AK::StringUtils::matches("", "?"));
    EXPECT(!AK::StringUtils::matches("", "a"));

    EXPECT(!AK::StringUtils::matches("a", ""));
}

TEST_CASE(matches_case_sensitive)
{
    EXPECT(AK::StringUtils::matches("a", "a", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::matches("a", "A", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::matches("A", "a", CaseSensitivity::CaseSensitive));
}

TEST_CASE(matches_case_insensitive)
{
    EXPECT(!AK::StringUtils::matches("aa", "a"));
    EXPECT(AK::StringUtils::matches("aa", "*"));
    EXPECT(!AK::StringUtils::matches("cb", "?a"));
    EXPECT(AK::StringUtils::matches("adceb", "a*b"));
    EXPECT(!AK::StringUtils::matches("acdcb", "a*c?b"));
}

TEST_CASE(convert_to_int)
{
    auto value = AK::StringUtils::convert_to_int(StringView());
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_int("");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_int("a");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_int("+");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_int("-");
    EXPECT(!value.has_value());

    auto actual = AK::StringUtils::convert_to_int("0");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 0);

    actual = AK::StringUtils::convert_to_int("1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = AK::StringUtils::convert_to_int("+1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = AK::StringUtils::convert_to_int("-1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), -1);

    actual = AK::StringUtils::convert_to_int("01");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1);

    actual = AK::StringUtils::convert_to_int("12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 12345);

    actual = AK::StringUtils::convert_to_int("-12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), -12345);
}

TEST_CASE(convert_to_uint)
{
    auto value = AK::StringUtils::convert_to_uint(StringView());
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("a");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("+");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("-");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("+1");
    EXPECT(!value.has_value());

    value = AK::StringUtils::convert_to_uint("-1");
    EXPECT(!value.has_value());

    auto actual = AK::StringUtils::convert_to_uint("0");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 0u);

    actual = AK::StringUtils::convert_to_uint("1");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1u);

    actual = AK::StringUtils::convert_to_uint("01");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 1u);

    actual = AK::StringUtils::convert_to_uint("12345");
    EXPECT_EQ(actual.has_value(), true);
    EXPECT_EQ(actual.value(), 12345u);
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    EXPECT(AK::StringUtils::ends_with(test_string, "DEF", CaseSensitivity::CaseSensitive));
    EXPECT(AK::StringUtils::ends_with(test_string, "ABCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::ends_with(test_string, "ABCDE", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::ends_with(test_string, "ABCDEFG", CaseSensitivity::CaseSensitive));
    EXPECT(AK::StringUtils::ends_with(test_string, "def", CaseSensitivity::CaseInsensitive));
    EXPECT(!AK::StringUtils::ends_with(test_string, "def", CaseSensitivity::CaseSensitive));
}

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    EXPECT(AK::StringUtils::starts_with(test_string, "ABC", CaseSensitivity::CaseSensitive));
    EXPECT(AK::StringUtils::starts_with(test_string, "ABCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::starts_with(test_string, "BCDEF", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::starts_with(test_string, "ABCDEFG", CaseSensitivity::CaseSensitive));
    EXPECT(AK::StringUtils::starts_with(test_string, "abc", CaseSensitivity::CaseInsensitive));
    EXPECT(!AK::StringUtils::starts_with(test_string, "abc", CaseSensitivity::CaseSensitive));
}

TEST_MAIN(StringUtils)
