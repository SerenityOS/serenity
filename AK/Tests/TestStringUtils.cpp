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
    bool ok = false;
    AK::StringUtils::convert_to_int(StringView(), ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_int("", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_int("a", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_int("+", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_int("-", ok);
    EXPECT(!ok);

    int actual = AK::StringUtils::convert_to_int("0", ok);
    EXPECT(ok && actual == 0);

    actual = AK::StringUtils::convert_to_int("1", ok);
    EXPECT(ok && actual == 1);

    actual = AK::StringUtils::convert_to_int("+1", ok);
    EXPECT(ok && actual == 1);

    actual = AK::StringUtils::convert_to_int("-1", ok);
    EXPECT(ok && actual == -1);

    actual = AK::StringUtils::convert_to_int("01", ok);
    EXPECT(ok && actual == 1);

    actual = AK::StringUtils::convert_to_int("12345", ok);
    EXPECT(ok && actual == 12345);

    actual = AK::StringUtils::convert_to_int("-12345", ok);
    EXPECT(ok && actual == -12345);
}

TEST_CASE(convert_to_uint)
{
    bool ok = false;
    AK::StringUtils::convert_to_uint(StringView(), ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("a", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("+", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("-", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("+1", ok);
    EXPECT(!ok);

    AK::StringUtils::convert_to_uint("-1", ok);
    EXPECT(!ok);

    unsigned actual = AK::StringUtils::convert_to_uint("0", ok);
    EXPECT(ok && actual == 0u);

    actual = AK::StringUtils::convert_to_uint("1", ok);
    EXPECT(ok && actual == 1u);

    actual = AK::StringUtils::convert_to_uint("01", ok);
    EXPECT(ok && actual == 1u);

    actual = AK::StringUtils::convert_to_uint("12345", ok);
    EXPECT(ok && actual == 12345u);
}

TEST_MAIN(StringUtils)
