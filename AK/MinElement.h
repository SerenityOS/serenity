/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Iterator.h>

namespace AK {

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator>
constexpr auto min_element(
    TIterator const& begin,
    TEndIterator const& end,
    auto const& comp)
{
    if (begin == end) {
        return end;
    }

    TIterator smallest = begin;
    for (TIterator cur = begin + 1; cur != end; ++cur) {
        if (comp(*cur, *smallest)) {
            smallest = cur;
        }
    }
    return smallest;
}

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator>
constexpr auto min_element(
    TIterator const& begin,
    TEndIterator const& end)
{
    constexpr auto less = [](auto const& a, auto const& b) {
        return a < b;
    };
    return min_element(begin, end, less);
}

template<IterableContainer Container>
constexpr auto min_element(Container&& container, auto const& comp)
{
    return min_element(container.begin(), container.end(), comp);
}

template<IterableContainer Container>
constexpr auto min_element(Container&& container)
{
    return min_element(container.begin(), container.end());
}

}

using AK::min_element;
