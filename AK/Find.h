/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

template<typename TIterator, typename TUnaryPredicate>
constexpr TIterator find_if(TIterator first, TIterator last, TUnaryPredicate&& pred)
{
    for (; first != last; ++first) {
        if (pred(*first)) {
            return first;
        }
    }
    return last;
}

template<typename TIterator, typename T>
constexpr TIterator find(TIterator first, TIterator last, const T& value)
{
    return find_if(first, last, [&](const auto& v) { return Traits<T>::equals(value, v); });
}

template<typename TIterator, typename T>
constexpr size_t find_index(TIterator first, TIterator last, const T& value)
{
    return find_if(first, last, [&](const auto& v) { return Traits<T>::equals(value, v); }).index();
}

}
