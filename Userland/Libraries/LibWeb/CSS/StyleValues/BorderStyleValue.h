/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class BorderStyleValue final : public StyleValueWithDefaultOperators<BorderStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> border_width,
        ValueComparingNonnullRefPtr<StyleValue> border_style,
        ValueComparingNonnullRefPtr<StyleValue> border_color)
    {
        return adopt_ref(*new (nothrow) BorderStyleValue(move(border_width), move(border_style), move(border_color)));
    }
    virtual ~BorderStyleValue() override;

    ValueComparingNonnullRefPtr<StyleValue> border_width() const { return m_properties.border_width; }
    ValueComparingNonnullRefPtr<StyleValue> border_style() const { return m_properties.border_style; }
    ValueComparingNonnullRefPtr<StyleValue> border_color() const { return m_properties.border_color; }

    virtual String to_string() const override;

    bool properties_equal(BorderStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> border_width,
        ValueComparingNonnullRefPtr<StyleValue> border_style,
        ValueComparingNonnullRefPtr<StyleValue> border_color);

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> border_width;
        ValueComparingNonnullRefPtr<StyleValue> border_style;
        ValueComparingNonnullRefPtr<StyleValue> border_color;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
