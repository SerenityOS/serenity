/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/OwnPtr.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>

namespace Markdown {

class ContainerBlock final : public Block {
public:
    ContainerBlock(Vector<NonnullOwnPtr<Block>> blocks, bool has_blank_lines, bool has_trailing_blank_lines)
        : m_blocks(move(blocks))
        , m_has_blank_lines(has_blank_lines)
        , m_has_trailing_blank_lines(has_trailing_blank_lines)
    {
    }

    virtual ~ContainerBlock() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;

    static OwnPtr<ContainerBlock> parse(LineIterator& lines);

    bool has_blank_lines() const { return m_has_blank_lines; }
    bool has_trailing_blank_lines() const { return m_has_trailing_blank_lines; }

    Vector<NonnullOwnPtr<Block>> const& blocks() const { return m_blocks; }

private:
    Vector<NonnullOwnPtr<Block>> m_blocks;
    bool m_has_blank_lines;
    bool m_has_trailing_blank_lines;
};

}
