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

template<typename Container, typename ValueType>
constexpr bool any_of(
    SimpleIterator<Container, ValueType> const& begin,
    SimpleIterator<Container, ValueType> const& end,
    auto const& predicate)
{
    return find_if(begin, end, predicate) != end;
}

template<IterableContainer Container>
constexpr bool any_of(Container&& container, auto const& predicate)
{
    for (auto&& entry : container) {
        if (predicate(entry))
            return true;
    }
    return false;
}

}

using AK::any_of;
