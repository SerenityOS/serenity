/*
 * Copyright (c) 2026, Eduardo Casadei <educasadei@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Format.h>
#include <AK/Optional.h>

namespace AK {

class ANSIStyle {
public:
    enum Color : u8 {
        Black = 30,
        Red = 31,
        Green = 32,
        Yellow = 33,
        Blue = 34,
        Magenta = 35,
        Cyan = 36,
        White = 37,
        Default = 39,
        BrightBlack = 90,
        BrightRed = 91,
        BrightGreen = 92,
        BrightYellow = 93,
        BrightBlue = 94,
        BrightMagenta = 95,
        BrightCyan = 96,
        BrightWhite = 97,
    };

    enum Style : u8 {
        None = 0,
        Reset = 1 << 0,
        Bold = 1 << 1,
        Italic = 1 << 2,
        Underline = 1 << 3,
        Blink = 1 << 4,
        Negative = 1 << 5,
        Concealed = 1 << 6
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(Style);

    constexpr ANSIStyle() = default;

    constexpr ANSIStyle operator|(ANSIStyle other) const
    {
        ANSIStyle result = *this;
        if (other.m_foreground.has_value())
            result.m_foreground = other.m_foreground;
        if (other.m_background.has_value())
            result.m_background = other.m_background;

        result.m_style |= other.m_style;

        return result;
    }

    bool has_style() const
    {
        if (m_style != Style::None)
            return true;
        if (m_foreground.has_value())
            return true;
        if (m_background.has_value())
            return true;

        return false;
    }

    void apply(StringBuilder& file) const;
    void apply(FILE* file) const;

private:
    Optional<Color> m_foreground = {};
    Optional<Color> m_background = {};
    Style m_style = Style::None;

    friend constexpr ANSIStyle foreground(Color);
    friend constexpr ANSIStyle background(Color);
    friend constexpr ANSIStyle style(Style);
    friend struct AK::Formatter<ANSIStyle>;
};

constexpr ANSIStyle foreground(ANSIStyle::Color value)
{
    ANSIStyle ansi_style;
    ansi_style.m_foreground = value;
    return ansi_style;
}

constexpr ANSIStyle background(ANSIStyle::Color value)
{
    ANSIStyle ansi_style;
    ansi_style.m_background = value;
    return ansi_style;
}

constexpr ANSIStyle style(ANSIStyle::Style value)
{
    ANSIStyle ansi_style;
    ansi_style.m_style = value;
    return ansi_style;
}

template<>
struct Formatter<ANSIStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, ANSIStyle value);
};

void set_color_enabled(bool);

void vout(FILE*, ANSIStyle ansi_style, StringView fmtstr, TypeErasedFormatParams&, bool newline = false);

template<typename... Parameters>
void out(FILE* file, ANSIStyle ansi_style, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vout(file, ansi_style, fmtstr.view(), variadic_format_params);
}

template<typename... Parameters>
void outln(FILE* file, ANSIStyle ansi_style, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vout(file, ansi_style, fmtstr.view(), variadic_format_params, true);
}

template<typename... Parameters>
void out(ANSIStyle ansi_style, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    out(stdout, ansi_style, move(fmtstr), parameters...);
}

template<typename... Parameters>
void outln(ANSIStyle ansi_style, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    outln(stdout, ansi_style, move(fmtstr), parameters...);
}
}

#if USING_AK_GLOBALLY
using AK::ANSIStyle;
using AK::out;
using AK::outln;
#endif
