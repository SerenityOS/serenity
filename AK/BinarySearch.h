/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

struct DefaultComparator {
    template<typename T, typename S>
    [[nodiscard]] constexpr int operator()(T& lhs, S& rhs)
    {
        if (lhs > rhs)
            return 1;

        if (lhs < rhs)
            return -1;

        return 0;
    }
};

template<typename Container, typename Needle, typename Comparator = DefaultComparator>
constexpr auto binary_search(
    Container&& haystack,
    Needle&& needle,
    size_t* nearby_index = nullptr,
    Comparator comparator = Comparator {}) -> decltype(&haystack[0])
{
    if (haystack.size() == 0) {
        // FIXME: Refactor the function's API to make it always return a valid index.
        if (nearby_index)
            *nearby_index = 0;
        return nullptr;
    }

    size_t low = 0;
    size_t high = haystack.size() - 1;
    while (low != high) {
        size_t middle = low + (high - low) / 2;
        int comparison = comparator(needle, haystack[middle]);
        if (comparison <= 0)
            high = middle;
        else
            low = middle + 1;
    }

    if (nearby_index)
        *nearby_index = high;
    if (comparator(needle, haystack[high]) == 0)
        return &haystack[high];
    return nullptr;
}

// Unlike their std equivalents, these two function require the entire Container to be sorted!
// std::[lower,upper]_bound only require the array to be sorted after and before, respectively, the needle.

template<typename Container, typename Needle, typename Comparator = DefaultComparator, typename Return = decltype(&Container {}[0])>
constexpr auto lower_bound(
    Container&& haystack,
    Needle&& needle,
    Comparator comparator = {}) -> Optional<size_t>
{
    size_t index {};
    binary_search(haystack, needle, &index, comparator);
    if (index == haystack.size() - 1 && comparator(needle, haystack[index]) > 0)
        return OptionalNone {};
    return index;
}

template<typename Container, typename Needle, typename Comparator = DefaultComparator, typename Return = decltype(&Container {}[0])>
constexpr auto strict_lower_bound(
    Container&& haystack,
    Needle&& needle,
    Comparator comparator = {}) -> Optional<size_t>
{
    // lower_bound give you the "first element x such that i ≤ x".
    // This gives you the last element x such that x < i.
    size_t index {};
    binary_search(haystack, needle, &index, comparator);
    if (index > 0 && comparator(needle, haystack[index]) <= 0)
        index--;

    if (index == 0 && comparator(needle, haystack[index]) <= 0)
        return OptionalNone {};
    return index;
}

struct UpperBoundComparator {
    template<typename T, typename S>
    [[nodiscard]] constexpr int operator()(T& lhs, S& rhs)
    {
        if (lhs >= rhs)
            return 1;
        return -1;
    }
};

template<typename Container, typename Needle, typename Comparator = UpperBoundComparator, typename Return = decltype(&Container {}[0])>
requires IsIntegral<RemoveReference<Needle>>
constexpr auto upper_bound(
    Container&& haystack,
    Needle&& needle,
    Comparator comparator = {}) -> Optional<size_t>
{
    size_t index = 0;
    binary_search(haystack, needle, &index, comparator);
    if (index == haystack.size() - 1 && comparator(needle, haystack[index]) >= 0)
        return OptionalNone {};
    return index;
}

}

#if USING_AK_GLOBALLY
using AK::binary_search;
using AK::lower_bound;
using AK::strict_lower_bound;
using AK::upper_bound;
#endif
