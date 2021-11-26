/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Types.h>
#include <string.h>

struct TestCase {
    const u8* haystack;
    size_t haystack_length;
    const u8* needle;
    size_t needle_length;
    ssize_t matching_offset { -1 };
};

const static TestCase g_test_cases[] = {
    { (const u8*) {}, 0u, (const u8*) {}, 0u, 0 },
    { (const u8[]) { 1, 2, 3 }, 3u, (const u8[]) { 1, 2, 3 }, 3u, 0 },
    { (const u8[]) { 1, 2, 4 }, 3u, (const u8[]) { 1, 2, 3 }, 3u, -1 },
    { (const u8*)"abcdef", 6u, (const u8[]) {}, 0u, 0 },
    { (const u8*)"abcdef", 6u, (const u8*)"de", 2u, 3 },
    { (const u8[]) { 0, 1, 2, 5, 2, 5 }, 6u, (const u8[]) { 1 }, 1u, 1 },
    { (const u8[]) { 0, 1, 2, 5, 2, 5 }, 6u, (const u8[]) { 1, 2 }, 2u, 1 },
    { (const u8[]) { 0, 1, 1, 2 }, 4u, (const u8[]) { 1, 5 }, 2u, -1 },
    { (const u8[64]) { 0 }, 64u, (const u8[33]) { 0 }, 33u, 0 },
    { (const u8[64]) { 0, 1, 1, 2 }, 64u, (const u8[33]) { 1, 1 }, 2u, 1 },
};

TEST_CASE(memmem_search)
{
    size_t i = 0;
    for (const auto& test_case : g_test_cases) {
        auto expected = test_case.matching_offset >= 0 ? test_case.haystack + test_case.matching_offset : nullptr;
        auto result = memmem(test_case.haystack, test_case.haystack_length, test_case.needle, test_case.needle_length);
        if (result != expected) {
            FAIL(String::formatted("Test {} FAILED! expected {:p}, got {:p}", i, expected, result));
        }
        ++i;
    }
}
