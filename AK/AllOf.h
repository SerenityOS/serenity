/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Iterator.h>

namespace AK {

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator>
constexpr bool all_of(
    TIterator const& begin,
    TEndIterator const& end,
    auto const& predicate)
{
    for (auto iter = begin; iter != end; ++iter) {
        if (!predicate(*iter)) {
            return false;
        }
    }
    return true;
}

template<IterableContainer Container>
constexpr bool all_of(Container&& container, auto const& predicate)
{
    for (auto&& entry : container) {
        if (!predicate(entry))
            return false;
    }
    return true;
}

}

using AK::all_of;
