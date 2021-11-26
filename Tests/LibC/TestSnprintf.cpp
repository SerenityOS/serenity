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

#pragma GCC diagnostic ignored "-Wformat-nonliteral"

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
    return builder.build();
}

static bool test_single(const Testcase& testcase)
{
    constexpr size_t SANDBOX_CANARY_SIZE = 8;

    // Preconditions:
    if (testcase.dest_n != testcase.dest_expected_n) {
        warnln("dest length {} != expected dest length {}? Check testcase! (Probably miscounted.)", testcase.dest_n, testcase.dest_expected_n);
        return false;
    }

    // Setup
    ByteBuffer actual = ByteBuffer::create_uninitialized(SANDBOX_CANARY_SIZE + testcase.dest_n + SANDBOX_CANARY_SIZE).release_value();
    fill_with_random(actual.data(), actual.size());
    ByteBuffer expected = actual;
    VERIFY(actual.offset_pointer(0) != expected.offset_pointer(0));
    actual.overwrite(SANDBOX_CANARY_SIZE, testcase.dest, testcase.dest_n);
    expected.overwrite(SANDBOX_CANARY_SIZE, testcase.dest_expected, testcase.dest_expected_n);
    // "unsigned char" != "char", so we have to convince the compiler to allow this.
    char* dst = reinterpret_cast<char*>(actual.offset_pointer(SANDBOX_CANARY_SIZE));

    // The actual call:
    int actual_return = snprintf(dst, testcase.dest_n, testcase.fmt, testcase.arg);

    // Checking the results:
    bool return_ok = actual_return == testcase.expected_return;
    bool canary_1_ok = actual.slice(0, SANDBOX_CANARY_SIZE) == expected.slice(0, SANDBOX_CANARY_SIZE);
    bool main_ok = actual.slice(SANDBOX_CANARY_SIZE, testcase.dest_n) == expected.slice(SANDBOX_CANARY_SIZE, testcase.dest_n);
    bool canary_2_ok = actual.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE) == expected.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE);
    bool buf_ok = actual == expected;

    // Evaluate gravity:
    if (buf_ok && (!canary_1_ok || !main_ok || !canary_2_ok)) {
        warnln("Internal error! ({} != {} | {} | {})", buf_ok, canary_1_ok, main_ok, canary_2_ok);
        buf_ok = false;
    }
    if (!canary_1_ok) {
        warnln("Canary 1 overwritten: Expected {}\n"
               "                   instead got {}",
            show(expected.slice(0, SANDBOX_CANARY_SIZE)),
            show(actual.slice(0, SANDBOX_CANARY_SIZE)));
    }
    if (!main_ok) {
        warnln("Wrong output: Expected {}\n"
               "          instead, got {}",
            show(expected.slice(SANDBOX_CANARY_SIZE, testcase.dest_n)),
            show(actual.slice(SANDBOX_CANARY_SIZE, testcase.dest_n)));
    }
    if (!canary_2_ok) {
        warnln("Canary 2 overwritten: Expected {}\n"
               "                  instead, got {}",
            show(expected.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE)),
            show(actual.slice(SANDBOX_CANARY_SIZE + testcase.dest_n, SANDBOX_CANARY_SIZE)));
    }
    if (!return_ok) {
        warnln("Wrong return value: Expected {}, got {} instead!", testcase.expected_return, actual_return);
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
    EXPECT_EQ(snprintf(nullptr, 0, "Hello, friend!"), 14);
    EXPECT(test_single({ LITERAL(""), "", POISON, 0, LITERAL("") }));
    EXPECT(test_single({ LITERAL("x"), "", POISON, 0, LITERAL("\0") }));
    EXPECT(test_single({ LITERAL("xx"), "", POISON, 0, LITERAL("\0x") }));
    EXPECT(test_single({ LITERAL("xxx"), "", POISON, 0, LITERAL("\0xx") }));
    EXPECT(test_single({ LITERAL(""), "whf", POISON, 3, LITERAL("") }));
    EXPECT(test_single({ LITERAL("x"), "whf", POISON, 3, LITERAL("\0") }));
    EXPECT(test_single({ LITERAL("xx"), "whf", POISON, 3, LITERAL("w\0") }));
}
