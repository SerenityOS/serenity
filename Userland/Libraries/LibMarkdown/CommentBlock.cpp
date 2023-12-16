/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Forward.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/CommentBlock.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

ByteString CommentBlock::render_to_html(bool) const
{
    StringBuilder builder;

    builder.append("<!--"sv);
    builder.append(escape_html_entities(m_comment));
    // TODO: This is probably incorrect, because we technically need to escape "--" in some form. However, Browser does not care about this.
    builder.append("-->\n"sv);

    return builder.to_byte_string();
}

Vector<ByteString> CommentBlock::render_lines_for_terminal(size_t) const
{
    return Vector<ByteString> {};
}

RecursionDecision CommentBlock::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    // Normalize return value.
    return RecursionDecision::Continue;
}

OwnPtr<CommentBlock> CommentBlock::parse(LineIterator& lines)
{
    if (lines.is_end())
        return {};

    constexpr auto comment_start = "<!--"sv;
    constexpr auto comment_end = "-->"sv;

    StringView line = *lines;
    if (!line.starts_with(comment_start))
        return {};
    line = line.substring_view(comment_start.length());

    StringBuilder builder;

    while (true) {
        // Invariant: At the beginning of the loop, `line` is valid and should be added to the builder.
        bool ends_here = line.ends_with(comment_end);
        if (ends_here)
            line = line.substring_view(0, line.length() - comment_end.length());
        builder.append(line);
        if (!ends_here)
            builder.append('\n');

        ++lines;
        if (lines.is_end() || ends_here) {
            break;
        }
        line = *lines;
    }

    return make<CommentBlock>(builder.to_byte_string());
}

}
