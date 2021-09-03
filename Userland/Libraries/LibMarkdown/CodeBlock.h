/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/Text.h>

namespace Markdown {

class CodeBlock final : public Block {
public:
    CodeBlock(Text&& style_spec, const String& code)
        : m_code(move(code))
        , m_style_spec(move(style_spec))
    {
    }
    virtual ~CodeBlock() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    static OwnPtr<CodeBlock> parse(Vector<StringView>::ConstIterator& lines);

private:
    String style_language() const;
    Text::Style style() const;

    String m_code;
    Text m_style_spec;
};

}
