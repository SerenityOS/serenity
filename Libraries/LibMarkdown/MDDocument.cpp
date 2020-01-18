/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibMarkdown/MDCodeBlock.h>
#include <LibMarkdown/MDDocument.h>
#include <LibMarkdown/MDHeading.h>
#include <LibMarkdown/MDList.h>
#include <LibMarkdown/MDParagraph.h>

String MDDocument::render_to_html() const
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

String MDDocument::render_for_terminal() const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_for_terminal();
        builder.append(s);
    }

    return builder.build();
}

template<typename Block>
static bool helper(Vector<StringView>::ConstIterator& lines, NonnullOwnPtrVector<MDBlock>& blocks)
{
    NonnullOwnPtr<Block> block = make<Block>();
    bool success = block->parse(lines);
    if (!success)
        return false;
    blocks.append(move(block));
    return true;
}

bool MDDocument::parse(const StringView& str)
{
    const Vector<StringView> lines_vec = str.lines();
    auto lines = lines_vec.begin();

    while (true) {
        if (lines.is_end())
            return true;

        if ((*lines).is_empty()) {
            ++lines;
            continue;
        }

        bool any = helper<MDList>(lines, m_blocks) || helper<MDParagraph>(lines, m_blocks) || helper<MDCodeBlock>(lines, m_blocks) || helper<MDHeading>(lines, m_blocks);

        if (!any)
            return false;
    }
}
