/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <YAK/StringView.h>
#include <YAK/Vector.h>
#include <LibMarkdown/Block.h>
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
    virtual ~Heading() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    static OwnPtr<Heading> parse(Vector<StringView>::ConstIterator& lines);

private:
    Text m_text;
    size_t m_level { 0 };
};

}
