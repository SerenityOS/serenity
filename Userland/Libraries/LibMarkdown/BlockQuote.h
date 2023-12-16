/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/ContainerBlock.h>

namespace Markdown {

class BlockQuote final : public Block {
public:
    BlockQuote(OwnPtr<ContainerBlock> contents)
        : m_contents(move(contents))
    {
    }
    virtual ~BlockQuote() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;

    static OwnPtr<BlockQuote> parse(LineIterator& lines);

private:
    OwnPtr<ContainerBlock> m_contents;
};

}
