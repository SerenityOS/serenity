/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class Paragraph final : public Block {
public:
    class Line {
    public:
        explicit Line(Text&& text)
            : m_text(move(text))
        {
        }

        static OwnPtr<Line> parse(Vector<StringView>::ConstIterator& lines);
        Text const& text() const { return m_text; }

    private:
        Text m_text;
    };

    Paragraph(NonnullOwnPtrVector<Line>&& lines)
        : m_lines(move(lines))
    {
    }

    virtual ~Paragraph() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;

private:
    NonnullOwnPtrVector<Line> m_lines;
};

}
