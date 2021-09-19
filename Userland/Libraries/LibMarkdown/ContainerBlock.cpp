/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/ContainerBlock.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

String ContainerBlock::render_to_html() const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_to_html();
        builder.append(s);
    }

    return builder.build();
}

String ContainerBlock::render_for_terminal(size_t view_width) const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_for_terminal(view_width);
        builder.append(s);
    }

    return builder.build();
}

template<typename BlockType>
static bool try_parse_block(Vector<StringView>::ConstIterator& lines, NonnullOwnPtrVector<Block>& blocks)
{
    OwnPtr<BlockType> block = BlockType::parse(lines);
    if (!block)
        return false;
    blocks.append(block.release_nonnull());
    return true;
}

OwnPtr<ContainerBlock> ContainerBlock::parse(Vector<StringView>::ConstIterator& lines)
{
    NonnullOwnPtrVector<Block> blocks;

    StringBuilder paragraph_text;

    auto flush_paragraph = [&] {
        if (paragraph_text.is_empty())
            return;
        auto paragraph = make<Paragraph>(Text::parse(paragraph_text.build()));
        blocks.append(move(paragraph));
        paragraph_text.clear();
    };

    while (true) {
        if (lines.is_end())
            break;

        if ((*lines).is_empty()) {
            ++lines;

            flush_paragraph();
            continue;
        }

        bool any = try_parse_block<Table>(lines, blocks) || try_parse_block<List>(lines, blocks) || try_parse_block<CodeBlock>(lines, blocks)
            || try_parse_block<Heading>(lines, blocks) || try_parse_block<HorizontalRule>(lines, blocks);

        if (any) {
            if (!paragraph_text.is_empty()) {
                auto last_block = blocks.take_last();
                flush_paragraph();
                blocks.append(move(last_block));
            }
            continue;
        }

        if (!paragraph_text.is_empty())
            paragraph_text.append("\n");
        paragraph_text.append(*lines++);
    }

    flush_paragraph();

    return make<ContainerBlock>(move(blocks));
}

}
