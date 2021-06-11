/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Find.h>
#include <AK/Iterator.h>

namespace AK {

template<typename Container, typename ValueType>
constexpr bool any_of(
    const SimpleIterator<Container, ValueType>& begin,
    const SimpleIterator<Container, ValueType>& end,
    const auto& predicate)
{
    return find_if(begin, end, predicate) != end;
}

}

using AK::any_of;
