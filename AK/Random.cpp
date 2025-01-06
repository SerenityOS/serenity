/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>

namespace AK {

u32 get_random_uniform(u32 max_bounds)
{
    // If we try to divide all 2**32 numbers into groups of "max_bounds" numbers, we may end up
    // with one group that is a bit too small. For this reason, the implementation
    // `arc4random() % max_bounds` would be insufficient. Here we compute the first number of
    // the first "full group" as 2**32 % max_bounds, and rely on the equivalence
    // `2**32 % x == (2**32 - x) % x` to keep calculations in u32 domain:
    u32 const threshold = static_cast<u32>(-max_bounds) % max_bounds;
    auto random_value = get_random<u32>();
    for (int i = 0; i < 20 && random_value < threshold; ++i) {
        // By chance we picked a value from the incomplete group. Note that this group has size at
        // most 2**31-1, so picking this group has a chance of less than 50%.
        // In practice, this means that for the worst possible input, there is still only a
        // once-in-a-million chance to get to iteration 20. In theory we should be able to loop
        // forever. Here we prefer marginally imperfect random numbers over weird runtime behavior.
        random_value = get_random<u32>();
    }
    return random_value % max_bounds;
}

u64 get_random_uniform_64(u64 max_bounds)
{
    // Uses the same algorithm as `get_random_uniform`,
    // by replacing u32 with u64.
    u64 const threshold = static_cast<u64>(-max_bounds) % max_bounds;
    auto random_value = get_random<u64>();
    for (int i = 0; i < 20 && random_value < threshold; ++i) {
        random_value = get_random<u64>();
    }
    return random_value % max_bounds;
}

}
