/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>
#include <string.h>

struct Testcase {
    const char* dest;
    size_t dest_n;
    const char* src;
    size_t src_n;
    const char* dest_expected;
    size_t dest_expected_n; // == dest_n
};

static String show(const ByteBuffer& buf)
{
    StringBuilder builder;
    for (size_t i = 0; i < buf.size(); ++i) {
        builder.appendf("%02x", buf[i]);
    }
    builder.append(' ');
    builder.append('(');
    for (size_t i = 0; i < buf.size(); ++i) {
        if (isprint(buf[i]))
            builder.append(buf[i]);
        else
            builder.append('_');
    }
    builder.append(')');
    return builder.build();
}

static const size_t SANDBOX_CANARY_SIZE = 8;

static bool test_single(const Testcase& testcase)
{
    // Preconditions:
    if (testcase.dest_n != testcase.dest_expected_n) {
        fprintf(stderr, "dest length %zu != expected dest length %zu? Check testcase! (Probably miscounted.)\n",
            testcase.dest_n, testcase.dest_expected_n);
        return false;
    }
    if (testcase.src_n != strlen(testcase.src)) {
        fprintf(stderr, "src length %zu != actual src length %zu? src can't contain NUL bytes!\n",
            testcase.src_n, strlen(testcase.src));
        return false;
    }

    // Setup
    ByteBuffer actual = ByteBuffer::create_uninitialized(SANDBOX_CANARY_SIZE + testcase.dest_n + SANDBOX_CANARY_SIZE);
    AK::fill_with_random(actual.data(), actual.size());
    ByteBuffer expected = actual.isolated_copy();
    ASSERT(actual.offset_pointer(0) != expected.offset_pointer(0));
    actual.overwrite(SANDBOX_CANARY_SIZE, testcase.dest, testcase.dest_n);
    expected.overwrite(SANDBOX_CANARY_SIZE, testcase.dest_expected, testcase.dest_expected_n);
    // "unsigned char" != "char", so we have to convince the compiler to allow this.
    char* dst = reinterpret_cast<char*>(actual.offset_pointer(SANDBOX_CANARY_SIZE));

    // The actual call:
    size_t actual_return = strlcpy(dst, testcase.src, testcase.dest_n);

    // Checking the results:
    bool return_ok = actual_return == testcase.src_n;
    bool canary_1_ok = actual.slice_view(0, SANDBOX_CANARY_SIZE) == expected.slice_view(0, SANDBOX_CANARY_SIZE);
    bool main_ok = actual.slice_view(SANDBOX_CANARY_SIZE, testcase.dest_n) == expected.slice_view(SANDBOX_CANARY_SIZE, testcase.dest_n);
    bool canary_2_ok = actual.slice_view(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE) == expected.slice_view(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE);
    bool buf_ok = actual == expected;

    // Evaluate gravity:
    if (buf_ok && (!canary_1_ok || !main_ok || !canary_2_ok)) {
        fprintf(stderr, "Internal error! (%d != %d | %d | %d)\n",
            buf_ok, canary_1_ok, main_ok, canary_2_ok);
        buf_ok = false;
    }
    if (!canary_1_ok) {
        warn() << "Canary 1 overwritten: Expected canary "
               << show(expected.slice_view(0, SANDBOX_CANARY_SIZE))
               << ", got "
               << show(actual.slice_view(0, SANDBOX_CANARY_SIZE))
               << " instead!";
    }
    if (!main_ok) {
        warn() << "Wrong output: Expected "
               << show(expected.slice_view(SANDBOX_CANARY_SIZE, testcase.dest_n))
               << "\n          instead, got " // visually align
               << show(actual.slice_view(SANDBOX_CANARY_SIZE, testcase.dest_n));
    }
    if (!canary_2_ok) {
        warn() << "Canary 2 overwritten: Expected "
               << show(expected.slice_view(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE))
               << ", got "
               << show(actual.slice_view(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE))
               << " instead!";
    }
    if (!return_ok) {
        fprintf(stderr, "Wrong return value: Expected %zu, got %zu instead!\n",
            testcase.src_n, actual_return);
    }

    return buf_ok && return_ok;
}

// Drop the NUL terminator added by the C++ compiler.
#define LITERAL(x) x, (sizeof(x) - 1)

//static Testcase TESTCASES[] = {
//    // Golden path:

//    // Hitting the border:

//    // Too long:
//    { LITERAL("Hello World!\0"), LITERAL("Hello Friend!"), LITERAL("Hello Friend\0") },
//    { LITERAL("Hello World!\0"), LITERAL("This source is just *way* too long!"), LITERAL("This source \0") },
//    { LITERAL("x"), LITERAL("This source is just *way* too long!"), LITERAL("\0") },
//    // Other special cases:
//    { LITERAL(""), LITERAL(""), LITERAL("") },
//    { LITERAL(""), LITERAL("Empty test"), LITERAL("") },
//    { LITERAL("x"), LITERAL(""), LITERAL("\0") },
//    { LITERAL("xx"), LITERAL(""), LITERAL("\0x") },
//    { LITERAL("xxx"), LITERAL(""), LITERAL("\0xx") },
//};

TEST_CASE(golden_path)
{
    EXPECT(test_single({ LITERAL("Hello World!\0\0\0"), LITERAL("Hello Friend!"), LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single({ LITERAL("Hello World!\0\0\0"), LITERAL("Hello Friend!"), LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single({ LITERAL("aaaaaaaaaa"), LITERAL("whf"), LITERAL("whf\0aaaaaa") }));
}

TEST_CASE(exact_fit)
{
    EXPECT(test_single({ LITERAL("Hello World!\0\0"), LITERAL("Hello Friend!"), LITERAL("Hello Friend!\0") }));
    EXPECT(test_single({ LITERAL("AAAA"), LITERAL("aaa"), LITERAL("aaa\0") }));
}

TEST_CASE(off_by_one)
{
    EXPECT(test_single({ LITERAL("AAAAAAAAAA"), LITERAL("BBBBB"), LITERAL("BBBBB\0AAAA") }));
    EXPECT(test_single({ LITERAL("AAAAAAAAAA"), LITERAL("BBBBBBBCC"), LITERAL("BBBBBBBCC\0") }));
    EXPECT(test_single({ LITERAL("AAAAAAAAAA"), LITERAL("BBBBBBBCCX"), LITERAL("BBBBBBBCC\0") }));
    EXPECT(test_single({ LITERAL("AAAAAAAAAA"), LITERAL("BBBBBBBCCXY"), LITERAL("BBBBBBBCC\0") }));
}

TEST_CASE(nearly_empty)
{
    EXPECT(test_single({ LITERAL(""), LITERAL(""), LITERAL("") }));
    EXPECT(test_single({ LITERAL(""), LITERAL("Empty test"), LITERAL("") }));
    EXPECT(test_single({ LITERAL("x"), LITERAL(""), LITERAL("\0") }));
    EXPECT(test_single({ LITERAL("xx"), LITERAL(""), LITERAL("\0x") }));
    EXPECT(test_single({ LITERAL("x"), LITERAL("y"), LITERAL("\0") }));
}

static char* const POISON = (char*)1;
TEST_CASE(to_nullptr)
{
    EXPECT_EQ(0u, strlcpy(POISON, "", 0));
    EXPECT_EQ(1u, strlcpy(POISON, "x", 0));
    EXPECT(test_single({ LITERAL("Hello World!\0\0\0"), LITERAL("Hello Friend!"), LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single({ LITERAL("aaaaaaaaaa"), LITERAL("whf"), LITERAL("whf\0aaaaaa") }));
}

TEST_MAIN(Sprintf)
