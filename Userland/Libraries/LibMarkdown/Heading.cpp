/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/Heading.h>

namespace Markdown {

String Heading::render_to_html() const
{
    return String::formatted("<h{}>{}</h{}>\n", m_level, m_text.render_to_html(), m_level);
}

String Heading::render_for_terminal(size_t) const
{
    StringBuilder builder;

    switch (m_level) {
    case 1:
    case 2:
        builder.append("\n\033[1m");
        builder.append(m_text.render_for_terminal().to_uppercase());
        builder.append("\033[0m\n");
        break;
    default:
        builder.append("\n\033[1m");
        builder.append(m_text.render_for_terminal());
        builder.append("\033[0m\n");
        break;
    }

    return builder.build();
}

OwnPtr<Heading> Heading::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return {};

    const StringView& line = *lines;
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
