/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h>
#include <LibTest/PBT/GenResult.h>
#include <LibTest/PBT/RandSource.h>
#include <LibTest/PBT/RandomRun.h>

#include <AK/Function.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringView.h>

/* Generators take random bits from the RandSource and return a value back.

   Example:
   - Gen::constant(100)         --> 100
   - Gen::i32(-5,5)             --> 0, -1, 5, -5, 3, ...
   - Gen::vector(Gen::i32(1,3)) --> {2,1,1,3}, {1}, {}, {1,3,3}, ...
 */
namespace Gen {

/* This is a foundational generator: it's the only one low-level enough to
   handle the adding to / reading of values from the RandSource.

   Other generators will be largely built from this one.

   The minimum value will always be 0.
   The maximum value is given by user in the argument.

   Gen::unsigned_int(10) -> value 5, RandomRun [5]
                         -> value 8, RandomRun [8]
                         etc.

   Shrinks towards 0.
 */
inline u32 unsigned_int(u32 max)
{
    RandSource rand = Test::rand_source();
    RandomRun& run = rand.run();
    if (rand.is_live()) {
        if (run.is_full()) {
            FAIL("Generators have hit maximum RandomRun length (generating too much data).");
            // TODO do we need a different FAIL that doesn't get shushed during shrinking?
        }
        u32 value = AK::get_random_uniform(max);
        run.append(value);
        return value;
    }
    // Not live => Recorded.
    auto next = run.next();
    if (next.has_value()) {
        return next.value();
    }
    FAIL("Ran out of recorded bits");
    VERIFY_NOT_REACHED();
}

/* An unsigned integer generator in a particular range.

   The minimum value is the smaller of the two arguments.
   The maximum value is the larger of the two arguments.

   In the general case this is the behaviour:

   Gen::unsigned_int(3,10) -> value 3,  RandomRun [3]
                           -> value 8,  RandomRun [8]
                           -> value 10, RandomRun [10]
                           etc.

   In case `min == max`, the RandomRun footprint will be smaller, as we'll
   switch to a `constant` and won't need any randomness to generate that
   value:

   Gen::unsigned_int(3,3) -> value 3, RandomRun [] (always)

   Shrinks towards the smaller argument.
 */
inline u32 unsigned_int(u32 min, u32 max)
{
    if (min > max) {
        return unsigned_int(max, min);
    }
    if (min == max) {
        return min;
    }
    return unsigned_int(max - min) + min;
}

} // namespace Gen
