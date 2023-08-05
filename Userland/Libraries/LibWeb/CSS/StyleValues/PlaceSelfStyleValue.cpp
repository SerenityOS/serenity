/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceSelfStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceSelfStyleValue::to_string() const
{
    auto align_self = TRY(m_properties.align_self->to_string());
    auto justify_self = TRY(m_properties.justify_self->to_string());
    if (align_self == justify_self)
        return String::formatted("{}", align_self);
    return String::formatted("{} {}", align_self, justify_self);
}

}
