/*
 * Copyright (c) 2023, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaceContentStyleValue.h"

namespace Web::CSS {

ErrorOr<String> PlaceContentStyleValue::to_string() const
{
    auto align_content = TRY(m_properties.align_content->to_string());
    auto justify_content = TRY(m_properties.justify_content->to_string());
    if (align_content == justify_content)
        return String::formatted("{}", align_content);
    return String::formatted("{} {}", align_content, justify_content);
}

}
