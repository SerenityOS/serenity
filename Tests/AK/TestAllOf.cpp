/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/AllOf.h>
#include <AK/Array.h>

TEST_CASE(should_determine_if_predicate_applies_to_all_elements_in_container)
{
    constexpr Array<int, 10> a {};

    static_assert(all_of(a.begin(), a.end(), [](auto elem) { return elem == 0; }));
    static_assert(!all_of(a.begin(), a.end(), [](auto elem) { return elem == 1; }));

    EXPECT(all_of(a.begin(), a.end(), [](auto elem) { return elem == 0; }));
    EXPECT(!all_of(a.begin(), a.end(), [](auto elem) { return elem == 1; }));
}
