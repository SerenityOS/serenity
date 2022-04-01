/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Length {
public:
    enum class Type {
        Calculated,
        Auto,
        Cm,
        In,
        Mm,
        Q,
        Px,
        Pt,
        Pc,
        Ex,
        Em,
        Ch,
        Rem,
        Vh,
        Vw,
        Vmax,
        Vmin,
    };

    static Optional<Type> unit_from_name(StringView);

    // We have a RefPtr<CalculatedStyleValue> member, but can't include the header StyleValue.h as it includes
    // this file already. To break the cyclic dependency, we must move all method definitions out.
    Length(int value, Type type);
    Length(float value, Type type);

    static Length make_auto();
    static Length make_px(float value);
    static Length make_calculated(NonnullRefPtr<CalculatedStyleValue>);
    Length percentage_of(Percentage const&) const;

    Length resolved(Layout::Node const& layout_node) const;

    bool is_auto() const { return m_type == Type::Auto; }
    bool is_calculated() const { return m_type == Type::Calculated; }
    bool is_px() const { return m_type == Type::Px; }

    bool is_absolute() const
    {
        return m_type == Type::Cm
            || m_type == Type::In
            || m_type == Type::Mm
            || m_type == Type::Px
            || m_type == Type::Pt
            || m_type == Type::Pc
            || m_type == Type::Q;
    }

    bool is_relative() const
    {
        return m_type == Type::Ex
            || m_type == Type::Em
            || m_type == Type::Ch
            || m_type == Type::Rem
            || m_type == Type::Vh
            || m_type == Type::Vw
            || m_type == Type::Vmax
            || m_type == Type::Vmin;
    }

    float raw_value() const { return m_value; }

    float to_px(Layout::Node const&) const;

    ALWAYS_INLINE float to_px(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const
    {
        if (is_auto())
            return 0;
        if (is_relative())
            return relative_length_to_px(viewport_rect, font_metrics, font_size, root_font_size);
        if (is_calculated())
            VERIFY_NOT_REACHED(); // We can't resolve a calculated length from here. :^(
        return absolute_length_to_px();
    }

    ALWAYS_INLINE float absolute_length_to_px() const
    {
        constexpr float inch_pixels = 96.0f;
        constexpr float centimeter_pixels = (inch_pixels / 2.54f);
        switch (m_type) {
        case Type::Cm:
            return m_value * centimeter_pixels; // 1cm = 96px/2.54
        case Type::In:
            return m_value * inch_pixels; // 1in = 2.54 cm = 96px
        case Type::Px:
            return m_value; // 1px = 1/96th of 1in
        case Type::Pt:
            return m_value * ((1.0f / 72.0f) * inch_pixels); // 1pt = 1/72th of 1in
        case Type::Pc:
            return m_value * ((1.0f / 6.0f) * inch_pixels); // 1pc = 1/6th of 1in
        case Type::Mm:
            return m_value * ((1.0f / 10.0f) * centimeter_pixels); // 1mm = 1/10th of 1cm
        case Type::Q:
            return m_value * ((1.0f / 40.0f) * centimeter_pixels); // 1Q = 1/40th of 1cm
        default:
            VERIFY_NOT_REACHED();
        }
    }

    String to_string() const;

    bool operator==(Length const& other) const
    {
        if (is_calculated())
            return m_calculated_style == other.m_calculated_style;
        return m_type == other.m_type && m_value == other.m_value;
    }

    bool operator!=(Length const& other) const
    {
        return !(*this == other);
    }

    float relative_length_to_px(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const;

private:
    char const* unit_name() const;

    Type m_type;
    float m_value { 0 };

    RefPtr<CalculatedStyleValue> m_calculated_style;
};

}

template<>
struct AK::Formatter<Web::CSS::Length> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Length const& length)
    {
        return Formatter<StringView>::format(builder, length.to_string());
    }
};
