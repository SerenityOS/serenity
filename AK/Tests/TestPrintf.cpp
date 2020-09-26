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

#include <AK/PrintfImplementation.h>
#include <AK/StringBuilder.h>

TEST_CASE(format_unsigned_with_internal_implementation)
{
    Array<u8, 128> buffer;
    size_t used = 0;

    used = PrintfImplementation::convert_unsigned_to_string(12341234, buffer, 10, false);
    EXPECT_EQ(StringView { buffer.span().trim(used) }, "12341234");

    used = PrintfImplementation::convert_unsigned_to_string(12341234, buffer, 16, false);
    EXPECT_EQ(StringView { buffer.span().trim(used) }, "bc4ff2");

    used = PrintfImplementation::convert_unsigned_to_string(12341234, buffer, 16, true);
    EXPECT_EQ(StringView { buffer.span().trim(used) }, "BC4FF2");

    used = PrintfImplementation::convert_unsigned_to_string(0, buffer, 10, true);
    EXPECT_EQ(StringView { buffer.span().trim(used) }, "0");

    used = PrintfImplementation::convert_unsigned_to_string(NumericLimits<u64>::max(), buffer, 10, true);
    EXPECT_EQ(StringView { buffer.span().trim(used) }, "18446744073709551615");
}

TEST_CASE(format_unsigned_just_pass_through)
{
    StringBuilder builder;
    size_t used = 0;

    builder.clear();
    used = PrintfImplementation::convert_unsigned_to_string(12341234, builder);
    EXPECT_EQ(used, 8u);
    EXPECT_EQ(builder.to_string(), "12341234");

    builder.clear();
    used = PrintfImplementation::convert_unsigned_to_string(12341234, builder, 16);
    EXPECT_EQ(used, 6u);
    EXPECT_EQ(builder.to_string(), "bc4ff2");

    builder.clear();
    used = PrintfImplementation::convert_unsigned_to_string(12341234, builder, 16, false, true);
    EXPECT_EQ(used, 6u);
    EXPECT_EQ(builder.to_string(), "BC4FF2");
}

TEST_CASE(format_unsigned)
{
    StringBuilder builder;

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Right, 4, '*', PrintfImplementation::SignMode::OnlyIfNeeded);
    EXPECT_EQ(builder.to_string(), "0042");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Left, 4, '*', PrintfImplementation::SignMode::OnlyIfNeeded);
    EXPECT_EQ(builder.to_string(), "42**");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Center, 4, '*', PrintfImplementation::SignMode::OnlyIfNeeded);
    EXPECT_EQ(builder.to_string(), "*42*");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Center, 9, '*', PrintfImplementation::SignMode::OnlyIfNeeded);
    EXPECT_EQ(builder.to_string(), "***42****");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Center, 9, '*', PrintfImplementation::SignMode::Reserved);
    EXPECT_EQ(builder.to_string(), "*** 42***");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Left, 4, '*', PrintfImplementation::SignMode::Always, true);
    EXPECT_EQ(builder.to_string(), "-42*");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Center, 4, '*', PrintfImplementation::SignMode::Reserved, true);
    EXPECT_EQ(builder.to_string(), "-42*");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(42, builder, 10, false, false, true, PrintfImplementation::Align::Right, 4, '*', PrintfImplementation::SignMode::OnlyIfNeeded, true);
    EXPECT_EQ(builder.to_string(), "-042");

    builder.clear();
    PrintfImplementation::convert_unsigned_to_string(32, builder, 16, true, false, true, PrintfImplementation::Align::Right, 8, '*', PrintfImplementation::SignMode::OnlyIfNeeded, true);
    EXPECT_EQ(builder.to_string(), "-0x00020");
}

TEST_CASE(format_signed)
{
    StringBuilder builder;

    builder.clear();
    PrintfImplementation::convert_signed_to_string(42, builder, 10, false, false, false, PrintfImplementation::Align::Right, 8, '/', PrintfImplementation::SignMode::OnlyIfNeeded);
    EXPECT_EQ(builder.to_string(), "//////42");
}

TEST_MAIN(Printf)
