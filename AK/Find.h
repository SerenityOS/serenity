/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator, typename TUnaryPredicate>
[[nodiscard]] constexpr TIterator find_if(TIterator first, TEndIterator last, TUnaryPredicate&& pred)
{
    for (; first != last; ++first) {
        if (pred(*first)) {
            return first;
        }
    }
    return last;
}

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator, typename T>
[[nodiscard]] constexpr TIterator find(TIterator first, TEndIterator last, T const& value)
{
    // FIXME: Use the iterator's trait type, and swap arguments in equals call.
    return find_if(first, last, [&](auto const& v) { return Traits<T>::equals(value, v); });
}

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator, typename T>
[[nodiscard]] constexpr size_t find_index(TIterator first, TEndIterator last, T const& value)
requires(requires(TIterator it) { it.index(); })
{
    // FIXME: Use the iterator's trait type, and swap arguments in equals call.
    return find_if(first, last, [&](auto const& v) { return Traits<T>::equals(value, v); }).index();
}

}
