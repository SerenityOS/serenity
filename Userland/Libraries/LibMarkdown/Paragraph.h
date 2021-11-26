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
    Paragraph(Text text)
        : m_text(move(text))
    {
    }

    virtual ~Paragraph() override { }

    virtual String render_to_html(bool tight = false) const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;

private:
    Text m_text;
};

}
