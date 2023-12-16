/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class Heading final : public Block {
public:
    Heading(Text&& text, size_t level)
        : m_text(move(text))
        , m_level(level)
    {
        VERIFY(m_level > 0);
    }
    virtual ~Heading() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;
    static OwnPtr<Heading> parse(LineIterator& lines);

private:
    Text m_text;
    size_t m_level { 0 };
};

}
