/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontStyleValue.h"

namespace Web::CSS {

ErrorOr<String> FontStyleValue::to_string() const
{
    return String::formatted("{} {} {} / {} {}", TRY(m_properties.font_style->to_string()), TRY(m_properties.font_weight->to_string()), TRY(m_properties.font_size->to_string()), TRY(m_properties.line_height->to_string()), TRY(m_properties.font_families->to_string()));
}

}
