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
   - Gen::u32(5,10) --> 9, 7, 5, 10, 8, ...
 */
namespace Gen {

/* An unsigned integer generator.

   The minimum value will always be 0.
   The maximum value is given by user in the argument.

   Gen::unsigned_int(10) -> value 5, RandomRun [5]
                         -> value 8, RandomRun [8]
                         etc.

   Shrinks towards 0.

   This is a foundational generator: it's one of the only two generators
   that add to / read values from the RandSource (the other being
   weighted_boolean). Other generators will be largely built from this one.
 */
static u32 unsigned_int(u32 max)
{
    // BOILERPLATE - START
    //
    // TODO extract the common parts from unsigned_int(u32) and
    // weighted_boolean(float)?
    RandSource& rand = Test::rand_source();
    RandomRun& run = rand.run();
    if (rand.is_live()) {
        if (run.is_full()) {
            Test::set_current_test_result(TestResult::HitLimit);
        }
        // THE INTERESTING PART - START
        u32 value = AK::get_random_uniform(max + 1);
        run.append(value);
        return value;
        // THE INTERESTING PART - END
    }
    // Not live => Recorded.
    auto next = run.next();
    if (next.has_value()) {
        // THE INTERESTING PART - START
        return min(next.value(), max);
        // THE INTERESTING PART - END
    }
    Test::set_current_test_result(TestResult::Overrun);
    return 0; // The value returned doesn't matter at this point but we need to return _something_
    // BOILERPLATE - END
}

/* An unsigned integer generator in a particular range.

   The minimum value is the smaller of the two arguments.
   The maximum value is the larger of the two arguments.

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
static inline u32 unsigned_int(u32 min, u32 max)
{
    if (min > max) {
        return unsigned_int(max, min);
    }
    if (min == max) {
        return min;
    }
    return unsigned_int(max - min) + min;
}

/* A generator returning `true` with the given `probability` (0..1).

   If probability <= 0, doesn't use any randomness and returns false.
   If probability >= 1, doesn't use any randomness and returns true.

   In general case:
   Gen::weighted_boolean(0.75)
     -> value false, RandomRun [0]
     -> value true,  RandomRun [1]

   Shrinks towards false.

   This is a foundational generator: it's one of the only two generators
   that add to / read values from the RandSource (the other being
   unsigned_int).
 */
static inline bool weighted_boolean(float probability) {
    if (probability <= 0) return false;
    if (probability >= 1) return true;

    // BOILERPLATE - START
    //
    // TODO extract the common parts from unsigned_int(u32) and
    // weighted_boolean(float)?
    RandSource& rand = Test::rand_source();
    RandomRun& run = rand.run();
    if (rand.is_live()) {
        if (run.is_full()) {
            Test::set_current_test_result(TestResult::HitLimit);
        }
        // THE INTERESTING PART - START
        static constexpr u32 max_u32 = NumericLimits<u32>::max();
        u32 random_u32 = AK::get_random_uniform(max_u32);
        double random_float = static_cast<double>(random_u32) / static_cast<double>(max_u32);

        bool random_bool = random_float <= probability;
        run.append(random_bool ? 1 : 0);
        return random_bool;
        // THE INTERESTING PART - END
    }
    // Not live => Recorded.
    auto next = run.next();
    if (next.has_value()) {
        // THE INTERESTING PART - START
        auto next_value = next.value();
        bool next_bool = next_value > 0;
        return next_bool;
        // THE INTERESTING PART - END
    }
    Test::set_current_test_result(TestResult::Overrun);
    return false; // The value returned doesn't matter at this point but we need to return _something_
    // BOILERPLATE - END
}

/* A vector generator of a random length between the given limits.

   The minimum value is the smaller of the two size_t arguments.
   The maximum value is the larger of the two size_t arguments.

   Gen::vector(2,3,[]() { return Gen::unsigned_int(5); })
     -> value [1,5],      RandomRun [1,1,1,5,0]
     -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
     etc.

   In case `min == max`, the RandomRun footprint will be smaller, as there will
   be no randomness involved in figuring out the length:

   Gen::vector(3,3,[]() { return Gen::unsigned_int(5); })
     -> value [1,3], RandomRun [1,3]
     -> value [5,2], RandomRun [5,2]
     etc.

   Shrinks towards shorter vectors, with simpler elements inside.
 */
template<typename FN>
inline Vector<InvokeResult<FN>> vector(size_t min, size_t max, FN item_gen) {
    if (max < min) {
        return vector(max, min, item_gen);
    }
    if (max <= 0) {
        return Vector<InvokeResult<FN>>();
    }
    size_t size = 0;
    Vector<InvokeResult<FN>> acc;

    // Special case: no randomness for the boolean
    if (min == max) {
        while (size < min) {
            acc.append(item_gen());
            ++size;
        }
        return acc;
    }

    // General case: before each item we "flip a coin" to decide whether to
    // generate another one.
    while (size < min) {
        acc.append(item_gen());
        ++size;
    }

    float average = static_cast<float>(min + max) / 2.0;
    EXPECT(average > 0);

    // A geometric distribution: https://en.wikipedia.org/wiki/Geometric_distribution#Moments_and_cumulants
    // The below derives from the E(X) = 1/p formula.
    //
    // We need to flip the `p` to `1-p` as our success ("another item!") is
    // a "failure" in the geometric distribution's interpretation ("we fail X
    // times before succeeding the first time").
    //
    // That gives us `1 - 1/p`. Then, E(X) also contains the final success, so we
    // need to say `1 + average` instead of `average`, as it will mean "our X
    // items + the final failure that stops the process".
    float probability = 1.0 - 1.0 / (1.0 + average);

    while (size < max) {
        if (weighted_boolean(probability)) {
            acc.append(item_gen());
            ++size;
        } else {
            break;
        }
    }

    return acc;
}

/* A vector generator of a random length between 0 and `max` elements.

   Gen::vector(2,[]() { return Gen::unsigned_int(5); })
     -> value [],         RandomRun [0]
     -> value [1],        RandomRun [1,1,0]
     -> value [1,5],      RandomRun [1,1,1,5,0]
     etc.

   Shrinks towards shorter vectors, with simpler elements inside.
 */
template<typename FN>
inline Vector<InvokeResult<FN>> vector(size_t max, FN item_gen) {
    return vector(0,max,item_gen);
}

/* A vector generator of a random length between 0 and 32 elements.

   If you need a different length, use vector(max,item_gen) or
   vector(min,max,item_gen).
   
   Gen::vector([]() { return Gen::unsigned_int(5); })
     -> value [],         RandomRun [0]
     -> value [1],        RandomRun [1,1,0]
     -> value [1,5],      RandomRun [1,1,1,5,0]
     -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
     -> value [1,5,0,2],  RandomRun [1,1,1,5,1,0,1,2,0]
     etc.

   Shrinks towards shorter vectors, with simpler elements inside.
 */
template<typename FN>
inline Vector<InvokeResult<FN>> vector(FN item_gen) {
    return vector(0,32,item_gen);
}

/* A vector generator of a given length.

   Gen::vector_of_length(3,[]() { return Gen::unsigned_int(5); })
     -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
     -> value [2,9,3],    RandomRun [1,2,1,9,1,3,0]
     etc.

   Shrinks towards shorter vectors, with simpler elements inside.
 */
template<typename FN>
inline Vector<InvokeResult<FN>> vector_of_length(size_t len, FN item_gen) {
    return vector(len,len,item_gen);
}

} // namespace Gen
