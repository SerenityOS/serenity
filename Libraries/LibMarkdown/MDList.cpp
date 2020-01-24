/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/MDList.h>

String MDList::render_to_html() const
{
    StringBuilder builder;

    const char* tag = m_is_ordered ? "ol" : "ul";
    builder.appendf("<%s>", tag);

    for (auto& item : m_items) {
        builder.append("<li>");
        builder.append(item.render_to_html());
        builder.append("</li>\n");
    }

    builder.appendf("</%s>\n", tag);

    return builder.build();
}

String MDList::render_for_terminal() const
{
    StringBuilder builder;

    int i = 0;
    for (auto& item : m_items) {
        builder.append("  ");
        if (m_is_ordered)
            builder.appendf("%d. ", ++i);
        else
            builder.append("* ");
        builder.append(item.render_for_terminal());
        builder.append("\n");
    }
    builder.append("\n");

    return builder.build();
}

bool MDList::parse(Vector<StringView>::ConstIterator& lines)
{
    bool first = true;
    while (true) {
        if (lines.is_end())
            break;
        const StringView& line = *lines;
        if (line.is_empty())
            break;

        bool appears_unordered = false;
        size_t offset = 0;
        if (line.length() > 2)
            if (line[1] == ' ' && (line[0] == '*' || line[0] == '-')) {
                appears_unordered = true;
                offset = 2;
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

        ASSERT(!(appears_unordered && appears_ordered));
        if (!appears_unordered && !appears_ordered)
            return false;
        if (first)
            m_is_ordered = appears_ordered;
        else if (m_is_ordered != appears_ordered)
            return false;

        first = false;
        MDText text;
        bool success = text.parse(line.substring_view(offset, line.length() - offset));
        ASSERT(success);
        m_items.append(move(text));
        ++lines;
    }

    return !first;
}
