/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceItemsStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceItemsStyleValue::to_string() const
{
    auto align_items = TRY(m_properties.align_items->to_string());
    auto justify_items = TRY(m_properties.justify_items->to_string());
    if (align_items == justify_items)
        return String::formatted("{}", align_items);
    return String::formatted("{} {}", align_items, justify_items);
}

}
