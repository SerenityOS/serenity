/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Forward.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String Paragraph::render_to_html(bool tight) const
{
    StringBuilder builder;

    if (!tight)
        builder.append("<p>"sv);

    builder.append(m_text.render_to_html());

    if (!tight)
        builder.append("</p>"sv);

    builder.append('\n');

    return builder.to_string().release_value_but_fixme_should_propagate_errors();
}

Vector<String> Paragraph::render_lines_for_terminal(size_t) const
{
    return Vector<String> { String::formatted("  {}", m_text.render_for_terminal()).release_value_but_fixme_should_propagate_errors(), String::from_utf8_short_string(""sv) };
}

RecursionDecision Paragraph::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_text.walk(visitor);
}

}
