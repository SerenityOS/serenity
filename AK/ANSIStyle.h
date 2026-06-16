/*
 * Copyright (c) 2026, Eduardo Casadei <educasadei@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Format.h>
#include <AK/Types.h>

namespace AK {

class ANSIStyle {
public:
    enum class Fg : u8 {
        FgNone,
        FgBlack = 30,
        FgRed = 31,
        FgGreen = 32,
        FgYellow = 33,
        FgBlue = 34,
        FgMagenta = 35,
        FgCyan = 36,
        FgWhite = 37,
        FgDefault = 39,
        FgBrightBlack = 90,
        FgBrightRed = 91,
        FgBrightGreen = 92,
        FgBrightYellow = 93,
        FgBrightBlue = 94,
        FgBrightMagenta = 95,
        FgBrightCyan = 96,
        FgBrightWhite = 97,
    };

    enum class Bg : u8 {
        BgNone,
        BgBlack = 40,
        BgRed = 41,
        BgGreen = 42,
        BgYellow = 43,
        BgBlue = 44,
        BgMagenta = 45,
        BgCyan = 46,
        BgWhite = 47,
        BgDefault = 49,
        BgBrightBlack = 100,
        BgBrightRed = 101,
        BgBrightGreen = 102,
        BgBrightYellow = 103,
        BgBrightBlue = 104,
        BgBrightMagenta = 105,
        BgBrightCyan = 106,
        BgBrightWhite = 107,
    };

    enum class Flags : u8 {
        NoAttributes = 0,
        Reset = 1 << 0,
        Bold = 1 << 1,
        Italic = 1 << 2,
        Underline = 1 << 3,
        Blink = 1 << 4,
        Negative = 1 << 5,
        Concealed = 1 << 6
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(Flags);

    using enum Fg;
    using enum Bg;
    using enum Flags;

    friend constexpr ANSIStyle operator|(Bg bg, ANSIStyle style) { return ANSIStyle(bg) | style; }
    friend constexpr ANSIStyle operator|(Fg fg, ANSIStyle style) { return ANSIStyle(fg) | style; }
    friend constexpr ANSIStyle operator|(Flags flags, ANSIStyle style) { return ANSIStyle(flags) | style; }

    constexpr ANSIStyle() = default;
    constexpr ANSIStyle(Fg fg)
        : m_fg(fg)
    {
    }
    constexpr ANSIStyle(Bg bg)
        : m_bg(bg)
    {
    }
    constexpr ANSIStyle(Flags flags)
        : m_flags(flags)
    {
    }

    constexpr ANSIStyle operator|(ANSIStyle other) const
    {
        ANSIStyle result = *this;
        if (other.m_fg != Fg::FgNone)
            result.m_fg = other.m_fg;
        if (other.m_bg != Bg::BgNone)
            result.m_bg = other.m_bg;

        result.m_flags |= other.m_flags;

        return result;
    }

    bool has_style() const
    {
        if (m_flags != Flags::NoAttributes)
            return true;
        if (m_fg != Fg::FgNone)
            return true;
        if (m_bg != Bg::BgNone)
            return true;

        return false;
    }

    static void apply(FILE*, ANSIStyle);

private:
    Fg m_fg { Fg::FgNone };
    Bg m_bg = { Bg::BgNone };
    Flags m_flags = { Flags::NoAttributes };

    friend struct AK::Formatter<ANSIStyle>;
};

template<>
struct Formatter<ANSIStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, ANSIStyle value);
};

void set_color_enabled(bool);
}

#if USING_AK_GLOBALLY
using AK::ANSIStyle;
#endif
