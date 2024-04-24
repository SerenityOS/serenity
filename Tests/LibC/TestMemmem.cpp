/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Types.h>
#include <string.h>

struct TestCase {
    u8 const* haystack;
    size_t haystack_length;
    u8 const* needle;
    size_t needle_length;
    ssize_t matching_offset { -1 };
};

static TestCase const g_test_cases[] = {
    { (u8 const*) {}, 0u, (u8 const*) {}, 0u, 0 },
    { (u8 const[]) { 1, 2, 3 }, 3u, (u8 const[]) { 1, 2, 3 }, 3u, 0 },
    { (u8 const[]) { 1, 2, 4 }, 3u, (u8 const[]) { 1, 2, 3 }, 3u, -1 },
    { (u8 const*)"abcdef", 6u, (u8 const[]) {}, 0u, 0 },
    { (u8 const*)"abcdef", 6u, (u8 const*)"de", 2u, 3 },
    { (u8 const[]) { 0, 1, 2, 5, 2, 5 }, 6u, (u8 const[]) { 1 }, 1u, 1 },
    { (u8 const[]) { 0, 1, 2, 5, 2, 5 }, 6u, (u8 const[]) { 1, 2 }, 2u, 1 },
    { (u8 const[]) { 0, 1, 1, 2 }, 4u, (u8 const[]) { 1, 5 }, 2u, -1 },
    { (u8 const[64]) { 0 }, 64u, (u8 const[33]) { 0 }, 33u, 0 },
    { (u8 const[64]) { 0, 1, 1, 2 }, 64u, (u8 const[33]) { 1, 1 }, 2u, 1 },
};

TEST_CASE(memmem_search)
{
    size_t i = 0;
    for (auto const& test_case : g_test_cases) {
        auto expected = test_case.matching_offset >= 0 ? test_case.haystack + test_case.matching_offset : nullptr;
        auto result = memmem(test_case.haystack, test_case.haystack_length, test_case.needle, test_case.needle_length);
        if (result != expected) {
            FAIL(ByteString::formatted("Test {} FAILED! expected {:p}, got {:p}", i, expected, result));
        }
        ++i;
    }
}
