/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Markdown {

class Text final {
    AK_MAKE_NONCOPYABLE(Text);

public:
    struct Style {
        bool emph { false };
        bool strong { false };
        bool code { false };
        String href;
        String img;
    };

    struct Span {
        String text;
        Style style;
    };

    explicit Text(String&& text);
    Text(Text&& text) = default;
    Text() = default;

    Text& operator=(Text&&) = default;

    const Vector<Span>& spans() const { return m_spans; }

    String render_to_html() const;
    String render_for_terminal() const;

    static Optional<Text> parse(StringView const&);

private:
    Text(Vector<Span>&& spans)
        : m_spans(move(spans))
    {
    }

    Vector<Span> m_spans;
};

}
