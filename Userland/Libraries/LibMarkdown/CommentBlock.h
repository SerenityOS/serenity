/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>

namespace Markdown {

class CommentBlock final : public Block {
public:
    CommentBlock(DeprecatedString const& comment)
        : m_comment(comment)
    {
    }
    virtual ~CommentBlock() override = default;

    virtual DeprecatedString render_to_html(bool tight = false) const override;
    virtual Vector<DeprecatedString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;
    static OwnPtr<CommentBlock> parse(LineIterator& lines);

private:
    DeprecatedString m_comment;
};

}
