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

String FontStyleValue::to_string() const
{
    return MUST(String::formatted("{} {} {} / {} {}", m_properties.font_style->to_string(), m_properties.font_weight->to_string(), m_properties.font_size->to_string(), m_properties.line_height->to_string(), m_properties.font_families->to_string()));
}

}
