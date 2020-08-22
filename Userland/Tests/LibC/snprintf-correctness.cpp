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

struct Testcase {
    const char* dest;
    size_t dest_n;
    const char* fmt;
    const char* arg;
    int expected_return;
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
    int actual_return = snprintf(dst, testcase.dest_n, testcase.fmt, testcase.arg);

    // Checking the results:
    bool return_ok = actual_return == testcase.expected_return;
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
        fprintf(stderr, "Wrong return value: Expected %d, got %d instead!\n",
            testcase.expected_return, actual_return);
    }

    return buf_ok && return_ok;
}

// Drop the NUL terminator added by the C++ compiler.
#define LITERAL(x) x, (sizeof(x) - 1)

static const char* const POISON = (const char*)1;

TEST_CASE(golden_path)
{
    EXPECT(test_single({ LITERAL("Hello World!\0\0\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single({ LITERAL("Hello World!\0\0\0"), "Hello %s!", "Friend", 13, LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single({ LITERAL("aaaaaaaaaa"), "whf", POISON, 3, LITERAL("whf\0aaaaaa") }));
    EXPECT(test_single({ LITERAL("aaaaaaaaaa"), "w%sf", "h", 3, LITERAL("whf\0aaaaaa") }));
}

TEST_CASE(border_cases)
{
    EXPECT(test_single({ LITERAL("Hello World!\0\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend!\0") }));
    EXPECT(test_single({ LITERAL("AAAA"), "whf", POISON, 3, LITERAL("whf\0") }));
    EXPECT(test_single({ LITERAL("AAAA"), "%s", "whf", 3, LITERAL("whf\0") }));
}

TEST_CASE(too_long)
{
    EXPECT(test_single({ LITERAL("Hello World!\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend\0") }));
    EXPECT(test_single({ LITERAL("Hello World!\0"), "This source is %s too long!", "just *way*", 35, LITERAL("This source \0") }));
    EXPECT(test_single({ LITERAL("x"), "This source is %s too long!", "just *way*", 35, LITERAL("\0") }));
}

TEST_CASE(special_cases)
{
    EXPECT(test_single({ LITERAL(""), "Hello Friend!", POISON, 13, LITERAL("") }));
    EXPECT_EQ(snprintf(nullptr, 0, "Hello, friend!"), 0);
    EXPECT(test_single({ LITERAL(""), "", POISON, 0, LITERAL("") }));
    EXPECT(test_single({ LITERAL("x"), "", POISON, 0, LITERAL("\0") }));
    EXPECT(test_single({ LITERAL("xx"), "", POISON, 0, LITERAL("\0x") }));
    EXPECT(test_single({ LITERAL("xxx"), "", POISON, 0, LITERAL("\0xx") }));
    EXPECT(test_single({ LITERAL(""), "whf", POISON, 3, LITERAL("") }));
    EXPECT(test_single({ LITERAL("x"), "whf", POISON, 3, LITERAL("\0") }));
    EXPECT(test_single({ LITERAL("xx"), "whf", POISON, 3, LITERAL("w\0") }));
}

TEST_MAIN(Sprintf)
