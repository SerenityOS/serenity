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

class FontStyleValue final : public StyleValueWithDefaultOperators<FontStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FontStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> font_stretch,
        ValueComparingNonnullRefPtr<StyleValue> font_style,
        ValueComparingNonnullRefPtr<StyleValue> font_weight,
        ValueComparingNonnullRefPtr<StyleValue> font_size,
        ValueComparingNonnullRefPtr<StyleValue> line_height,
        ValueComparingNonnullRefPtr<StyleValue> font_families)
    {
        return adopt_ref(*new (nothrow) FontStyleValue(move(font_stretch), move(font_style), move(font_weight), move(font_size), move(line_height), move(font_families)));
    }
    virtual ~FontStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> font_stretch() const { return m_properties.font_stretch; }
    ValueComparingNonnullRefPtr<StyleValue> font_style() const { return m_properties.font_style; }
    ValueComparingNonnullRefPtr<StyleValue> font_weight() const { return m_properties.font_weight; }
    ValueComparingNonnullRefPtr<StyleValue> font_size() const { return m_properties.font_size; }
    ValueComparingNonnullRefPtr<StyleValue> line_height() const { return m_properties.line_height; }
    ValueComparingNonnullRefPtr<StyleValue> font_families() const { return m_properties.font_families; }

    virtual String to_string() const override;

    bool properties_equal(FontStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    FontStyleValue(ValueComparingNonnullRefPtr<StyleValue> font_stretch, ValueComparingNonnullRefPtr<StyleValue> font_style, ValueComparingNonnullRefPtr<StyleValue> font_weight, ValueComparingNonnullRefPtr<StyleValue> font_size, ValueComparingNonnullRefPtr<StyleValue> line_height, ValueComparingNonnullRefPtr<StyleValue> font_families)
        : StyleValueWithDefaultOperators(Type::Font)
        , m_properties { .font_stretch = move(font_stretch), .font_style = move(font_style), .font_weight = move(font_weight), .font_size = move(font_size), .line_height = move(line_height), .font_families = move(font_families) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> font_stretch;
        ValueComparingNonnullRefPtr<StyleValue> font_style;
        ValueComparingNonnullRefPtr<StyleValue> font_weight;
        ValueComparingNonnullRefPtr<StyleValue> font_size;
        ValueComparingNonnullRefPtr<StyleValue> line_height;
        ValueComparingNonnullRefPtr<StyleValue> font_families;
        // FIXME: Implement font-variant.
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
