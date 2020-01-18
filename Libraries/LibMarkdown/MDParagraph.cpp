/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibMarkdown/MDParagraph.h>

String MDParagraph::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<p>");
    builder.append(m_text.render_to_html());
    builder.appendf("</p>\n");
    return builder.build();
}

String MDParagraph::render_for_terminal() const
{
    StringBuilder builder;
    builder.append(m_text.render_for_terminal());
    builder.appendf("\n\n");
    return builder.build();
}

bool MDParagraph::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    bool first = true;
    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        StringView line = *lines;
        if (line.is_empty())
            break;
        char ch = line[0];
        // See if it looks like a blockquote
        // or like an indented block.
        if (ch == '>' || ch == ' ')
            break;
        if (line.length() > 1) {
            // See if it looks like a heading.
            if (ch == '#' && (line[1] == '#' || line[1] == ' '))
                break;
            // See if it looks like a code block.
            if (ch == '`' && line[1] == '`')
                break;
            // See if it looks like a list.
            if (ch == '*' || ch == '-')
                if (line[1] == ' ')
                    break;
        }

        if (!first)
            builder.append(' ');
        builder.append(line);
        first = false;
        ++lines;
    }

    if (first)
        return false;

    bool success = m_text.parse(builder.build());
    ASSERT(success);
    return true;
}
