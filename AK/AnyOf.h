/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Iterator.h>

namespace AK {

template<typename Container, typename ValueType>
constexpr bool any_of(
    const SimpleIterator<Container, ValueType>& begin,
    const SimpleIterator<Container, ValueType>& end,
    const auto& predicate)
{
    for (auto iter = begin; iter != end; ++iter) {
        if (predicate(*iter)) {
            return true;
        }
    }
    return false;
}

}

using AK::any_of;
