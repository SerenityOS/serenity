/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BorderStyleValue.h"

namespace Web::CSS {

BorderStyleValue::BorderStyleValue(
    ValueComparingNonnullRefPtr<StyleValue> border_width,
    ValueComparingNonnullRefPtr<StyleValue> border_style,
    ValueComparingNonnullRefPtr<StyleValue> border_color)
    : StyleValueWithDefaultOperators(Type::Border)
    , m_properties { .border_width = move(border_width), .border_style = move(border_style), .border_color = move(border_color) }
{
}

BorderStyleValue::~BorderStyleValue() = default;

String BorderStyleValue::to_string() const
{
    return MUST(String::formatted("{} {} {}", m_properties.border_width->to_string(), m_properties.border_style->to_string(), m_properties.border_color->to_string()));
}

}
