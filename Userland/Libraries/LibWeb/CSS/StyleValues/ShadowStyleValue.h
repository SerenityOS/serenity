/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

enum class ShadowPlacement {
    Outer,
    Inner,
};

class ShadowStyleValue final : public StyleValueWithDefaultOperators<ShadowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ShadowStyleValue> create(Color color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
    {
        return adopt_ref(*new ShadowStyleValue(color, offset_x, offset_y, blur_radius, spread_distance, placement));
    }
    virtual ~ShadowStyleValue() override = default;

    Color color() const { return m_properties.color; }
    Length const& offset_x() const { return m_properties.offset_x; }
    Length const& offset_y() const { return m_properties.offset_y; }
    Length const& blur_radius() const { return m_properties.blur_radius; }
    Length const& spread_distance() const { return m_properties.spread_distance; }
    ShadowPlacement placement() const { return m_properties.placement; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(ShadowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    explicit ShadowStyleValue(Color color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
        : StyleValueWithDefaultOperators(Type::Shadow)
        , m_properties { .color = color, .offset_x = offset_x, .offset_y = offset_y, .blur_radius = blur_radius, .spread_distance = spread_distance, .placement = placement }
    {
    }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const override;

    struct Properties {
        Color color;
        Length offset_x;
        Length offset_y;
        Length blur_radius;
        Length spread_distance;
        ShadowPlacement placement;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
