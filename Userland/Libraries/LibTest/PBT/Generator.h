/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/PBT/GenResult.h>
#include <LibTest/PBT/RandSource.h>
#include <LibTest/PBT/RandomRun.h>

#include <AK/Function.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <AK/StringView.h>

/* Generators are recipes for turning RandSource into a generated random value
   (more specifically, into GenResult<T>: generators can fail).

   Example:
   - Gen::constant(100)         --> 100
   - Gen::i32(-5,5)             --> 0, -1, 5, -5, 3, ...
   - Gen::vector(Gen::i32(1,3)) --> {2,1,1,3}, {1}, {}, {1,3,3}, ...
 */
template<typename T>
struct Generator : Function<GenResult<T>(RandSource)> {
    using Function<GenResult<T>(RandSource)>::Function;
    using Function<GenResult<T>(RandSource)>::operator();

/* Runs the provided function on each value of the generator.

   Gen::constant(100)                                 --> 100
   Gen::constant(100).map([](int i){ return i + 2; }) --> 102

   This doesn't incur any extra RandomRun footprint.

   Shrunk values will still honor this mapping:

   Gen::u32(10).map([](auto i){ return i * 100; }) --> 0, 100, 200, ..., 1000
                                                       even after shrinking
 */
template<typename FN>
Generator<InvokeResult<FN, T>> map(FN map_fn)
{
    return [gen = move(*this), map_fn = move(map_fn)](RandSource const& rand) {
        GenResult<T> result = gen(rand);
        return result.visit(
            [&](Generated<T> g) { return GenResult<InvokeResult<FN, T>>::generated(g.run, map_fn(g.value)); },
            [](Rejected r) { return GenResult<InvokeResult<FN, T>>::rejected(r.reason); });
    };
}

/* Filters all generated values by the provided predicate.
   (Keeps all values x where predicate(x) == true.)

   Gen::u32(10)                                         --> 0,1,2,3,4,5,6,7,8,9,10
   Gen::u32(10).filter([](int n){ return n % 2 == 1; }) -->   1,  3,  5,  7,  9

   This doesn't incur any extra RandomRun footprint.

   Shrunk values will still honor this filtering.
 */
template<typename FN>
Generator<T> filter(FN predicate)
{
    return [gen = move(*this), predicate = move(predicate)](RandSource const& rand) {
        GenResult<T> result = gen(rand);
        return result.visit(
            [&](Generated<T> g) {
                if (predicate(g.value)) {
                    return result;
                }
                return GenResult<T>::rejected("Value filtered out"sv);
            },
            [&](Rejected) { return result; });
    };
}

template<typename FN>
InvokeResult<FN, T> then(FN bind_fn)
{
    // TODO naming? and_then, then, bind, chain, ...
    // TODO better syntax... we really want foo.then(...) instead of then(foo, ...)
    return [gen = move(*this), bind_fn = move(bind_fn)](RandSource const& rand) {
        GenResult<T> result = gen(rand);
        return result.visit(
            [&](Generated<T> g) {
                InvokeResult<FN, T> new_gen = bind_fn(g.value); // Really a Generator<T2> but my C++-fu is too low to express this
                auto rand2 = RandSource::live_with(g.run);
                return new_gen(rand2);
            },
            [&](Rejected) { return result; }); // forward the Rejected variant
    };
}

};

namespace Gen {



/* This generator always succeeds to generate the same value.
   FP folks will know this as `pure`, `return` or `succeed`.

   Gen::constant(x) -> value x, RandomRun [] (always)

   Shrinkers have no effect on the value.
 */
template<typename T>
inline Generator<T> constant(T const& value)
{
    return [value = move(value)](RandSource const& rand) {
        return GenResult<T>::generated(rand.run(), value);
    };
}

/* This generator always fails to generate a value.

   The given reason will be noted by the test runner and reported at the end
   if the runner fails to generate any value.

   Gen::reject("Bad hair day") -> no value, RandomRun [] (always)

   Shrinkers have no effect.
 */
template<typename T>
inline Generator<T> reject(String const& reason)
{
    return [&](RandSource const&) {
        return GenResult<T>::rejected(reason);
    };
}

/* This is a foundational generator: it's the only one low-level enough to
   handle the adding to / reading of values from the RandSource.

   Other generators will be largely built from this one via combinators.

   The minimum value will always be 0.
   The maximum value is given by user in the argument.

   Gen::unsigned_int(10) -> value 5, RandomRun [5]
                         -> value 8, RandomRun [8]
                         etc.

   Shrinks towards 0.
 */
inline Generator<u32> unsigned_int(u32 max)
{
    return [max](RandSource const& rand) {
        auto run = rand.run();
        if (run.is_full()) {
            return GenResult<u32>::rejected("Generators have hit maximum RandomRun length (generating too much data)."sv);
        }
        if (rand.is_live()) {
            auto value = AK::get_random_uniform(max);
            run.append(value);
            return GenResult<u32>::generated(run, value);
        }
        // Not live => Recorded.
        auto next = run.next();
        if (next.has_value()) {
            u32 value = next.value();
            return GenResult<u32>::generated(run, value);
        }
        return GenResult<u32>::rejected("Ran out of recorded bits"sv);
    };
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

   Shrinks towards the smaller of the argument.
 */
inline Generator<u32> unsigned_int(u32 min, u32 max)
{
    if (min > max) {
        return unsigned_int(max, min);
    }
    if (min == max) {
        return constant(min);
    }
    u32 range = max - min;
    return unsigned_int(range).map([min](u32 x) { return x + min; });
}

} // namespace Gen
