/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/PixelUnits.h>

namespace Web::CSS {

class Length {
public:
    enum class Type {
        // Font-relative
        Em,
        Rem,
        Ex,
        Ch,
        Lh,
        Rlh,

        // Viewport-relative
        Vw,
        Vh,
        Vmin,
        Vmax,

        // Absolute
        Cm,
        Mm,
        Q,
        In,
        Pt,
        Pc,
        Px,

        // FIXME: Remove auto somehow
        Auto,
    };

    struct FontMetrics {
        FontMetrics(CSSPixels font_size, Gfx::FontPixelMetrics const&, CSSPixels line_height);

        CSSPixels font_size;
        CSSPixels x_height;
        CSSPixels zero_advance;
        CSSPixels line_height;
    };

    static Optional<Type> unit_from_name(StringView);

    Length(int value, Type type);
    Length(float value, Type type);
    ~Length();

    static Length make_auto();
    static Length make_px(CSSPixels value);
    Length percentage_of(Percentage const&) const;

    Length resolved(Layout::Node const& layout_node) const;

    bool is_auto() const { return m_type == Type::Auto; }
    bool is_px() const { return m_type == Type::Px; }

    bool is_absolute() const
    {
        return m_type == Type::Cm
            || m_type == Type::Mm
            || m_type == Type::Q
            || m_type == Type::In
            || m_type == Type::Pt
            || m_type == Type::Pc
            || m_type == Type::Px;
    }

    bool is_font_relative() const
    {
        return m_type == Type::Em
            || m_type == Type::Rem
            || m_type == Type::Ex
            || m_type == Type::Ch
            || m_type == Type::Lh
            || m_type == Type::Rlh;
    }

    bool is_viewport_relative() const
    {
        return m_type == Type::Vw
            || m_type == Type::Vh
            || m_type == Type::Vmin
            || m_type == Type::Vmax;
    }

    bool is_relative() const
    {
        return is_font_relative() || is_viewport_relative();
    }

    Type type() const { return m_type; }
    float raw_value() const { return m_value; }

    CSSPixels to_px(Layout::Node const&) const;

    ALWAYS_INLINE CSSPixels to_px(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const
    {
        if (is_auto())
            return 0;
        if (is_relative())
            return relative_length_to_px(viewport_rect, font_metrics, root_font_metrics);
        return absolute_length_to_px();
    }

    ALWAYS_INLINE CSSPixels absolute_length_to_px() const
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

    ErrorOr<String> to_string() const;

    bool operator==(Length const& other) const
    {
        return m_type == other.m_type && m_value == other.m_value;
    }

    CSSPixels relative_length_to_px(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const;

    // Returns empty optional if it's already absolute.
    Optional<Length> absolutize(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const;
    Length absolutized(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const;

private:
    char const* unit_name() const;

    Type m_type;
    float m_value { 0 };
};

}

template<>
struct AK::Formatter<Web::CSS::Length> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Length const& length)
    {
        return Formatter<StringView>::format(builder, TRY(length.to_string()));
    }
};
