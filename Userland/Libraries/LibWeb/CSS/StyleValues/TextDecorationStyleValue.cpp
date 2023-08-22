/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextDecorationStyleValue.h"

namespace Web::CSS {

String TextDecorationStyleValue::to_string() const
{
    return MUST(String::formatted("{} {} {} {}", m_properties.line->to_string(), m_properties.thickness->to_string(), m_properties.style->to_string(), m_properties.color->to_string()));
}

}
