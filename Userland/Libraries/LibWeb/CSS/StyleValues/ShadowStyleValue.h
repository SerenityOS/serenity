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
    static ValueComparingNonnullRefPtr<ShadowStyleValue> create(
        Color color,
        ValueComparingNonnullRefPtr<StyleValue const> offset_x,
        ValueComparingNonnullRefPtr<StyleValue const> offset_y,
        ValueComparingNonnullRefPtr<StyleValue const> blur_radius,
        ValueComparingNonnullRefPtr<StyleValue const> spread_distance,
        ShadowPlacement placement)
    {
        return adopt_ref(*new (nothrow) ShadowStyleValue(color, move(offset_x), move(offset_y), move(blur_radius), move(spread_distance), placement));
    }
    virtual ~ShadowStyleValue() override = default;

    Color color() const { return m_properties.color; }
    ValueComparingNonnullRefPtr<StyleValue const> const& offset_x() const { return m_properties.offset_x; }
    ValueComparingNonnullRefPtr<StyleValue const> const& offset_y() const { return m_properties.offset_y; }
    ValueComparingNonnullRefPtr<StyleValue const> const& blur_radius() const { return m_properties.blur_radius; }
    ValueComparingNonnullRefPtr<StyleValue const> const& spread_distance() const { return m_properties.spread_distance; }
    ShadowPlacement placement() const { return m_properties.placement; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(ShadowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    ShadowStyleValue(
        Color color,
        ValueComparingNonnullRefPtr<StyleValue const> offset_x,
        ValueComparingNonnullRefPtr<StyleValue const> offset_y,
        ValueComparingNonnullRefPtr<StyleValue const> blur_radius,
        ValueComparingNonnullRefPtr<StyleValue const> spread_distance,
        ShadowPlacement placement)
        : StyleValueWithDefaultOperators(Type::Shadow)
        , m_properties {
            .color = color,
            .offset_x = move(offset_x),
            .offset_y = move(offset_y),
            .blur_radius = move(blur_radius),
            .spread_distance = move(spread_distance),
            .placement = placement
        }
    {
    }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Length::FontMetrics const& font_metrics, Length::FontMetrics const& root_font_metrics) const override;

    struct Properties {
        Color color;
        ValueComparingNonnullRefPtr<StyleValue const> offset_x;
        ValueComparingNonnullRefPtr<StyleValue const> offset_y;
        ValueComparingNonnullRefPtr<StyleValue const> blur_radius;
        ValueComparingNonnullRefPtr<StyleValue const> spread_distance;
        ShadowPlacement placement;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
