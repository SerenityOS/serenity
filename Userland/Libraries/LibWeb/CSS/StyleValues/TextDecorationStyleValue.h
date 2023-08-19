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

class TextDecorationStyleValue final : public StyleValueWithDefaultOperators<TextDecorationStyleValue> {
public:
    static ValueComparingNonnullRefPtr<TextDecorationStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> line,
        ValueComparingNonnullRefPtr<StyleValue> thickness,
        ValueComparingNonnullRefPtr<StyleValue> style,
        ValueComparingNonnullRefPtr<StyleValue> color)
    {
        return adopt_ref(*new (nothrow) TextDecorationStyleValue(move(line), move(thickness), move(style), move(color)));
    }
    virtual ~TextDecorationStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> line() const { return m_properties.line; }
    ValueComparingNonnullRefPtr<StyleValue> thickness() const { return m_properties.thickness; }
    ValueComparingNonnullRefPtr<StyleValue> style() const { return m_properties.style; }
    ValueComparingNonnullRefPtr<StyleValue> color() const { return m_properties.color; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(TextDecorationStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    TextDecorationStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> line,
        ValueComparingNonnullRefPtr<StyleValue> thickness,
        ValueComparingNonnullRefPtr<StyleValue> style,
        ValueComparingNonnullRefPtr<StyleValue> color)
        : StyleValueWithDefaultOperators(Type::TextDecoration)
        , m_properties { .line = move(line), .thickness = move(thickness), .style = move(style), .color = move(color) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> line;
        ValueComparingNonnullRefPtr<StyleValue> thickness;
        ValueComparingNonnullRefPtr<StyleValue> style;
        ValueComparingNonnullRefPtr<StyleValue> color;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
