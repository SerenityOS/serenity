/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once
#include <stdlib.h>

namespace Line {

class Style {
public:
    enum class Color : int {
        Default = 9,
        Black = 0,
        Red,
        Green,
        Yellow,
        Blue,
        Magenta,
        Cyan,
        White,
        // TODO: it appears that we do not support these SGR options
        BrightBlack = 60,
        BrightRed,
        BrightGreen,
        BrightYellow,
        BrightBlue,
        BrightMagenta,
        BrightCyan,
        BrightWhite,
    };

    struct UnderlineTag {
    };
    struct BoldTag {
    };
    struct ItalicTag {
    };
    struct Background {
        explicit Background(Color color)
            : m_color(color)
        {
        }
        Color m_color;
    };
    struct Foreground {
        explicit Foreground(Color color)
            : m_color(color)
        {
        }
        Color m_color;
    };

    static constexpr UnderlineTag Underline {};
    static constexpr BoldTag Bold {};
    static constexpr ItalicTag Italic {};

    // prepare for the horror of templates
    template <typename T, typename... Rest>
    Style(const T& style_arg, Rest... rest)
        : Style(rest...)
    {
        set(style_arg);
    }
    Style() {}

    bool underline() const { return m_underline; }
    bool bold() const { return m_bold; }
    bool italic() const { return m_italic; }
    Color background() const { return m_background; }
    Color foreground() const { return m_foreground; }

    void set(const ItalicTag&) { m_italic = true; }
    void set(const BoldTag&) { m_bold = true; }
    void set(const UnderlineTag&) { m_underline = true; }
    void set(const Background& bg) { m_background = bg.m_color; }
    void set(const Foreground& fg) { m_foreground = fg.m_color; }

private:
    bool m_underline { false };
    bool m_bold { false };
    bool m_italic { false };
    Color m_background { Color::Default };
    Color m_foreground { Color::Default };
};
}
