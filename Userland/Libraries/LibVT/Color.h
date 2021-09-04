/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace VT {

class Color {
public:
    enum class ANSIColor : u16 {
        Black = 0,
        Red,
        Green,
        Yellow,
        Blue,
        Magenta,
        Cyan,
        White,
        BrightBlack,
        BrightRed,
        BrightGreen,
        BrightYellow,
        BrightBlue,
        BrightMagenta,
        BrightCyan,
        BrightWhite,
        // We use the values above to directly index into the color lookup table,
        // but the ones below are handled separately.
        DefaultForeground = 256,
        DefaultBackground
    };

    static constexpr Color rgb(u32 rgb)
    {
        return Color(rgb);
    }

    static constexpr Color indexed(u8 index)
    {
        return Color(index);
    }

    static constexpr Color named(ANSIColor name)
    {
        return Color(name);
    }

    constexpr bool is_rgb() const
    {
        return m_kind == Kind::RGB;
    }

    constexpr bool is_indexed() const
    {
        return m_kind == Kind::Indexed;
    }

    constexpr bool is_named() const
    {
        return m_kind == Kind::Named;
    }

    constexpr u32 as_rgb() const
    {
        VERIFY(is_rgb());
        return m_value.as_rgb;
    }

    constexpr u8 as_indexed() const
    {
        VERIFY(is_indexed());
        return m_value.as_indexed;
    }

    constexpr ANSIColor as_named() const
    {
        VERIFY(is_named());
        return m_value.as_named;
    }

    constexpr Color to_bright() const
    {
        if (is_named()) {
            auto numeric_value = static_cast<u16>(as_named());
            if (numeric_value < 8)
                return Color::named(static_cast<ANSIColor>(numeric_value + 8));
            return *this;
        } else {
            return *this;
        }
    }

    constexpr bool operator==(Color const& other) const
    {
        if (m_kind != other.kind())
            return false;

        switch (m_kind) {
        case RGB:
            return m_value.as_rgb == other.as_rgb();
        case Indexed:
            return m_value.as_indexed == other.as_indexed();
        case Named:
            return m_value.as_named == other.as_named();
        default:
            VERIFY_NOT_REACHED();
        };
    }

    enum Kind {
        RGB,
        Indexed,
        Named
    };

    constexpr Kind kind() const
    {
        return m_kind;
    }

private:
    Kind m_kind;

    union {
        u32 as_rgb;
        u8 as_indexed;
        ANSIColor as_named;
    } m_value;

    constexpr Color(u32 rgb)
        : m_kind(Kind::RGB)
    {
        m_value.as_rgb = rgb;
    }

    constexpr Color(u8 index)
        : m_kind(Kind::Indexed)
    {
        m_value.as_indexed = index;
    }

    constexpr Color(ANSIColor name)
        : m_kind(Kind::Named)
    {
        m_value.as_named = name;
    }
};
}
