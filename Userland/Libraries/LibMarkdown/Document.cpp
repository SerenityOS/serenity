/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringBuilder.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

String Document::render_to_html() const
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head>\n");
    builder.append("<style>\n");
    builder.append("code { white-space: pre; }\n");
    builder.append("</style>\n");
    builder.append("</head>\n");
    builder.append("<body>\n");

    builder.append(render_to_inline_html());

    builder.append("</body>\n");
    builder.append("</html>\n");
    return builder.build();
}

String Document::render_to_inline_html() const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_to_html();
        builder.append(s);
    }

    return builder.build();
}

String Document::render_for_terminal(size_t view_width) const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_for_terminal(view_width);
        builder.append(s);
    }

    return builder.build();
}

template<typename BlockType>
static bool helper(Vector<StringView>::ConstIterator& lines, NonnullOwnPtrVector<Block>& blocks)
{
    OwnPtr<BlockType> block = BlockType::parse(lines);
    if (!block)
        return false;
    blocks.append(block.release_nonnull());
    return true;
}

OwnPtr<Document> Document::parse(const StringView& str)
{
    const Vector<StringView> lines_vec = str.lines();
    auto lines = lines_vec.begin();
    auto document = make<Document>();
    auto& blocks = document->m_blocks;
    NonnullOwnPtrVector<Paragraph::Line> paragraph_lines;

    auto flush_paragraph = [&] {
        if (paragraph_lines.is_empty())
            return;
        auto paragraph = make<Paragraph>(move(paragraph_lines));
        document->m_blocks.append(move(paragraph));
        paragraph_lines.clear();
    };
    while (true) {
        if (lines.is_end())
            break;

        if ((*lines).is_empty()) {
            ++lines;
            flush_paragraph();
            continue;
        }

        bool any = helper<Table>(lines, blocks) || helper<List>(lines, blocks) || helper<CodeBlock>(lines, blocks)
            || helper<Heading>(lines, blocks) || helper<HorizontalRule>(lines, blocks);

        if (any) {
            if (!paragraph_lines.is_empty()) {
                auto last_block = document->m_blocks.take_last();
                flush_paragraph();
                document->m_blocks.append(move(last_block));
            }
            continue;
        }

        auto line = Paragraph::Line::parse(lines);
        if (!line)
            return {};

        paragraph_lines.append(line.release_nonnull());
    }

    if (!paragraph_lines.is_empty())
        flush_paragraph();

    return document;
}

}
