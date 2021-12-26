/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Find.h>
#include <AK/Iterator.h>

namespace AK {

template<typename TEndIterator, IteratorPairWith<TEndIterator> TIterator>
constexpr bool any_of(
    TIterator const& begin,
    TEndIterator const& end,
    auto const& predicate)
{
    return find_if(begin, end, predicate) != end;
}

template<IterableContainer Container>
constexpr bool any_of(Container&& container, auto const& predicate)
{
    return any_of(container.begin(), container.end(), predicate);
}

}

using AK::any_of;
