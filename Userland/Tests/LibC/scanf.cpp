/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Array.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef long double longdouble;
typedef long long longlong;
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
constexpr static Array<unsigned char, 32> str_to_value_t(const char (&x)[N])
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

static Array<u8, 32> arg_to_value_t(const Argument& arg)
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

#undef DECL_WITH_TYPE

charstar _charstararg0;
charstar _charstararg1;
charstar _charstararg2;
Argument charstararg0 { sizeof(charstar), &_charstararg0[0] };
Argument charstararg1 { sizeof(charstar), &_charstararg1[0] };
Argument charstararg2 { sizeof(charstar), &_charstararg2[0] };

struct TestSuite {
    const char* format;
    const char* input;
    int expected_output;
    size_t argument_count;
    Argument arguments[8];
    Array<unsigned char, 32> expected_values[8]; // 32 bytes for each argument's value.
};

const TestSuite test_suites[] {
    { "%d", "", 0, 0, {}, {} },
    { "%x", "0x519", 1, 1, { unsignedarg0 }, { to_value_t(0x519) } },
    { "%x", "0x51g", 1, 1, { unsignedarg0 }, { to_value_t(0x51u) } },
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
};

bool g_any_failed = false;

static bool check_value_conformance(const TestSuite& test)
{
    bool fail = false;
    for (int i = 0; i < test.expected_output; ++i) {
        auto& arg = test.arguments[i];
        auto arg_value = arg_to_value_t(arg);
        auto& value = test.expected_values[i];
        if (arg_value != value) {
            auto arg_ptr = (const u32*)arg_value.data();
            auto value_ptr = (const u32*)value.data();
            printf("        value %d FAIL, expected %04x%04x%04x%04x%04x%04x%04x%04x but got %04x%04x%04x%04x%04x%04x%04x%04x\n",
                i,
                value_ptr[0], value_ptr[1], value_ptr[2], value_ptr[3],
                value_ptr[4], value_ptr[5], value_ptr[6], value_ptr[7],
                arg_ptr[0], arg_ptr[1], arg_ptr[2], arg_ptr[3],
                arg_ptr[4], arg_ptr[5], arg_ptr[6], arg_ptr[7]);
            fail = true;
        } else {
            printf("        value %d PASS\n", i);
        }
    }

    return !fail;
}

static void do_one_test(const TestSuite& test)
{
    printf("Testing '%s' against '%s'...\n", test.input, test.format);

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
    auto rc = sscanf(test.input, test.format,
        test.arguments[0].data, test.arguments[1].data, test.arguments[2].data, test.arguments[3].data,
        test.arguments[4].data, test.arguments[5].data, test.arguments[6].data, test.arguments[7].data);
#pragma GCC diagnostic pop

    bool overall = true;
    printf("    output value...\n");
    if (rc != test.expected_output) {
        printf("    output value FAIL, expected %d but got %d\n", test.expected_output, rc);
        overall = false;
    } else {
        printf("    output value PASS\n");
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

    g_any_failed = g_any_failed || !overall;
}

int main()
{
    for (auto& test : test_suites)
        do_one_test(test);

    return g_any_failed ? 1 : 0;
}
