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

#include <AK/ByteBuffer.h>

TEST_CASE(equality_operator)
{
    ByteBuffer a = ByteBuffer::copy("Hello, world", 7);
    ByteBuffer b = ByteBuffer::copy("Hello, friend", 7);
    // `a` and `b` are both "Hello, ".
    ByteBuffer c = ByteBuffer::copy("asdf", 4);
    ByteBuffer d;
    EXPECT_EQ(a == a, true);
    EXPECT_EQ(a == b, true);
    EXPECT_EQ(a == c, false);
    EXPECT_EQ(a == d, false);
    EXPECT_EQ(b == a, true);
    EXPECT_EQ(b == b, true);
    EXPECT_EQ(b == c, false);
    EXPECT_EQ(b == d, false);
    EXPECT_EQ(c == a, false);
    EXPECT_EQ(c == b, false);
    EXPECT_EQ(c == c, true);
    EXPECT_EQ(c == d, false);
    EXPECT_EQ(d == a, false);
    EXPECT_EQ(d == b, false);
    EXPECT_EQ(d == c, false);
    EXPECT_EQ(d == d, true);
}

/*
 * FIXME: These `negative_*` tests should cause precisely one compilation error
 * each, and always for the specified reason. Currently we do not have a harness
 * for that, so in order to run the test you need to set the #define to 1, compile
 * it, and check the error messages manually.
 */
#define COMPILE_NEGATIVE_TESTS 0
#if COMPILE_NEGATIVE_TESTS
TEST_CASE(negative_operator_lt)
{
    ByteBuffer a = ByteBuffer::copy("Hello, world", 10);
    ByteBuffer b = ByteBuffer::copy("Hello, friend", 10);
    (void)(a < b);
    // error: error: use of deleted function ‘bool AK::ByteBuffer::operator<(const AK::ByteBuffer&) const’
}
#endif /* COMPILE_NEGATIVE_TESTS */

TEST_MAIN(ByteBuffer)
