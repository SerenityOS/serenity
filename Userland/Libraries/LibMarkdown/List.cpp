/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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
        builder.append("<li>");
        builder.append(item.render_to_html());
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
        builder.append(item.render_for_terminal());
        builder.append("\n");
    }
    builder.append("\n");

    return builder.build();
}

OwnPtr<List> List::parse(Vector<StringView>::ConstIterator& lines)
{
    Vector<Text> items;
    bool is_ordered = false;

    bool first = true;
    size_t offset = 0;
    StringBuilder item_builder;
    auto flush_item_if_needed = [&] {
        if (first)
            return true;

        auto text = Text::parse(item_builder.string_view());
        items.append(move(text));

        item_builder.clear();
        return true;
    };

    while (true) {
        if (lines.is_end())
            break;
        const StringView& line = *lines;
        if (line.is_empty())
            break;

        bool appears_unordered = false;
        if (line.length() > 2) {
            if (line[1] == ' ' && (line[0] == '*' || line[0] == '-')) {
                appears_unordered = true;
                offset = 2;
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

        if (appears_unordered || appears_ordered) {
            if (first)
                is_ordered = appears_ordered;
            else if (is_ordered != appears_ordered)
                return {};

            if (!flush_item_if_needed())
                return {};

            while (offset + 1 < line.length() && line[offset + 1] == ' ')
                offset++;

        } else {
            if (first)
                return {};
            for (size_t i = 0; i < offset; i++) {
                if (line[i] != ' ')
                    return {};
            }
        }

        first = false;
        if (!item_builder.is_empty())
            item_builder.append(' ');
        VERIFY(offset <= line.length());
        item_builder.append(line.substring_view(offset, line.length() - offset));
        ++lines;
        offset = 0;
    }

    if (!flush_item_if_needed() || first)
        return {};
    return make<List>(move(items), is_ordered);
}

}
