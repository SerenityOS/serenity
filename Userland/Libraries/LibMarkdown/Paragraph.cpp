/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

DeprecatedString Paragraph::render_to_html(bool tight) const
{
    StringBuilder builder;

    if (!tight)
        builder.append("<p>"sv);

    builder.append(m_text.render_to_html());

    if (!tight)
        builder.append("</p>"sv);

    builder.append('\n');

    return builder.build();
}

DeprecatedString Paragraph::render_for_terminal(size_t) const
{
    StringBuilder builder;
    builder.append("  "sv);
    builder.append(m_text.render_for_terminal());
    builder.append("\n\n"sv);
    return builder.build();
}

RecursionDecision Paragraph::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_text.walk(visitor);
}

}
