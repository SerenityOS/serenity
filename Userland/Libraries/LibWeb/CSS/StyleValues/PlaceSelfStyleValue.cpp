/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceSelfStyleValue.h"

namespace Web::CSS {

String PlaceSelfStyleValue::to_string() const
{
    auto align_self = m_properties.align_self->to_string();
    auto justify_self = m_properties.justify_self->to_string();
    if (align_self == justify_self)
        return MUST(String::formatted("{}", align_self));
    return MUST(String::formatted("{} {}", align_self, justify_self));
}

}
