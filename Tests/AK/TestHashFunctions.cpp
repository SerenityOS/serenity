/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashFunctions.h>
#include <AK/Types.h>

TEST_CASE(int_hash)
{
    static_assert(int_hash(42) == 3564735745u);
    static_assert(int_hash(0) == 1177991625u);
}

TEST_CASE(double_hash)
{
    static_assert(double_hash(666) == 171644115u);
    static_assert(double_hash(0) == 1189591134u);
    static_assert(double_hash(0xBA5EDB01) == 0u);
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

        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(42)), 2824066580u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(0)), 954888656u);
    } else {
        EXPECT_EQ(ptr_hash(FlatPtr(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(FlatPtr(0)), 1177991625u);

        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(0)), 1177991625u);
    }
}

TEST_CASE(constexpr_ptr_hash)
{
    // This test does not check the result because the goal is just to
    // ensure the function can be executed in a constexpr context. The
    // "ptr_hash" test binds the result.
    static_assert(ptr_hash(FlatPtr(42)));
}
