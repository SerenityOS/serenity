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
#include <inttypes.h>
#include <stdint.h>
#include <stdio.h>

#pragma GCC diagnostic ignored "-Wformat-nonliteral"

template<typename TArg>
struct Testcase {
    const char* dest;
    size_t dest_n;
    const char* fmt;
    const TArg arg;
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

template<typename TArg>
static bool test_single(const Testcase<TArg>& testcase)
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
    EXPECT(test_single<const char*>({ LITERAL("Hello World!\0\0\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single<const char*>({ LITERAL("Hello World!\0\0\0"), "Hello %s!", "Friend", 13, LITERAL("Hello Friend!\0\0") }));
    EXPECT(test_single<const char*>({ LITERAL("aaaaaaaaaa"), "whf", POISON, 3, LITERAL("whf\0aaaaaa") }));
    EXPECT(test_single<const char*>({ LITERAL("aaaaaaaaaa"), "w%sf", "h", 3, LITERAL("whf\0aaaaaa") }));
}

TEST_CASE(border_cases)
{
    EXPECT(test_single<const char*>({ LITERAL("Hello World!\0\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend!\0") }));
    EXPECT(test_single<const char*>({ LITERAL("AAAA"), "whf", POISON, 3, LITERAL("whf\0") }));
    EXPECT(test_single<const char*>({ LITERAL("AAAA"), "%s", "whf", 3, LITERAL("whf\0") }));
}

TEST_CASE(too_long)
{
    EXPECT(test_single<const char*>({ LITERAL("Hello World!\0"), "Hello Friend!", POISON, 13, LITERAL("Hello Friend\0") }));
    EXPECT(test_single<const char*>({ LITERAL("Hello World!\0"), "This source is %s too long!", "just *way*", 35, LITERAL("This source \0") }));
    EXPECT(test_single<const char*>({ LITERAL("x"), "This source is %s too long!", "just *way*", 35, LITERAL("\0") }));
}

TEST_CASE(special_cases)
{
    EXPECT(test_single<const char*>({ LITERAL(""), "Hello Friend!", POISON, 13, LITERAL("") }));
    EXPECT_EQ(snprintf(nullptr, 0, "Hello, friend!"), 14);
    EXPECT(test_single<const char*>({ LITERAL(""), "", POISON, 0, LITERAL("") }));
    EXPECT(test_single<const char*>({ LITERAL("x"), "", POISON, 0, LITERAL("\0") }));
    EXPECT(test_single<const char*>({ LITERAL("xx"), "", POISON, 0, LITERAL("\0x") }));
    EXPECT(test_single<const char*>({ LITERAL("xxx"), "", POISON, 0, LITERAL("\0xx") }));
    EXPECT(test_single<const char*>({ LITERAL(""), "whf", POISON, 3, LITERAL("") }));
    EXPECT(test_single<const char*>({ LITERAL("x"), "whf", POISON, 3, LITERAL("\0") }));
    EXPECT(test_single<const char*>({ LITERAL("xx"), "whf", POISON, 3, LITERAL("w\0") }));
}

TEST_CASE(octal_values)
{
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5o|", 017, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5o|", 01000, 12, LITERAL("|     01000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5o|", 010000, 12, LITERAL("|    010000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5o|", 017, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-10.5o|", 017, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-010.5o|", 017, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010.5o|", 017, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010o|", 017, 12, LITERAL("|0000000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10o|", 017, 12, LITERAL("|        17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxx\0"), "|%.5o|", 017, 7, LITERAL("|00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.1o|", 017, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.0o|", 017, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xx\0"), "|%.0o|", 00, 2, LITERAL("||\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%#.0o|", 00, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%#.0o|", 01, 4, LITERAL("|01|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%#.1o|", 00, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%#.1o|", 01, 4, LITERAL("|01|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%o|", 00, 3, LITERAL("|0|\0") }));
}

TEST_CASE(decimal_values)
{
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5d|", 17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10.5d|", 17, 12, LITERAL("|    +00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5d|", -17, 12, LITERAL("|    -00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10.5d|", -17, 12, LITERAL("|    -00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-10.5d|", 17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+-10.5d|", 17, 12, LITERAL("|+00017    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+-10.5d|", -17, 12, LITERAL("|-00017    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-10.5d|", -17, 12, LITERAL("|-00017    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-010.5d|", 17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010.5d|", 17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010d|", 17, 12, LITERAL("|0000000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+010d|", 17, 12, LITERAL("|+000000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010d|", -17, 12, LITERAL("|-000000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010d|", 170000000, 12, LITERAL("|0170000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+010d|", 170000000, 12, LITERAL("|+170000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10d|", -170000000, 12, LITERAL("|-170000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10d|", -170000000, 12, LITERAL("|-170000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010d|", 1700000000, 12, LITERAL("|1700000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxxx\0"), "|%+010d|", 1700000000, 13, LITERAL("|+1700000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxxx\0"), "|%10d|", -1700000000, 13, LITERAL("|-1700000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxxx\0"), "|%+10d|", -1700000000, 13, LITERAL("|-1700000000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10d|", 17, 12, LITERAL("|        17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10d|", 17, 12, LITERAL("|       +17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10d|", -17, 12, LITERAL("|       -17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxx\0"), "|%.5d|", 17, 7, LITERAL("|00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.1d|", 17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.0d|", 17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xx\0"), "|%.0d|", 0, 2, LITERAL("||\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%+.0d|", 0, 3, LITERAL("|+|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%#.1d|", 0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%d|", 0, 3, LITERAL("|0|\0") }));
}

TEST_CASE(unsigned_decimal_values)
{
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5u|", 17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10.5u|", 17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-10.5u|", 17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+-10.5u|", 17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-010.5u|", 17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010.5u|", 17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010u|", 17, 12, LITERAL("|0000000017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+010u|", 17, 12, LITERAL("|0000000017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010u|", 170000000, 12, LITERAL("|0170000000|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+010u|", 170000000, 12, LITERAL("|0170000000|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010u|", 1700000000, 12, LITERAL("|1700000000|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+010u|", 1700000000, 12, LITERAL("|1700000000|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10u|", 17, 12, LITERAL("|        17|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxxxxxxx\0"), "|%+10u|", 17, 12, LITERAL("|        17|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxxxxx\0"), "|%.5u|", 17, 7, LITERAL("|00017|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxx\0"), "|%.1u|", 17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxxx\0"), "|%.0u|", 17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xx\0"), "|%.0u|", 0, 2, LITERAL("||\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xx\0"), "|%+.0u|", 0, 2, LITERAL("||\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxx\0"), "|%#.1u|", 0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<unsigned int>({ LITERAL("xxx\0"), "|%u|", 0, 3, LITERAL("|0|\0") }));
}

TEST_CASE(hexadecimal_values)
{
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5X|", 0xab, 12, LITERAL("|     000AB|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5X|", 0xab, 12, LITERAL("|   0x000AB|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5x|", 0xab, 12, LITERAL("|     000ab|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5x|", 0xab, 12, LITERAL("|   0x000ab|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5x|", 0x1000, 12, LITERAL("|     01000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5x|", 0x1000, 12, LITERAL("|   0x01000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5x|", 0x10000, 12, LITERAL("|     10000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10.5x|", 0x10000, 12, LITERAL("|   0x10000|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10.5x|", 0x17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-#10.5x|", 0x17, 12, LITERAL("|0x00017   |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-10.5x|", 0x17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%-010.5x|", 0x17, 12, LITERAL("|00017     |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010.5x|", 0x17, 12, LITERAL("|     00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%010x|", 0x17, 12, LITERAL("|0000000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#010x|", 0x17, 12, LITERAL("|0x00000017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%10x|", 0x17, 12, LITERAL("|        17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxxxxx\0"), "|%#10x|", 0x17, 12, LITERAL("|      0x17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxx\0"), "|%.5x|", 0x17, 7, LITERAL("|00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxxxxx\0"), "|%#.5x|", 0x17, 9, LITERAL("|0x00017|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.1x|", 0x17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxx\0"), "|%.0x|", 0x17, 4, LITERAL("|17|\0") }));
    EXPECT(test_single<int>({ LITERAL("xx\0"), "|%.0x|", 0x0, 2, LITERAL("||\0") }));
    EXPECT(test_single<int>({ LITERAL("xx\0"), "|%#.0x|", 0x0, 2, LITERAL("||\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxx\0"), "|%4.0x|", 0x0, 6, LITERAL("|    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxx\0"), "|%04.0x|", 0x0, 6, LITERAL("|    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxx\0"), "|%#4.0x|", 0x0, 6, LITERAL("|    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxxx\0"), "|%#04.0x|", 0x0, 6, LITERAL("|    |\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxx\0"), "|%#.0x|", 0x1, 5, LITERAL("|0x1|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%#.1x|", 0x0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%.1x|", 0x0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%x|", 0x0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxxxx\0"), "|%#.1x|", 0x1, 5, LITERAL("|0x1|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%x|", 0, 3, LITERAL("|0|\0") }));
    EXPECT(test_single<int>({ LITERAL("xxx\0"), "|%#x|", 0, 3, LITERAL("|0|\0") }));
}

TEST_CASE(inttypes_macros)
{
    EXPECT(test_single<uint8_t>({ LITERAL("xxxxx"), "|%" PRIx8 "|", 0xAB, 4, LITERAL("|ab|\0") }));
    EXPECT(test_single<uint8_t>({ LITERAL("xxxxx"), "|%" PRIX8 "|", 0xAB, 4, LITERAL("|AB|\0") }));
    EXPECT(test_single<uint16_t>({ LITERAL("xxxxxxx"), "|%" PRIx16 "|", 0xC0DE, 6, LITERAL("|c0de|\0") }));
    EXPECT(test_single<uint16_t>({ LITERAL("xxxxxxx"), "|%" PRIX16 "|", 0xC0DE, 6, LITERAL("|C0DE|\0") }));
}

TEST_CASE(float_values)
{
    union {
        float f;
        int i;
    } v;

    v.i = 0x7fc00000;
    EXPECT(test_single<double>({ LITERAL("xxxxxxx"), "|%4f|", v.f, 6, LITERAL("| nan|\0") }));
    EXPECT(test_single<double>({ LITERAL("xxxxxxx"), "|%4f|", -v.f, 6, LITERAL("|-nan|\0") }));

    v.i = 0x7f800000;
    EXPECT(test_single<double>({ LITERAL("xxxxxxx"), "|%4f|", v.f, 6, LITERAL("| inf|\0") }));
    EXPECT(test_single<double>({ LITERAL("xxxxxxx"), "|%4f|", -v.f, 6, LITERAL("|-inf|\0") }));
}
