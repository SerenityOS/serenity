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
    size_t indent = 0;

    // Allow for up to 3 spaces of indentation.
    // https://spec.commonmark.org/0.30/#example-68
    for (size_t i = 0; i < 3; ++i) {
        if (line[i] != ' ')
            break;

        ++indent;
    }

    size_t level;

    for (level = 0; indent + level < line.length(); level++) {
        if (line[indent + level] != '#')
            break;
    }

    if (!level || indent + level >= line.length() || line[indent + level] != ' ' || level > 6)
        return {};

    StringView title_view = line.substring_view(indent + level + 1);
    auto text = Text::parse(title_view);
    auto heading = make<Heading>(move(text), level);

    ++lines;
    return heading;
}

}
