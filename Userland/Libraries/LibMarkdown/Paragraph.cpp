/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/TextAlignment.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String Paragraph::render_to_html(bool tight) const
{
    StringBuilder builder;

    if (!tight)
        builder.append("<p>");

    builder.append(m_text.render_to_html());

    if (!tight)
        builder.append("</p>");

    builder.append('\n');

    return builder.build();
}

String Paragraph::render_for_terminal(size_t width) const
{
    StringBuilder builder;
    builder.append(TextAlignment::justify(m_text.render_for_terminal(), width - 1));
    builder.append("\n\n");
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
