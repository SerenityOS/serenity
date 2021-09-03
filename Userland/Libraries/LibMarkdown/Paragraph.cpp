/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringBuilder.h>
#include <LibMarkdown/Paragraph.h>

namespace Markdown {

String Paragraph::render_to_html() const
{
    StringBuilder builder;
    builder.append("<p>");
    bool first = true;
    for (auto& line : m_lines) {
        if (!first)
            builder.append('\n');
        first = false;
        builder.append(line.text().render_to_html().trim(" \t"));
    }
    builder.append("</p>\n");
    return builder.build();
}

String Paragraph::render_for_terminal(size_t) const
{
    StringBuilder builder;
    bool first = true;
    for (auto& line : m_lines) {
        if (!first)
            builder.append(' ');
        first = false;
        builder.append(line.text().render_for_terminal());
    }
    builder.append("\n\n");
    return builder.build();
}

OwnPtr<Paragraph::Line> Paragraph::Line::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return {};

    auto text = Text::parse(*lines++);
    if (!text.has_value())
        return {};

    return make<Paragraph::Line>(text.release_value());
}
}
