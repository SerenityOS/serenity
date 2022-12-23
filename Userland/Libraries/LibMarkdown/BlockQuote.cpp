/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibMarkdown/BlockQuote.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

DeprecatedString BlockQuote::render_to_html(bool) const
{
    StringBuilder builder;
    builder.append("<blockquote>\n"sv);
    builder.append(m_contents->render_to_html());
    builder.append("</blockquote>\n"sv);
    return builder.build();
}

Vector<DeprecatedString> BlockQuote::render_lines_for_terminal(size_t view_width) const
{
    // FIXME: Indent lines inside the blockquote
    return m_contents->render_lines_for_terminal(view_width);
}

RecursionDecision BlockQuote::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_contents->walk(visitor);
}

OwnPtr<BlockQuote> BlockQuote::parse(LineIterator& lines)
{
    lines.push_context(LineIterator::Context::block_quote());
    if (lines.is_end()) {
        lines.pop_context();
        return {};
    }

    auto contents = ContainerBlock::parse(lines);
    lines.pop_context();

    if (!contents)
        return {};

    return make<BlockQuote>(move(contents));
}

}
