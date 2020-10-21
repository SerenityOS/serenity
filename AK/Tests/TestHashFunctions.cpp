/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/HashFunctions.h>
#include <AK/Types.h>

TEST_CASE(int_hash)
{
    static_assert(int_hash(42) == 3564735745u);
    static_assert(int_hash(0) == 1177991625u);
}

TEST_CASE(double_hash)
{
    static_assert(double_hash(42) == 524450u);
    static_assert(double_hash(0) == 12384u);
}

TEST_CASE(pair_int_hash)
{
    static_assert(pair_int_hash(42, 17) == 339337046u);
    static_assert(pair_int_hash(0, 0) == 954888656u);
}

TEST_CASE(u64_hash)
{
    static_assert(u64_hash(42) == 2824066580u);
    static_assert(u64_hash(0) == 954888656u);
}

TEST_CASE(ptr_hash)
{
    // These tests are not static_asserts because the values are
    // different and the goal is to bind the behavior.
    if constexpr (sizeof(FlatPtr) == 8) {
        EXPECT_EQ(ptr_hash(FlatPtr(42)), 2824066580u);
        EXPECT_EQ(ptr_hash(FlatPtr(0)), 954888656u);

        EXPECT_EQ(ptr_hash(reinterpret_cast<const void*>(42)), 2824066580u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<const void*>(0)), 954888656u);
    } else {
        EXPECT_EQ(ptr_hash(FlatPtr(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(FlatPtr(0)), 1177991625u);

        EXPECT_EQ(ptr_hash(reinterpret_cast<const void*>(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<const void*>(0)), 1177991625u);
    }
}

TEST_CASE(constexpr_ptr_hash)
{
    // This test does not check the result because the goal is just to
    // ensure the function can be executed in a constexpr context. The
    // "ptr_hash" test binds the result.
    static_assert(ptr_hash(FlatPtr(42)));
}

TEST_MAIN(HashFunctions)
