/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

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

int main()
{
    bool failed = false;
    size_t i = 0;
    for (const auto& test_case : g_test_cases) {
        auto expected = test_case.matching_offset >= 0 ? test_case.haystack + test_case.matching_offset : nullptr;
        auto result = memmem(test_case.haystack, test_case.haystack_length, test_case.needle, test_case.needle_length);
        if (result != expected) {
            failed = true;
            fprintf(stderr, "Test %zu FAILED! expected %p, got %p\n", i, expected, result);
        }
        ++i;
    }

    printf(failed ? "FAIL\n" : "PASS\n");
    return failed ? 1 : 0;
}
