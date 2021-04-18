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

#include <AK/Hex.h>

TEST_CASE(should_decode_hex_digit)
{
    EXPECT_EQ(0u, decode_hex_digit('0'));
    EXPECT_EQ(1u, decode_hex_digit('1'));
    EXPECT_EQ(2u, decode_hex_digit('2'));
    EXPECT_EQ(3u, decode_hex_digit('3'));
    EXPECT_EQ(4u, decode_hex_digit('4'));
    EXPECT_EQ(5u, decode_hex_digit('5'));
    EXPECT_EQ(6u, decode_hex_digit('6'));
    EXPECT_EQ(7u, decode_hex_digit('7'));
    EXPECT_EQ(8u, decode_hex_digit('8'));
    EXPECT_EQ(9u, decode_hex_digit('9'));
    EXPECT_EQ(10u, decode_hex_digit('a'));
    EXPECT_EQ(11u, decode_hex_digit('b'));
    EXPECT_EQ(12u, decode_hex_digit('c'));
    EXPECT_EQ(13u, decode_hex_digit('d'));
    EXPECT_EQ(14u, decode_hex_digit('e'));
    EXPECT_EQ(15u, decode_hex_digit('f'));
    EXPECT_EQ(10u, decode_hex_digit('A'));
    EXPECT_EQ(11u, decode_hex_digit('B'));
    EXPECT_EQ(12u, decode_hex_digit('C'));
    EXPECT_EQ(13u, decode_hex_digit('D'));
    EXPECT_EQ(14u, decode_hex_digit('E'));
    EXPECT_EQ(15u, decode_hex_digit('F'));
}

TEST_CASE(should_constexpr_decode_hex_digit)
{
    static_assert(0u == decode_hex_digit('0'));
    static_assert(1u == decode_hex_digit('1'));
    static_assert(2u == decode_hex_digit('2'));
    static_assert(3u == decode_hex_digit('3'));
    static_assert(4u == decode_hex_digit('4'));
    static_assert(5u == decode_hex_digit('5'));
    static_assert(6u == decode_hex_digit('6'));
    static_assert(7u == decode_hex_digit('7'));
    static_assert(8u == decode_hex_digit('8'));
    static_assert(9u == decode_hex_digit('9'));
    static_assert(10u == decode_hex_digit('a'));
    static_assert(11u == decode_hex_digit('b'));
    static_assert(12u == decode_hex_digit('c'));
    static_assert(13u == decode_hex_digit('d'));
    static_assert(14u == decode_hex_digit('e'));
    static_assert(15u == decode_hex_digit('f'));
    static_assert(10u == decode_hex_digit('A'));
    static_assert(11u == decode_hex_digit('B'));
    static_assert(12u == decode_hex_digit('C'));
    static_assert(13u == decode_hex_digit('D'));
    static_assert(14u == decode_hex_digit('E'));
    static_assert(15u == decode_hex_digit('F'));
}

TEST_MAIN(Hex)
