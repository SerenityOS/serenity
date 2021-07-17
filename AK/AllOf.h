/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Find.h>
#include <AK/Iterator.h>

namespace AK {

template<typename Container, typename ValueType>
constexpr bool all_of(
    SimpleIterator<Container, ValueType> const& begin,
    SimpleIterator<Container, ValueType> const& end,
    auto const& predicate)
{
    constexpr auto negated_predicate = [](auto const& pred) {
        return [&](auto const& elem) { return !pred(elem); };
    };
    return find_if(begin, end, negated_predicate(predicate)) == end;
}

}

using AK::all_of;
