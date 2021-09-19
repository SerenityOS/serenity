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
    ContainerBlock(NonnullOwnPtrVector<Block> blocks)
        : m_blocks(move(blocks))
    {
    }

    virtual ~ContainerBlock() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;

    static OwnPtr<ContainerBlock> parse(LineIterator& lines);

private:
    NonnullOwnPtrVector<Block> m_blocks;
};

}
