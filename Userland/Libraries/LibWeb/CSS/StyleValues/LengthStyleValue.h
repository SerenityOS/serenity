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

class LengthStyleValue : public StyleValueWithDefaultOperators<LengthStyleValue> {
public:
    static ValueComparingNonnullRefPtr<LengthStyleValue> create(Length const&);
    virtual ~LengthStyleValue() override = default;

    Length const& length() const { return m_length; }

    virtual bool has_auto() const override { return m_length.is_auto(); }
    virtual bool has_length() const override { return true; }
    virtual bool has_identifier() const override { return has_auto(); }
    virtual ErrorOr<String> to_string() const override { return m_length.to_string(); }
    virtual Length to_length() const override { return m_length; }
    virtual ValueID to_identifier() const override { return has_auto() ? ValueID::Auto : ValueID::Invalid; }
    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const override;

    bool properties_equal(LengthStyleValue const& other) const { return m_length == other.m_length; }

private:
    explicit LengthStyleValue(Length const& length)
        : StyleValueWithDefaultOperators(Type::Length)
        , m_length(length)
    {
    }

    Length m_length;
};

}
