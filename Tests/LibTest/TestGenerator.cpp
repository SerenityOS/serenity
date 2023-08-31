/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/PBT/Generator.h>
#include <LibTest/TestCase.h>

PBTEST_CASE(constant_int, Gen::constant(42), x)
{
    EXPECT_EQ(x, 42);
}

PBTEST_CASE(constant_bool, Gen::constant(true), my_bool)
{
    EXPECT_EQ(my_bool, true);
}

PBTEST_CASE(unsigned_int_max_bounds, Gen::unsigned_int(10), n)
{
    EXPECT(n >= 0 && n <= 10);
}

PBTEST_CASE(unsigned_int_min_max_bounds, Gen::unsigned_int(3, 6), n)
{
    EXPECT(n >= 3 && n <= 6);
}

PBTEST_CASE(map_transforms,
    Gen::unsigned_int(10).map([](auto n) { return n * 2; }),
    n)
{
    EXPECT(n % 2 == 0);
}

/* TODO how to test that a test fails with a correct value?

   Ideally in this test we'd like to test that it will fail with 400.

   We'd need a way to run the test suite from _within_ a test and inspect
   the result as a value. Currently this would be tricky because of the singleton
   TestSuite.
*/
// PBTEST_CASE(map_shrinks_well,
//     Gen::unsigned_int(10).map([](auto n) { return n * 100; }),
//     n)
// {
//     EXPECT(n < 321);
// }

PBTEST_CASE(filter,
    Gen::unsigned_int(10).filter([](auto n) { return n % 2 == 0; }),
    n)
{
    EXPECT(n % 2 == 0);
}

// This is really a `map` reimplemented via `then`. There might be a better test
// lurking around, perhaps one of the vector-creating ones.
PBTEST_CASE(then,
    Gen::unsigned_int(1, 9).then([](auto n) {
        return Gen::constant(n).map([](auto n2) { return n2 * 10; });
    }),
    n)
{
    EXPECT(n >= 10 && n <= 90);
}