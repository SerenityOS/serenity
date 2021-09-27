/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/List.h>

namespace Markdown {

String List::render_to_html() const
{
    StringBuilder builder;

    const char* tag = m_is_ordered ? "ol" : "ul";
    builder.appendff("<{}>\n", tag);

    for (auto& item : m_items) {
        builder.append("<li>\n");
        builder.append(item->render_to_html());
        builder.append("</li>\n");
    }

    builder.appendff("</{}>\n", tag);

    return builder.build();
}

String List::render_for_terminal(size_t) const
{
    StringBuilder builder;

    int i = 0;
    for (auto& item : m_items) {
        builder.append("  ");
        if (m_is_ordered)
            builder.appendff("{}. ", ++i);
        else
            builder.append("* ");
        builder.append(item->render_for_terminal());
        builder.append("\n");
    }
    builder.append("\n");

    return builder.build();
}

OwnPtr<List> List::parse(LineIterator& lines)
{
    Vector<OwnPtr<ContainerBlock>> items;

    bool first = true;
    bool is_ordered = false;
    while (!lines.is_end()) {
        size_t offset = 0;

        const StringView& line = *lines;
        if (line.is_empty())
            break;

        bool appears_unordered = false;
        if (line.length() > 2) {
            if (line[1] == ' ' && (line[0] == '*' || line[0] == '-')) {
                appears_unordered = true;
                offset = 1;
            }
        }

        bool appears_ordered = false;
        for (size_t i = 0; i < 10 && i < line.length(); i++) {
            char ch = line[i];
            if ('0' <= ch && ch <= '9')
                continue;
            if (ch == '.' || ch == ')')
                if (i + 1 < line.length() && line[i + 1] == ' ') {
                    appears_ordered = true;
                    offset = i + 1;
                }
            break;
        }

        VERIFY(!(appears_unordered && appears_ordered));
        if (!appears_unordered && !appears_ordered) {
            if (first)
                return {};

            break;
        }

        while (offset < line.length() && line[offset] == ' ')
            offset++;

        if (first) {
            is_ordered = appears_ordered;
        } else if (appears_ordered != is_ordered) {
            break;
        }

        size_t saved_indent = lines.indent();
        lines.set_indent(saved_indent + offset);
        lines.ignore_next_prefix();

        items.append(ContainerBlock::parse(lines));

        lines.set_indent(saved_indent);

        first = false;
    }

    return make<List>(move(items), is_ordered);
}

}
