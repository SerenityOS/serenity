/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMarkdown/BlockQuote.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/ContainerBlock.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String ContainerBlock::render_to_html(bool tight) const
{
    StringBuilder builder;

    for (size_t i = 0; i + 1 < m_blocks.size(); ++i) {
        auto s = m_blocks[i].render_to_html(tight);
        builder.append(s);
    }

    // I don't like this edge case.
    if (m_blocks.size() != 0) {
        auto& block = m_blocks[m_blocks.size() - 1];
        auto s = block.render_to_html(tight);
        if (tight && dynamic_cast<Paragraph const*>(&block)) {
            builder.append(s.substring_view(0, s.length() - 1));
        } else {
            builder.append(s);
        }
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

RecursionDecision ContainerBlock::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    for (auto const& block : m_blocks) {
        rd = block.walk(visitor);
        if (rd == RecursionDecision::Break)
            return rd;
    }

    return RecursionDecision::Continue;
}

template<typename BlockType>
static bool try_parse_block(LineIterator& lines, NonnullOwnPtrVector<Block>& blocks)
{
    OwnPtr<BlockType> block = BlockType::parse(lines);
    if (!block)
        return false;
    blocks.append(block.release_nonnull());
    return true;
}

OwnPtr<ContainerBlock> ContainerBlock::parse(LineIterator& lines)
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

    bool has_blank_lines = false;
    bool has_trailing_blank_lines = false;

    while (true) {
        if (lines.is_end())
            break;

        if ((*lines).is_empty()) {
            has_trailing_blank_lines = true;
            ++lines;

            flush_paragraph();
            continue;
        } else {
            has_blank_lines = has_blank_lines || has_trailing_blank_lines;
        }

        bool any = try_parse_block<Table>(lines, blocks)
            || try_parse_block<List>(lines, blocks)
            || try_parse_block<CodeBlock>(lines, blocks)
            || try_parse_block<CommentBlock>(lines, blocks)
            || try_parse_block<Heading>(lines, blocks)
            || try_parse_block<HorizontalRule>(lines, blocks)
            || try_parse_block<BlockQuote>(lines, blocks);

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

    return make<ContainerBlock>(move(blocks), has_blank_lines, has_trailing_blank_lines);
}

}
