/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BorderRadiusStyleValue.h"

namespace Web::CSS {

ErrorOr<String> BorderRadiusStyleValue::to_string() const
{
    if (m_properties.horizontal_radius == m_properties.vertical_radius)
        return m_properties.horizontal_radius.to_string();
    return String::formatted("{} / {}", TRY(m_properties.horizontal_radius.to_string()), TRY(m_properties.vertical_radius.to_string()));
}

ValueComparingNonnullRefPtr<StyleValue const> BorderRadiusStyleValue::absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const
{
    if (m_properties.horizontal_radius.is_percentage() && m_properties.vertical_radius.is_percentage())
        return *this;
    auto absolutized_horizontal_radius = m_properties.horizontal_radius;
    auto absolutized_vertical_radius = m_properties.vertical_radius;
    if (!m_properties.horizontal_radius.is_percentage())
        absolutized_horizontal_radius = m_properties.horizontal_radius.length().absolutized(viewport_rect, font_metrics, font_size, root_font_size, line_height, root_line_height);
    if (!m_properties.vertical_radius.is_percentage())
        absolutized_vertical_radius = m_properties.vertical_radius.length().absolutized(viewport_rect, font_metrics, font_size, root_font_size, line_height, root_line_height);
    return BorderRadiusStyleValue::create(absolutized_horizontal_radius, absolutized_vertical_radius);
}

}
