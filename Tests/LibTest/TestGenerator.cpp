/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtraDetails.h>
#include <LibTest/PBT/Generator.h>
#include <LibTest/TestCase.h>

RANDOMIZED_TEST_CASE(unsigned_int_max_bounds)
{
    u32 n = Gen::unsigned_int(10);
    EXPECT(/* n >= 0 && */ n <= 10); // It's an unsigned int, of course it's >= 0
}

RANDOMIZED_TEST_CASE(unsigned_int_min_max_bounds)
{
    u32 n = Gen::unsigned_int(3, 6);
    EXPECT(n >= 3 && n <= 6);
}

RANDOMIZED_TEST_CASE(assume)
{
    u32 n = Gen::unsigned_int(10);
    ASSUME(n % 2 == 0); // This will try to generate until it finds an even number
    EXPECT(n % 2 == 0); // This will then succeed
    // (It can give up if the value passing the predicate is extremely unlikely.)
}

/* TODO can we test that a test fails, perhaps asserting on the stderr message? 
RANDOMIZED_TEST_CASE(degenerate_assume)
{
    u32 n = Gen::unsigned_int(10);
    ASSUME(n > 10); // This will never succeed
    EXPECT(n > 10);
}
*/

RANDOMIZED_TEST_CASE(map_like)
{
    u32 n1 = Gen::unsigned_int(10);
    u32 n2 = n1 * 2;
    EXPECT(n2 % 2 == 0);
}

RANDOMIZED_TEST_CASE(bind_like)
{
    u32 n1 = Gen::unsigned_int(1, 9);
    u32 n2 = Gen::unsigned_int(n1 * 10, n1 * 100);
    EXPECT(n2 >= 10 && n2 <= 900);
}

/* An example of an user-defined generator (for the test bind_vector_suboptimal).
  
   For why this is a suboptimal way to generate collections, see the comment in
   Shrink::shrink_delete().

   TL;DR: this makes the length non-local to the items we're trying to delete
   (except the first item).

   There's a better way: flip a (biased) coin to decide whether to generate
   a next item. That makes each item much better shrinkable, since its
   contribution to the sequence length (a boolean 0 or 1) is right next to its
   own data.
*/
template<typename FN>
Vector<InvokeResult<FN>> vector_suboptimal(FN item_gen)
{
    u32 length = Gen::unsigned_int(5);
    Vector<InvokeResult<FN>> acc;
    for (u32 i = 0; i < length; ++i) {
        acc.append(item_gen());
    }
    return acc;
}

RANDOMIZED_TEST_CASE(bind_vector_suboptimal)
{
    u32 max_item = 5;
    Vector<u32> vec = vector_suboptimal([&]() { return Gen::unsigned_int(max_item); });
    u32 sum = 0;
    for (u32 n : vec) {
        sum += n;
    }
    // TODO make this test shrink more consistently
    // Can be gleaned with a variant on:
    // while true; do ./Build/lagom/bin/TestGenerator; done
    warnln("vec {}, sum {}", vec, sum);
    EXPECT(sum <= max_item * vec.size());
}
