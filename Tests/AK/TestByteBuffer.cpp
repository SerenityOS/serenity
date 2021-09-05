/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteBuffer.h>

TEST_CASE(equality_operator)
{
    ByteBuffer a = ByteBuffer::copy("Hello, world", 7).release_value();
    ByteBuffer b = ByteBuffer::copy("Hello, friend", 7).release_value();
    // `a` and `b` are both "Hello, ".
    ByteBuffer c = ByteBuffer::copy("asdf", 4).release_value();
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
    [[maybe_unused]] auto res = a < b;
    // error: error: use of deleted function ‘bool AK::ByteBuffer::operator<(const AK::ByteBuffer&) const’
}
#endif /* COMPILE_NEGATIVE_TESTS */
