/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>

namespace Markdown {

class ContainerBlock final : public Block {
public:
    ContainerBlock(NonnullOwnPtrVector<Block> blocks, bool has_blank_lines, bool has_trailing_blank_lines)
        : m_blocks(move(blocks))
        , m_has_blank_lines(has_blank_lines)
        , m_has_trailing_blank_lines(has_trailing_blank_lines)
    {
    }

    virtual ~ContainerBlock() override { }

    virtual String render_to_html(bool tight = false) const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;

    static OwnPtr<ContainerBlock> parse(LineIterator& lines);

    bool has_blank_lines() const { return m_has_blank_lines; }
    bool has_trailing_blank_lines() const { return m_has_trailing_blank_lines; }

    NonnullOwnPtrVector<Block> const& blocks() const { return m_blocks; }

private:
    NonnullOwnPtrVector<Block> m_blocks;
    bool m_has_blank_lines;
    bool m_has_trailing_blank_lines;
};

}
