/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <stdio.h>
#include <string.h>

typedef long double longdouble;
typedef long long longlong;
typedef unsigned long long unsignedlonglong;
typedef unsigned long unsignedlong;
typedef char charstar[32];

template<typename T>
constexpr static Array<unsigned char, 32> to_value_t(T x)
{
    // The endianness doesn't really matter, since we're going to convert both sides with this anyway.
    union Value {
        u8 v[32];
        T t;
    };

    auto value = Value { .t = x };

    return {
        value.v[0],
        value.v[1],
        value.v[2],
        value.v[3],
        value.v[4],
        value.v[5],
        value.v[6],
        value.v[7],
        value.v[8],
        value.v[9],
        value.v[10],
        value.v[11],
        value.v[12],
        value.v[13],
        value.v[14],
        value.v[15],
        value.v[16],
        value.v[17],
        value.v[18],
        value.v[19],
        value.v[20],
        value.v[21],
        value.v[22],
        value.v[23],
        value.v[24],
        value.v[25],
        value.v[26],
        value.v[27],
        value.v[28],
        value.v[29],
        value.v[30],
        value.v[31],
    };
}

template<size_t N>
constexpr static Array<unsigned char, 32> str_to_value_t(char const (&x)[N])
{
    Array<unsigned char, 32> value { 0 };
    for (size_t i = 0; i < N; ++i)
        value[i] = x[i];
    return value;
}

struct Argument {
    size_t size;
    void* data;
};

static Array<u8, 32> arg_to_value_t(Argument const& arg)
{
    if (arg.size == 1)
        return to_value_t(*(u8*)arg.data);

    if (arg.size == 2)
        return to_value_t(*(u16*)arg.data);

    if (arg.size == 4)
        return to_value_t(*(u32*)arg.data);

    if (arg.size == 8)
        return to_value_t(*(u64*)arg.data);

    if (arg.size == 16) {
        auto& data = *(charstar*)arg.data;
        Array<unsigned char, 32> value { 0 };
        for (size_t i = 0; i < 16; ++i)
            value[i] = data[i];
        return value;
    }

    if (arg.size == 32) {
        auto& data = *(charstar*)arg.data;
        auto length = strlen(data);
        Array<unsigned char, 32> value { 0 };
        for (size_t i = 0; i < length; ++i)
            value[i] = data[i];
        return value;
    }

    VERIFY_NOT_REACHED();
}

#define DECL_WITH_TYPE(ty)                          \
    ty _##ty##arg0;                                 \
    ty _##ty##arg1;                                 \
    ty _##ty##arg2;                                 \
    Argument ty##arg0 { sizeof(ty), &_##ty##arg0 }; \
    Argument ty##arg1 { sizeof(ty), &_##ty##arg1 }; \
    Argument ty##arg2 { sizeof(ty), &_##ty##arg2 };

DECL_WITH_TYPE(int);
DECL_WITH_TYPE(unsigned);
DECL_WITH_TYPE(long);
DECL_WITH_TYPE(longlong);
DECL_WITH_TYPE(float);
DECL_WITH_TYPE(double);
DECL_WITH_TYPE(longdouble);
DECL_WITH_TYPE(unsignedlong);
DECL_WITH_TYPE(unsignedlonglong);

#undef DECL_WITH_TYPE

charstar _charstararg0;
charstar _charstararg1;
charstar _charstararg2;
Argument charstararg0 { sizeof(charstar), &_charstararg0[0] };
Argument charstararg1 { sizeof(charstar), &_charstararg1[0] };
Argument charstararg2 { sizeof(charstar), &_charstararg2[0] };

struct TestSuite {
    char const* format;
    char const* input;
    int expected_return_value;
    size_t argument_count;
    Argument arguments[8];
    Array<unsigned char, 32> expected_values[8]; // 32 bytes for each argument's value.
};

TestSuite const test_suites[] {
    { "%d", "", 0, 0, {}, {} },
    { "%x", "0x519", 1, 1, { unsignedarg0 }, { to_value_t(0x519) } },
    { "%x", "0x51g", 1, 1, { unsignedarg0 }, { to_value_t(0x51u) } },
    { "%06x", "0xabcdef", 1, 1, { unsignedarg0 }, { to_value_t(0xabcdefu) } },
    { "%X", "0xCAFEBABE", 1, 1, { unsignedarg0 }, { to_value_t(0xcafebabe) } },
    { "%04X", "0x5E4E", 1, 1, { unsignedarg0 }, { to_value_t(0x5e4e) } },
    { "%X", "0x51Eg", 1, 1, { unsignedarg0 }, { to_value_t(0x51e) } },
    { "\"%%%d#", "\"%42#", 1, 1, { intarg0 }, { to_value_t(42) } },
    { "  %d", "42", 1, 1, { intarg0 }, { to_value_t(42) } },
    { "%d", "  42", 1, 1, { intarg0 }, { to_value_t(42) } },
    { "%ld", "42", 1, 1, { longarg0 }, { to_value_t(42l) } },
    { "%lld", "42", 1, 1, { longlongarg0 }, { to_value_t(42ll) } },
    { "%f", "42", 1, 1, { floatarg0 }, { to_value_t(42.0f) } },
    { "%lf", "42", 1, 1, { doublearg0 }, { to_value_t(42.0) } },
    { "%s", "42", 1, 1, { charstararg0 }, { str_to_value_t("42") } },
    { "%d%s", "42yoinks", 2, 2, { intarg0, charstararg0 }, { to_value_t(42), str_to_value_t("yoinks") } },
    { "%[^\n]", "aaaa\n", 1, 1, { charstararg0 }, { str_to_value_t("aaaa") } },
    { "%u.%u.%u", "3.19", 2, 3, { unsignedarg0, unsignedarg1, unsignedarg2 }, { to_value_t(3u), to_value_t(19u) } },
    // Failing test case from previous impl:
    { "SSH-%d.%d-%[^\n]\n", "SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.1\n", 3, 3, { intarg0, intarg1, charstararg0 }, { to_value_t(2), to_value_t(0), str_to_value_t("OpenSSH_8.2p1 Ubuntu-4ubuntu0.1") } },
    // GCC failure tests
    { "%d.%d.%d", "10.2.0", 3, 3, { intarg0, intarg1, intarg2 }, { to_value_t(10), to_value_t(2), to_value_t(0) } },
    { "%lu", "3054       ", 1, 1, { unsignedlongarg0 }, { to_value_t(3054ul) } },
    // "actual" long long and unsigned long long, from #6096
    // Note: '9223372036854775806' is the max value for 'long long'.
    { "%lld", "9223372036854775805", 1, 1, { longlongarg0 }, { to_value_t(9223372036854775805LL) } },
    { "%llu", "9223372036854775810", 1, 1, { unsignedlonglongarg0 }, { to_value_t(9223372036854775810ULL) } },
    { "%n", "", 0, 1, { intarg0 }, { to_value_t(0) } },
    { "%d %n", "1 a", 1, 2, { intarg0, intarg1 }, { to_value_t(1), to_value_t(2) } },
    { "%*d", "  42", 0, 0, {}, {} },
    { "%d%*1[:/]%d", "24/7", 2, 2, { intarg0, intarg1 }, { to_value_t(24), to_value_t(7) } },
    { " %[^a]", " b", 1, 1, { charstararg0 }, { str_to_value_t("b") } },
};

bool g_any_failed = false;

static bool check_value_conformance(TestSuite const& test)
{
    bool fail = false;
    for (size_t i = 0; i < test.argument_count; ++i) {
        auto& arg = test.arguments[i];
        auto arg_value = arg_to_value_t(arg);
        auto& value = test.expected_values[i];
        if (arg_value != value) {
            auto arg_ptr = (u32 const*)arg_value.data();
            auto value_ptr = (u32 const*)value.data();
            printf("        value %zu FAIL,\n", i);
            printf("          expected %08x%08x%08x%08x%08x%08x%08x%08x\n",
                value_ptr[0], value_ptr[1], value_ptr[2], value_ptr[3],
                value_ptr[4], value_ptr[5], value_ptr[6], value_ptr[7]);
            printf("          but got %08x%08x%08x%08x%08x%08x%08x%08x\n",
                arg_ptr[0], arg_ptr[1], arg_ptr[2], arg_ptr[3],
                arg_ptr[4], arg_ptr[5], arg_ptr[6], arg_ptr[7]);
            fail = true;
        } else {
            printf("        value %zu PASS\n", i);
        }
    }

    return !fail;
}

static void do_one_test(TestSuite const& test)
{
    printf("Testing '%s' against '%s'...\n", test.input, test.format);

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
    auto rc = sscanf(test.input, test.format,
        test.arguments[0].data, test.arguments[1].data, test.arguments[2].data, test.arguments[3].data,
        test.arguments[4].data, test.arguments[5].data, test.arguments[6].data, test.arguments[7].data);
#pragma GCC diagnostic pop

    bool overall = true;
    printf("    return value...\n");
    if (rc != test.expected_return_value) {
        printf("    return value FAIL, expected %d but got %d\n", test.expected_return_value, rc);
        overall = false;
    } else {
        printf("    return value PASS\n");
    }

    printf("    read values...\n");
    if (check_value_conformance(test)) {
        printf("    read values PASS\n");
    } else {
        printf("    read values FAIL\n");
        overall = false;
    }

    if (overall)
        printf("    overall PASS\n");
    else
        printf("    overall FAIL\n");

    VERIFY(overall);
}

TEST_CASE(scanf)
{
    for (auto& test : test_suites)
        do_one_test(test);
}
