/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteBuffer.h>
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>
#include <string.h>

struct Testcase {
    char const* dest;
    size_t dest_n;
    char const* src;
    size_t src_n;
    char const* dest_expected;
    size_t dest_expected_n; // == dest_n
};

static ByteString show(ByteBuffer const& buf)
{
    StringBuilder builder;
    for (size_t i = 0; i < buf.size(); ++i) {
        builder.appendff("{:02x}", buf[i]);
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
    return builder.to_byte_string();
}

static bool test_single(Testcase const& testcase)
{
    constexpr size_t SANDBOX_CANARY_SIZE = 8;

    // Preconditions:
    if (testcase.dest_n != testcase.dest_expected_n) {
        warnln("dest length {} != expected dest length {}? Check testcase! (Probably miscounted.)", testcase.dest_n, testcase.dest_expected_n);
        return false;
    }
    if (testcase.src_n != strlen(testcase.src)) {
        warnln("src length {} != actual src length {}? src can't contain NUL bytes!", testcase.src_n, strlen(testcase.src));
        return false;
    }

    // Setup
    ByteBuffer actual = ByteBuffer::create_uninitialized(SANDBOX_CANARY_SIZE + testcase.dest_n + SANDBOX_CANARY_SIZE).release_value();
    fill_with_random(actual);
    ByteBuffer expected = actual;
    VERIFY(actual.offset_pointer(0) != expected.offset_pointer(0));
    actual.overwrite(SANDBOX_CANARY_SIZE, testcase.dest, testcase.dest_n);
    expected.overwrite(SANDBOX_CANARY_SIZE, testcase.dest_expected, testcase.dest_expected_n);
    // "unsigned char" != "char", so we have to convince the compiler to allow this.
    char* dst = reinterpret_cast<char*>(actual.offset_pointer(SANDBOX_CANARY_SIZE));

    // The actual call:
    size_t actual_return = strlcpy(dst, testcase.src, testcase.dest_n);

    // Checking the results:
    bool return_ok = actual_return == testcase.src_n;
    bool canary_1_ok = MUST(actual.slice(0, SANDBOX_CANARY_SIZE)) == MUST(expected.slice(0, SANDBOX_CANARY_SIZE));
    bool main_ok = MUST(actual.slice(SANDBOX_CANARY_SIZE, testcase.dest_n)) == MUST(expected.slice(SANDBOX_CANARY_SIZE, testcase.dest_n));
    bool canary_2_ok = MUST(actual.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE)) == MUST(expected.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE));
    bool buf_ok = actual == expected;

    // Evaluate gravity:
    if (buf_ok && (!canary_1_ok || !main_ok || !canary_2_ok)) {
        warnln("Internal error! ({} != {} | {} | {})", buf_ok, canary_1_ok, main_ok, canary_2_ok);
        buf_ok = false;
    }
    if (!canary_1_ok) {
        warnln("Canary 1 overwritten: Expected canary {}\n"
               "                          instead got {}",
            show(MUST(expected.slice(0, SANDBOX_CANARY_SIZE))),
            show(MUST(actual.slice(0, SANDBOX_CANARY_SIZE))));
    }
    if (!main_ok) {
        warnln("Wrong output: Expected {}\n"
               "           instead got {}",
            show(MUST(expected.slice(SANDBOX_CANARY_SIZE, testcase.dest_n))),
            show(MUST(actual.slice(SANDBOX_CANARY_SIZE, testcase.dest_n))));
    }
    if (!canary_2_ok) {
        warnln("Canary 2 overwritten: Expected {}\n"
               "                   instead got {}",
            show(MUST(expected.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE))),
            show(MUST(actual.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE))));
    }
    if (!return_ok) {
        warnln("Wrong return value: Expected {}, got {} instead!", testcase.src_n, actual_return);
    }

    return buf_ok && return_ok;
}

// Drop the NUL terminator added by the C++ compiler.
#define LITERAL(x) x, (sizeof(x) - 1)

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
