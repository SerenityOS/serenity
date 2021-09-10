/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class CodeBlock final : public Block {
public:
    CodeBlock(String const& language, String const& style, String const& code)
        : m_code(move(code))
        , m_language(language)
        , m_style(style)
    {
    }
    virtual ~CodeBlock() override { }

    virtual String render_to_html(bool tight = false) const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;
    static OwnPtr<CodeBlock> parse(LineIterator& lines);

private:
    String m_code;
    String m_language;
    String m_style;
};

}
