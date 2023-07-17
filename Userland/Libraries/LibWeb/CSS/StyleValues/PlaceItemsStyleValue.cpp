/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceItemsStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceItemsStyleValue::to_string() const
{
    return String::formatted("{} {}", TRY(m_properties.align_items->to_string()), TRY(m_properties.justify_items->to_string()));
}

}
