/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/AnyOf.h>
#include <AK/Array.h>
#include <AK/Vector.h>

TEST_CASE(should_determine_if_predicate_applies_to_any_element_in_container)
{
    constexpr Array<int, 10> a { 1 };

    static_assert(any_of(a.begin(), a.end(), [](auto elem) { return elem == 0; }));
    static_assert(any_of(a.begin(), a.end(), [](auto elem) { return elem == 1; }));
    static_assert(!any_of(a.begin(), a.end(), [](auto elem) { return elem == 2; }));

    EXPECT(any_of(a.begin(), a.end(), [](auto elem) { return elem == 0; }));
    EXPECT(any_of(a.begin(), a.end(), [](auto elem) { return elem == 1; }));
    EXPECT(!any_of(a.begin(), a.end(), [](auto elem) { return elem == 2; }));
}

TEST_CASE(container_form)
{
    constexpr Array a { 10, 20, 30 };
    static_assert(any_of(a, [](auto elem) { return elem == 10; }));
    static_assert(any_of(a, [](auto elem) { return elem == 20; }));
    static_assert(!any_of(a, [](auto elem) { return elem == 40; }));

    EXPECT(any_of(a, [](auto elem) { return elem == 10; }));
    EXPECT(any_of(a, [](auto elem) { return elem == 20; }));
    EXPECT(!any_of(a, [](auto elem) { return elem == 40; }));

    Vector b { 10, 20, 30 };
    EXPECT(any_of(b, [](auto elem) { return elem > 10; }));
    EXPECT(!any_of(b, [](auto elem) { return elem > 40; }));

    struct ArbitraryIterable {
        struct ArbitraryIterator {
            ArbitraryIterator(int v)
                : value(v)
            {
            }

            bool operator==(ArbitraryIterator const&) const = default;
            int operator*() const { return value; }
            ArbitraryIterator& operator++()
            {
                ++value;
                return *this;
            }

            int value;
        };
        ArbitraryIterator begin() const { return 0; }
        ArbitraryIterator end() const { return 20; }
    };

    ArbitraryIterable c;
    EXPECT(any_of(c, [](auto elem) { return elem < 20; }));
    EXPECT(!any_of(c, [](auto elem) { return elem > 31; }));
}
