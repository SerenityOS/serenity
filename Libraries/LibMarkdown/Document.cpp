/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

String Document::render_to_html() const
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head></head>\n");
    builder.append("<body>\n");

    for (auto& block : m_blocks) {
        auto s = block.render_to_html();
        builder.append(s);
    }

    builder.append("</body>\n");
    builder.append("</html>\n");
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

    while (true) {
        if (lines.is_end())
            break;

        if ((*lines).is_empty()) {
            ++lines;
            continue;
        }

        bool any = helper<Table>(lines, blocks) || helper<List>(lines, blocks) || helper<CodeBlock>(lines, blocks)
            || helper<Heading>(lines, blocks);

        if (any) {
            if (!paragraph_lines.is_empty()) {
                auto last_block = document->m_blocks.take_last();
                auto paragraph = make<Paragraph>(move(paragraph_lines));
                document->m_blocks.append(move(paragraph));
                document->m_blocks.append(move(last_block));
                paragraph_lines.clear();
            }
            continue;
        }

        auto line = Paragraph::Line::parse(lines);
        if (!line)
            return nullptr;

        paragraph_lines.append(line.release_nonnull());
    }

    if (!paragraph_lines.is_empty()) {
        auto paragraph = make<Paragraph>(move(paragraph_lines));
        document->m_blocks.append(move(paragraph));
    }

    return document;
}

}
