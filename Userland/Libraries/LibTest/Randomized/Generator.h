/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h>
#include <LibTest/Randomized/RandomRun.h>

#include <AK/Function.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Tuple.h>

namespace Test {
namespace Randomized {

// Returns a random double value in range 0..1.
inline double get_random_probability()
{
    static constexpr u32 max_u32 = NumericLimits<u32>::max();
    u32 random_u32 = AK::get_random_uniform(max_u32);
    return static_cast<double>(random_u32) / static_cast<double>(max_u32);
}

// Generators take random bits from the RandomnessSource and return a value
// back.
//
// Example:
// - Gen::u32(5,10) --> 9, 7, 5, 10, 8, ...
namespace Gen {

// An unsigned integer generator.
//
// The minimum value will always be 0.
// The maximum value is given by user in the argument.
//
// Gen::unsigned_int(10) -> value 5, RandomRun [5]
//                       -> value 8, RandomRun [8]
//                       etc.
//
// Shrinks towards 0.
inline u32 unsigned_int(u32 max)
{
    if (max == 0)
        return 0;

    u32 random = Test::randomness_source().draw_value(max, [&]() {
        // `clamp` to guard against integer overflow and calling get_random_uniform(0).
        u32 exclusive_bound = AK::clamp(max + 1, max, NumericLimits<u32>::max());
        return AK::get_random_uniform(exclusive_bound);
    });
    return random;
}

// An unsigned integer generator in a particular range.
//
// Gen::unsigned_int(3,10) -> value 3,  RandomRun [0]
//                         -> value 8,  RandomRun [5]
//                         -> value 10, RandomRun [7]
//                         etc.
//
// In case `min == max`, the RandomRun footprint will be smaller: no randomness
// is needed.
//
// Gen::unsigned_int(3,3) -> value 3, RandomRun [] (always)
//
// Shrinks towards the minimum.
inline u32 unsigned_int(u32 min, u32 max)
{
    VERIFY(max >= min);
    return unsigned_int(max - min) + min;
}

// Randomly (uniformly) selects a value out of the given arguments.
//
// Gen::one_of(20,5,10) --> value 20, RandomRun [0]
//                      --> value 5,  RandomRun [1]
//                      --> value 10, RandomRun [2]
//
// Shrinks towards the earlier arguments (above, towards 20).
template<typename... Ts>
requires(sizeof...(Ts) > 0)
CommonType<Ts...> one_of(Ts... choices)
{
    Vector<CommonType<Ts...>> choices_vec { choices... };

    constexpr size_t count = sizeof...(choices);
    size_t i = unsigned_int(count - 1);
    return choices_vec[i];
}

template<typename T>
struct Choice {
    i32 weight;
    T value;
};
// Workaround for clang bug fixed in clang 17
template<typename T>
Choice(i32, T) -> Choice<T>;

// Randomly (uniformly) selects a value out of the given weighted arguments.
//
// Gen::frequency(
//   Gen::Choice {5,999},
//   Gen::Choice {1,111},
// )
//     --> value 999 (5 out of 6 times), RandomRun [0]
//     --> value 111 (1 out of 6 times), RandomRun [1]
//
// Shrinks towards the earlier arguments (above, towards 'x').
template<typename... Ts>
requires(sizeof...(Ts) > 0)
CommonType<Ts...> frequency(Choice<Ts>... choices)
{
    Vector<Choice<CommonType<Ts...>>> choices_vec { choices... };

    u32 sum = 0;
    for (auto const& choice : choices_vec) {
        VERIFY(choice.weight > 0);
        sum += static_cast<u32>(choice.weight);
    }

    u32 target = unsigned_int(sum);
    size_t i = 0;
    for (auto const& choice : choices_vec) {
        u32 weight = static_cast<u32>(choice.weight);
        if (weight >= target) {
            return choice.value;
        }
        target -= weight;
        ++i;
    }
    return choices_vec[i - 1].value;
}

// An unsigned integer generator in the full u32 range.
//
// 8/17 (47%) of the time it will bias towards 8bit numbers,
// 4/17 (23%) towards 4bit numbers,
// 2/17 (12%) towards 16bit numbers,
// 1/17 (6%) towards 32bit numbers,
// 2/17 (12%) towards edge cases like 0 and NumericLimits::max() of various unsigned int types.
//
// Gen::unsigned_int() -> value 3,     RandomRun [0,3]
//                     -> value 8,     RandomRun [1,8]
//                     -> value 100,   RandomRun [2,100]
//                     -> value 5,     RandomRun [3,5]
//                     -> value 255,   RandomRun [4,1]
//                     -> value 65535, RandomRun [4,2]
//                     etc.
//
// Shrinks towards 0.
inline u32 unsigned_int()
{
    u32 bits = frequency(
        // weight, bits
        Choice { 4, 4 },
        Choice { 8, 8 },
        Choice { 2, 16 },
        Choice { 1, 32 },
        Choice { 2, 0 });

    // The special cases go last as they can be the most extreme (large) values.

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

// A generator returning `true` with the given `probability` (0..1).
//
// If probability <= 0, doesn't use any randomness and returns false.
// If probability >= 1, doesn't use any randomness and returns true.
//
// In general case:
// Gen::weighted_boolean(0.75)
//   -> value false, RandomRun [0]
//   -> value true,  RandomRun [1]
//
// Shrinks towards false.
inline bool weighted_boolean(double probability)
{
    if (probability <= 0)
        return false;
    if (probability >= 1)
        return true;

    u32 random_int = Test::randomness_source().draw_value(1, [&]() {
        double drawn_probability = get_random_probability();
        return drawn_probability <= probability ? 1 : 0;
    });
    bool random_bool = random_int == 1;
    return random_bool;
}

// A (fair) boolean generator.
//
// Gen::boolean()
//   -> value false, RandomRun [0]
//   -> value true,  RandomRun [1]
//
// Shrinks towards false.
inline bool boolean()
{
    return weighted_boolean(0.5);
}

// A vector generator of a random length between the given limits.
//
// Gen::vector(2,3,[]() { return Gen::unsigned_int(5); })
//   -> value [1,5],      RandomRun [1,1,1,5,0]
//   -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
//   etc.
//
// In case `min == max`, the RandomRun footprint will be smaller, as there will
// be no randomness involved in figuring out the length:
//
// Gen::vector(3,3,[]() { return Gen::unsigned_int(5); })
//   -> value [1,3], RandomRun [1,3]
//   -> value [5,2], RandomRun [5,2]
//   etc.
//
// Shrinks towards shorter vectors, with simpler elements inside.
template<typename Fn>
inline Vector<InvokeResult<Fn>> vector(size_t min, size_t max, Fn item_gen)
{
    VERIFY(max >= min);

    size_t size = 0;
    Vector<InvokeResult<Fn>> acc;

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
    //
    // This algorithm is used instead of the more intuitive "generate length,
    // then generate that many items" algorithm, because it produces RandomRun
    // patterns that shrink more easily.
    //
    // See the Hypothesis paper [1], section 3.3, around the paragraph starting
    // with "More commonly".
    //
    // [1]: https://drops.dagstuhl.de/opus/volltexte/2020/13170/pdf/LIPIcs-ECOOP-2020-13.pdf
    while (size < min) {
        acc.append(item_gen());
        ++size;
    }

    double average = static_cast<double>(min + max) / 2.0;
    VERIFY(average > 0);

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

// A vector generator of a given length.
//
// Gen::vector_of_length(3,[]() { return Gen::unsigned_int(5); })
//   -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
//   -> value [2,9,3],    RandomRun [1,2,1,9,1,3,0]
//   etc.
//
// Shrinks towards shorter vectors, with simpler elements inside.
template<typename Fn>
inline Vector<InvokeResult<Fn>> vector(size_t length, Fn item_gen)
{
    return vector(length, length, item_gen);
}

// A vector generator of a random length between 0 and 32 elements.
//
// If you need a different length, use vector(max,item_gen) or
// vector(min,max,item_gen).
//
// Gen::vector([]() { return Gen::unsigned_int(5); })
//   -> value [],         RandomRun [0]
//   -> value [1],        RandomRun [1,1,0]
//   -> value [1,5],      RandomRun [1,1,1,5,0]
//   -> value [1,5,0],    RandomRun [1,1,1,5,1,0,0]
//   -> value [1,5,0,2],  RandomRun [1,1,1,5,1,0,1,2,0]
//   etc.
//
// Shrinks towards shorter vectors, with simpler elements inside.
template<typename Fn>
inline Vector<InvokeResult<Fn>> vector(Fn item_gen)
{
    return vector(0, 32, item_gen);
}

} // namespace Gen
} // namespace Randomized
} // namespace Test
