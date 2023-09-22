/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h>
#include <LibTest/Randomized/RandSource.h>
#include <LibTest/Randomized/RandomRun.h>

#include <AK/Function.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Tuple.h>

/* Returns a random double value in range 0..1.
 */
inline double get_random_probability()
{
    static constexpr u32 max_u32 = NumericLimits<u32>::max();
    u32 random_u32 = AK::get_random_uniform(max_u32);
    return static_cast<double>(random_u32) / static_cast<double>(max_u32);
}

/* Generators take random bits from the RandSource and return a value back.

   Example:
   - Gen::u32(5,10) --> 9, 7, 5, 10, 8, ...
 */
namespace Gen {

/* An internal foundational generator: it adds values into / reads values from
   the RandSource.
 */
static inline u32 draw_from_rand_source(u32 max, Function<u32()> random_generator)
{
    RandSource& rand = Test::rand_source();
    RandomRun& run = rand.run();
    if (rand.is_live()) {
        u32 value = random_generator();
        run.append(value);
        return value;
    }

    auto next = run.next();
    if (next.has_value()) {
        return min(next.value(), max);
    }

    Test::set_current_test_result(TestResult::Overrun);
    return 0; // The value returned doesn't matter at this point but we need to return something
}

/* An unsigned integer generator.

   The minimum value will always be 0.
   The maximum value is given by user in the argument.

   Gen::unsigned_int(10) -> value 5, RandomRun [5]
                         -> value 8, RandomRun [8]
                         etc.

   Shrinks towards 0.
 */
static u32 unsigned_int(u32 max)
{
    u32 random = draw_from_rand_source(max, [&]() {
        return AK::get_random_uniform(max + 1);
    });
    return random;
}

/* An unsigned integer generator in a particular range.

   The minimum value is the smaller of the two arguments.
   The maximum value is the larger of the two arguments.

   Gen::unsigned_int(3,10) -> value 3,  RandomRun [0]
                           -> value 8,  RandomRun [5]
                           -> value 10, RandomRun [7]
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

/* Randomly (uniformly) selects a value out of the given arguments.

   Gen::one_of(20,5,10) --> value 20, RandomRun [0]
                        --> value 5,  RandomRun [1]
                        --> value 10, RandomRun [2]

   Shrinks towards the earlier arguments (above, towards 20).
*/
template<typename... Ts>
static inline CommonType<Ts...> one_of(Ts... choices)
{
    constexpr size_t count = sizeof...(choices);
    if (count == 0) {
        REJECT("one_of: The list of values can't be empty");
    }

    Vector<CommonType<Ts...>> choices_vec { choices... };

    size_t i = unsigned_int(count - 1);
    return choices_vec[i];
}

/* Randomly (uniformly) selects a value out of the given weighted arguments.

   Gen::frequency(
     Tuple{5,'x'},
     Tuple{1,'o'}
   )
       --> value 'x' (5 out of 6 times), RandomRun [0]
       --> value 'o' (1 out of 6 times), RandomRun [1]

   Shrinks towards the earlier arguments (above, towards 'x').
*/
template<typename... Ts>
static inline CommonType<Ts...> frequency(Tuple<i32, Ts>... choices)
{
    constexpr size_t count = sizeof...(choices);
    if (count == 0) {
        REJECT("frequency: The list of choices can't be empty");
    }

    Vector<Tuple<i32, CommonType<Ts...>>> choices_vec { choices... };

    i32 sum = 0;
    for (Tuple<i32, CommonType<Ts...>> choice : choices_vec) {
        i32 weight = choice.template get<0>();
        if (weight <= 0) {
            REJECT("frequency: Choice weights must be positive");
        }
        sum += choice.template get<0>();
    }

    i32 target = AK::get_random_uniform(sum + 1);
    size_t i = 0;
    for (Tuple<i32, CommonType<Ts...>> choice : choices_vec) {
        i32 weight = choice.template get<0>();
        if (weight >= target) {
            return choice.template get<1>();
        }
        target -= weight;
        ++i;
    }
    return choices_vec[i - 1].template get<1>();
}

/* Randomly (uniformly) selects a value out of the given weighted arguments.

   Gen::frequency(
     Tuple{0.5,'o'}
     Tuple{2.5,'x'},
   )
       --> value 'o' (with probability 0.5/3.0), RandomRun [0]
       --> value 'x' (with probability 2.5/3.0), RandomRun [1]

   Shrinks towards the earlier arguments (above, towards 'o').
*/
template<typename... Ts>
static inline CommonType<Ts...> frequency(Tuple<double, Ts>... choices)
{
    constexpr size_t count = sizeof...(choices);
    if (count == 0) {
        REJECT("frequency: The list of choices can't be empty");
    }

    Vector<Tuple<double, CommonType<Ts...>>> choices_vec { choices... };

    double sum = 0;
    for (Tuple<double, CommonType<Ts...>> choice : choices_vec) {
        double weight = choice.template get<0>();
        if (weight <= 0) {
            REJECT("frequency: Choice weights must be positive");
        }
        sum += choice.template get<0>();
    }

    double target = get_random_probability() * sum;
    size_t i = 0;
    for (Tuple<double, CommonType<Ts...>> choice : choices_vec) {
        double weight = choice.template get<0>();
        if (weight >= target) {
            return choice.template get<1>();
        }
        target -= weight;
        ++i;
    }
    return choices_vec[i - 1].template get<1>();
}

/* An unsigned integer generator in the full u32 range.

   It will bias towards 0..2^8,, then 0..2^4, then edge cases like MAX_U32
   integer, then 0..2^16, then 0..2^32.

   Gen::unsigned_int(3,10) -> value 3,  RandomRun [0]
                           -> value 8,  RandomRun [5]
                           -> value 10, RandomRun [7]
                           etc.

   In case `min == max`, the RandomRun footprint will be smaller, as we'll
   switch to a `constant` and won't need any randomness to generate that
   value:

   Gen::unsigned_int(3,3) -> value 3, RandomRun [] (always)

   Shrinks towards the smaller argument.
*/
static inline u32 unsigned_int()
{
    u32 bits = frequency(
        // weight, bits
        Tuple { 4, 4 },
        Tuple { 8, 8 },
        Tuple { 2, 0 },
        Tuple { 2, 16 },
        Tuple { 1, 32 });

    if (bits == 0) {
        // Special cases, eg. max integers for u8, u16, u32.
        return one_of(
            0U,
            NumericLimits<u8>::max(),
            NumericLimits<u16>::max(),
            NumericLimits<u32>::max());
    }

    u32 max = ((u64)1 << bits) - 1;
    return unsigned_int(max);
}

/* A generator returning `true` with the given `probability` (0..1).

   If probability <= 0, doesn't use any randomness and returns false.
   If probability >= 1, doesn't use any randomness and returns true.

   In general case:
   Gen::weighted_boolean(0.75)
     -> value false, RandomRun [0]
     -> value true,  RandomRun [1]

   Shrinks towards false.
 */
static inline bool weighted_boolean(double probability)
{
    if (probability <= 0)
        return false;
    if (probability >= 1)
        return true;

    u32 random_int = draw_from_rand_source(1, [&]() {
        double drawn_probability = get_random_probability();
        return drawn_probability <= probability ? 1 : 0;
    });
    bool random_bool = random_int == 1;
    return random_bool;
}

static inline bool boolean()
{
    return weighted_boolean(0.5);
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
inline Vector<InvokeResult<FN>> vector(size_t min, size_t max, FN item_gen)
{
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

    double average = static_cast<double>(min + max) / 2.0;
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
    double probability = 1.0 - 1.0 / (1.0 + average);

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
inline Vector<InvokeResult<FN>> vector(size_t max, FN item_gen)
{
    return vector(0, max, item_gen);
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
inline Vector<InvokeResult<FN>> vector(FN item_gen)
{
    return vector(0, 32, item_gen);
}

/* A vector generator of a given length.

   Gen::vector_of_length(3,[]() { return Gen::unsigned_int(5); })
     -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
     -> value [2,9,3],    RandomRun [1,2,1,9,1,3,0]
     etc.

   Shrinks towards shorter vectors, with simpler elements inside.
 */
template<typename FN>
inline Vector<InvokeResult<FN>> vector_of_length(size_t len, FN item_gen)
{
    return vector(len, len, item_gen);
}

} // namespace Gen
