/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class BorderRadiusStyleValue final : public StyleValueWithDefaultOperators<BorderRadiusStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderRadiusStyleValue> create(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
    {
        return adopt_ref(*new BorderRadiusStyleValue(horizontal_radius, vertical_radius));
    }
    virtual ~BorderRadiusStyleValue() override = default;

    LengthPercentage const& horizontal_radius() const { return m_properties.horizontal_radius; }
    LengthPercentage const& vertical_radius() const { return m_properties.vertical_radius; }
    bool is_elliptical() const { return m_properties.is_elliptical; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BorderRadiusStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderRadiusStyleValue(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
        : StyleValueWithDefaultOperators(Type::BorderRadius)
        , m_properties { .is_elliptical = horizontal_radius != vertical_radius, .horizontal_radius = horizontal_radius, .vertical_radius = vertical_radius }
    {
    }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const override;

    struct Properties {
        bool is_elliptical;
        LengthPercentage horizontal_radius;
        LengthPercentage vertical_radius;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
