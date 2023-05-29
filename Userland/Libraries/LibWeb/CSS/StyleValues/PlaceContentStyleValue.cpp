/*
 * Copyright (c) 2023, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceContentStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceContentStyleValue::to_string() const
{
    return String::formatted("{} {}", TRY(m_properties.align_content->to_string()), TRY(m_properties.justify_content->to_string()));
}

}
