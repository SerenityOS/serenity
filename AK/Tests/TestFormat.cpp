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

#include <AK/String.h>
#include <AK/StringBuilder.h>

TEST_CASE(format_string_literals)
{
    EXPECT_EQ(String::formatted("prefix-{}-suffix", "abc"), "prefix-abc-suffix");
    EXPECT_EQ(String::formatted("{}{}{}", "a", "b", "c"), "abc");
}

TEST_CASE(format_integers)
{
    EXPECT_EQ(String::formatted("{}", 42u), "42");
    EXPECT_EQ(String::formatted("{:4}", 42u), "  42");
    EXPECT_EQ(String::formatted("{:08}", 42u), "00000042");
    EXPECT_EQ(String::formatted("{:7}", -17), "    -17");
    EXPECT_EQ(String::formatted("{}", -17), "-17");
    EXPECT_EQ(String::formatted("{:04}", 13), "0013");
    EXPECT_EQ(String::formatted("{:08x}", 4096), "00001000");
    EXPECT_EQ(String::formatted("{:x}", 0x1111222233334444ull), "1111222233334444");
    EXPECT_EQ(String::formatted("{:4}", 12345678), "12345678");
}

TEST_CASE(reorder_format_arguments)
{
    EXPECT_EQ(String::formatted("{1}{0}", "a", "b"), "ba");
    EXPECT_EQ(String::formatted("{0}{1}", "a", "b"), "ab");
    EXPECT_EQ(String::formatted("{0}{0}{0}", "a", "b"), "aaa");
    EXPECT_EQ(String::formatted("{1}{}{0}", "a", "b", "c"), "baa");
}

TEST_CASE(escape_braces)
{
    EXPECT_EQ(String::formatted("{{{}", "foo"), "{foo");
    EXPECT_EQ(String::formatted("{}}}", "bar"), "bar}");
}

TEST_CASE(everything)
{
    EXPECT_EQ(String::formatted("{{{:04}/{}/{0:8}/{1}", 42u, "foo"), "{0042/foo/      42/foo");
}

TEST_CASE(string_builder)
{
    StringBuilder builder;
    builder.appendff(" {}  ", 42);
    builder.appendff("{1}{0} ", 1, 2);

    EXPECT_EQ(builder.to_string(), " 42  21 ");
}

TEST_CASE(format_without_arguments)
{
    EXPECT_EQ(String::formatted("foo"), "foo");
}

TEST_CASE(format_upper_case_integer)
{
    EXPECT_EQ(String::formatted("{:4X}", 0xff), "  FF");
    EXPECT_EQ(String::formatted("{:#4X}", 0xff), "0XFF");

    EXPECT_EQ(String::formatted("{:b}", 0xff), "11111111");
    EXPECT_EQ(String::formatted("{:B}", 0xff), "11111111");
    EXPECT_EQ(String::formatted("{:#b}", 0xff), "0b11111111");
}

TEST_CASE(format_aligned)
{
    EXPECT_EQ(String::formatted("{:*<8}", 13), "13******");
    EXPECT_EQ(String::formatted("{:*^8}", 13), "***13***");
    EXPECT_EQ(String::formatted("{:*>8}", 13), "******13");
    EXPECT_EQ(String::formatted("{:*>+8}", 13), "*****+13");
    EXPECT_EQ(String::formatted("{:*^ 8}", 13), "** 13***");
}

TEST_CASE(format_octal)
{
    EXPECT_EQ(String::formatted("{:o}", 0744), "744");
    EXPECT_EQ(String::formatted("{:#o}", 0744), "0744");
}

TEST_CASE(zero_pad)
{
    EXPECT_EQ(String::formatted("{: <010}", 42), "42        ");
    EXPECT_EQ(String::formatted("{:010}", 42), "0000000042");
    EXPECT_EQ(String::formatted("{:/^010}", 42), "////42////");
    EXPECT_EQ(String::formatted("{:04x}", -32), "-0020");
    EXPECT_EQ(String::formatted("{:#06x}", -64), "-0x000040");
}

TEST_CASE(replacement_field)
{
    EXPECT_EQ(String::formatted("{:*>{1}}", 13, static_cast<size_t>(10)), "********13");
    EXPECT_EQ(String::formatted("{:*<{1}}", 7, 4), "7***");
    EXPECT_EQ(String::formatted("{:{2}}", -5, 8, 16), "              -5");
    EXPECT_EQ(String::formatted("{{{:*^{1}}}}", 1, 3), "{*1*}");
    EXPECT_EQ(String::formatted("{:0{}}", 1, 3), "001");
}

TEST_CASE(complex_string_specifiers)
{
    EXPECT_EQ(String::formatted("{:.8}", "123456789"), "12345678");
    EXPECT_EQ(String::formatted("{:9}", "abcd"), "abcd     ");
    EXPECT_EQ(String::formatted("{:>9}", "abcd"), "     abcd");
    EXPECT_EQ(String::formatted("{:^9}", "abcd"), "  abcd   ");
}

TEST_CASE(cast_integer_to_character)
{
    EXPECT_EQ(String::formatted("{:c}", static_cast<int>('a')), "a");
    EXPECT_EQ(String::formatted("{:c}", static_cast<unsigned int>('f')), "f");
}

TEST_CASE(boolean_values)
{
    EXPECT_EQ(String::formatted("{}", true), "true");
    EXPECT_EQ(String::formatted("{}", false), "false");
    EXPECT_EQ(String::formatted("{:6}", true), "true  ");
    EXPECT_EQ(String::formatted("{:>4}", false), "false");
    EXPECT_EQ(String::formatted("{:d}", false), "0");
    EXPECT_EQ(String::formatted("{:d}", true), "1");
    EXPECT_EQ(String::formatted("{:#08x}", true), "0x00000001");
}

TEST_CASE(pointers)
{
    void* ptr = reinterpret_cast<void*>(0x4000);

    if (sizeof(void*) == 4) {
        EXPECT_EQ(String::formatted("{:p}", 32), "0x00000020");
        EXPECT_EQ(String::formatted("{:p}", ptr), "0x00004000");
        EXPECT_EQ(String::formatted("{}", ptr), "0x00004000");
    } else if (sizeof(void*) == 8) {
        EXPECT_EQ(String::formatted("{:p}", 32), "0x0000000000000020");
        EXPECT_EQ(String::formatted("{:p}", ptr), "0x0000000000004000");
        EXPECT_EQ(String::formatted("{}", ptr), "0x0000000000004000");
    } else {
        ASSERT_NOT_REACHED();
    }
}

// If the format implementation did absolutely nothing, all tests would pass. This
// is because when a test fails we only write "FAIL" to stdout using format.
//
// This is a bit scary, thus this test. At least this test should fail in this case.
TEST_CASE(ensure_that_format_works)
{

    if (String::formatted("FAIL") != "FAIL") {
        fprintf(stderr, "FAIL\n");
        exit(1);
    }

    if (String::formatted("{} FAIL {}", 1, 2) != "1 FAIL 2") {
        fprintf(stderr, "FAIL\n");
        exit(1);
    }
}

TEST_CASE(format_string_literal_as_pointer)
{
    const char* literal = "abc";
    EXPECT_EQ(String::formatted("{:p}", literal), String::formatted("{:p}", reinterpret_cast<FlatPtr>(literal)));
}

TEST_CASE(format_character)
{
    char a = 'a';
    EXPECT_EQ(String::formatted("{}", true ? a : 'b'), "a");
}

struct A {
};
struct B {
};
template<>
struct AK::Formatter<B> : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, B)
    {
        Formatter<StringView>::format(params, builder, "B");
    }
};

TEST_CASE(format_if_supported)
{
    EXPECT_EQ(String::formatted("{}", FormatIfSupported { A {} }), "?");
    EXPECT_EQ(String::formatted("{}", FormatIfSupported { B {} }), "B");
}

TEST_MAIN(Format)
