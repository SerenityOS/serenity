/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceSelfStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceSelfStyleValue::to_string() const
{
    return String::formatted("{} {}", TRY(m_properties.align_self->to_string()), TRY(m_properties.justify_self->to_string()));
}

}
