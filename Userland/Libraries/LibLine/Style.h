/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <stdlib.h>

namespace Line {

class Style {
public:
    bool operator==(const Style&) const = default;

    enum class XtermColor : int {
        Default = 9,
        Black = 0,
        Red,
        Green,
        Yellow,
        Blue,
        Magenta,
        Cyan,
        White,
        Unchanged,
    };

    struct AnchoredTag {
    };
    struct UnderlineTag {
    };
    struct BoldTag {
    };
    struct ItalicTag {
    };
    struct Color {
        bool operator==(const Color&) const = default;

        explicit Color(XtermColor color)
            : m_xterm_color(color)
            , m_is_rgb(false)
        {
        }
        Color(u8 r, u8 g, u8 b)
            : m_rgb_color({ r, g, b })
            , m_is_rgb(true)
        {
        }

        bool is_default() const
        {
            return !m_is_rgb && m_xterm_color == XtermColor::Unchanged;
        }

        XtermColor m_xterm_color { XtermColor::Unchanged };
        Vector<int, 3> m_rgb_color;
        bool m_is_rgb { false };
    };

    struct Background : public Color {
        explicit Background(XtermColor color)
            : Color(color)
        {
        }
        Background(u8 r, u8 g, u8 b)
            : Color(r, g, b)
        {
        }
        String to_vt_escape() const;
    };

    struct Foreground : public Color {
        explicit Foreground(XtermColor color)
            : Color(color)
        {
        }
        Foreground(u8 r, u8 g, u8 b)
            : Color(r, g, b)
        {
        }

        String to_vt_escape() const;
    };

    struct Hyperlink {
        bool operator==(const Hyperlink&) const = default;

        explicit Hyperlink(StringView link)
            : m_link(link)
        {
            m_has_link = true;
        }

        Hyperlink() { }

        String to_vt_escape(bool starting) const;

        bool is_empty() const { return !m_has_link; }

        String m_link;
        bool m_has_link { false };
    };

    static constexpr UnderlineTag Underline {};
    static constexpr BoldTag Bold {};
    static constexpr ItalicTag Italic {};
    static constexpr AnchoredTag Anchored {};

    // Prepare for the horror of templates.
    template<typename T, typename... Rest>
    Style(const T& style_arg, Rest... rest)
        : Style(rest...)
    {
        set(style_arg);
        m_is_empty = false;
    }
    Style() { }

    static Style reset_style()
    {
        return { Foreground(XtermColor::Default), Background(XtermColor::Default), Hyperlink("") };
    }

    Style unified_with(const Style& other, bool prefer_other = true) const
    {
        Style style = *this;
        style.unify_with(other, prefer_other);
        return style;
    }

    void unify_with(const Style&, bool prefer_other = false);

    bool underline() const { return m_underline; }
    bool bold() const { return m_bold; }
    bool italic() const { return m_italic; }
    Background background() const { return m_background; }
    Foreground foreground() const { return m_foreground; }
    Hyperlink hyperlink() const { return m_hyperlink; }

    void set(const ItalicTag&) { m_italic = true; }
    void set(const BoldTag&) { m_bold = true; }
    void set(const UnderlineTag&) { m_underline = true; }
    void set(const Background& bg) { m_background = bg; }
    void set(const Foreground& fg) { m_foreground = fg; }
    void set(const Hyperlink& link) { m_hyperlink = link; }
    void set(const AnchoredTag&) { m_is_anchored = true; }

    bool is_anchored() const { return m_is_anchored; }
    bool is_empty() const { return m_is_empty; }

    String to_string() const;

private:
    bool m_underline { false };
    bool m_bold { false };
    bool m_italic { false };
    Background m_background { XtermColor::Unchanged };
    Foreground m_foreground { XtermColor::Unchanged };
    Hyperlink m_hyperlink;

    bool m_is_anchored { false };

    bool m_is_empty { true };
};
}
