/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String Heading::render_to_html(bool) const
{
    return String::formatted("<h{}>{}</h{}>\n", m_level, m_text.render_to_html(), m_level);
}

String Heading::render_for_terminal(size_t) const
{
    StringBuilder builder;

    builder.append("\033[0;31;1m\n");
    switch (m_level) {
    case 1:
    case 2:
        builder.append(m_text.render_for_terminal().to_uppercase());
        builder.append("\033[0m\n");
        break;
    default:
        builder.append(m_text.render_for_terminal());
        builder.append("\033[0m\n");
        break;
    }

    return builder.build();
}

RecursionDecision Heading::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_text.walk(visitor);
}

OwnPtr<Heading> Heading::parse(LineIterator& lines)
{
    if (lines.is_end())
        return {};

    StringView line = *lines;
    size_t level;

    for (level = 0; level < line.length(); level++) {
        if (line[level] != '#')
            break;
    }

    if (!level || level >= line.length() || line[level] != ' ')
        return {};

    StringView title_view = line.substring_view(level + 1, line.length() - level - 1);
    auto text = Text::parse(title_view);
    auto heading = make<Heading>(move(text), level);

    ++lines;
    return heading;
}

}
