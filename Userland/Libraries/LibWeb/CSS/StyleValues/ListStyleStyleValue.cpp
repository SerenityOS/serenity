/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ListStyleStyleValue.h"

namespace Web::CSS {

ErrorOr<String> ListStyleStyleValue::to_string() const
{
    return String::formatted("{} {} {}", TRY(m_properties.position->to_string()), TRY(m_properties.image->to_string()), TRY(m_properties.style_type->to_string()));
}

}
