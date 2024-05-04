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

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator, typename V>
[[nodiscard]] constexpr TIterator find(TIterator first, TEndIterator last, V const& value)
{
    return find_if(first, last, [&]<typename T>(T const& entry) { return Traits<T>::equals(entry, value); });
}

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator, typename V>
[[nodiscard]] constexpr size_t find_index(TIterator first, TEndIterator last, V const& value)
requires(requires(TIterator it) { it.index(); })
{
    return find_if(first, last, [&]<typename T>(T const& entry) { return Traits<T>::equals(entry, value); }).index();
}

}

#if USING_AK_GLOBALLY
using AK::find;
using AK::find_if;
using AK::find_index;
#endif
