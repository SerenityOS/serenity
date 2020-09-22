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

#include <AK/Format.h>

TEST_CASE(format_string_literals)
{
    EXPECT_EQ(AK::format("prefix-{}-suffix", "abc"), "prefix-abc-suffix");
    EXPECT_EQ(AK::format("{}{}{}", "a", "b", "c"), "abc");
}

TEST_CASE(format_integers)
{
    EXPECT_EQ(AK::format("{}", 42u), "42");
    EXPECT_EQ(AK::format("{:4}", 42u), "  42");
    EXPECT_EQ(AK::format("{:08}", 42u), "00000042");
    // EXPECT_EQ(AK::format("{:7}", -17), "    -17");
    EXPECT_EQ(AK::format("{}", -17), "-17");
    EXPECT_EQ(AK::format("{:04}", 13), "0013");
    EXPECT_EQ(AK::format("{:08x}", 4096), "00001000");
    EXPECT_EQ(AK::format("{:x}", 0x1111222233334444ull), "1111222233334444");
}

TEST_CASE(reorder_format_arguments)
{
    EXPECT_EQ(AK::format("{1}{0}", "a", "b"), "ba");
    EXPECT_EQ(AK::format("{0}{1}", "a", "b"), "ab");
    EXPECT_EQ(AK::format("{0}{0}{0}", "a", "b"), "aaa");
    EXPECT_EQ(AK::format("{1}{}{0}", "a", "b", "c"), "baa");
}

TEST_CASE(escape_braces)
{
    EXPECT_EQ(AK::format("{{{}", "foo"), "{foo");
    EXPECT_EQ(AK::format("{}}}", "bar"), "bar}");
}

TEST_CASE(everything)
{
    EXPECT_EQ(AK::format("{{{:04}/{}/{0:8}/{1}", 42u, "foo"), "{0042/foo/      42/foo");
}

TEST_MAIN(Format)
