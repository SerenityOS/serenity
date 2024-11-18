/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2024, Sam Atkins <sam@ladybird.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibWeb/CSS/CSSStyleValue.h>

namespace Web::CSS {

// https://drafts.css-houdini.org/css-typed-om-1/#csscolorvalue
class CSSColorValue : public CSSStyleValue {
public:
    static ValueComparingNonnullRefPtr<CSSColorValue> create_from_color(Color color);
    virtual ~CSSColorValue() override = default;

    virtual bool has_color() const override { return true; }

    enum class ColorType {
        RGB,
        HSL,
        HWB,
        Lab,
        LCH,
        OKLab,
        OKLCH,
        XYZD50,
    };
    ColorType color_type() const { return m_color_type; }

protected:
    explicit CSSColorValue(ColorType color_type)
        : CSSStyleValue(Type::Color)
        , m_color_type(color_type)
    {
    }

    static Optional<float> resolve_hue(CSSStyleValue const&);
    static Optional<float> resolve_with_reference_value(CSSStyleValue const&, float one_hundred_percent_value);
    static Optional<float> resolve_alpha(CSSStyleValue const&);

private:
    ColorType m_color_type;
};

}
