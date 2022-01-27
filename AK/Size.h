/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>

namespace AK {

namespace Detail {

// clang-format off
template<typename T>
concept SizeQueryable = requires
{
    {
        declval<AddConst<T>>().size()
    } -> Unsigned;
};
// clang-format on

}

constexpr size_t size(auto begin, auto const& end) requires IteratorPairWith<decltype(begin), decltype(end)>
{
    size_t size = 0;
    while (begin != end) {
        ++begin;
        ++size;
    }
    return size;
}

template<typename T, size_t N>
constexpr size_t size(T (&)[N])
{
    return N;
}

constexpr auto size(auto const& container) requires IterableContainer<decltype(container)> || Detail::SizeQueryable<decltype(container)>
{
    if constexpr (Detail::SizeQueryable<decltype(container)>)
        return container.size();
    if constexpr (IterableContainer<decltype(container)>)
        return size(container.begin(), container.end());
    VERIFY_NOT_REACHED();
}

}

using AK::size;
