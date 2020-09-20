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
#include <LibMarkdown/Heading.h>

namespace Markdown {

String Heading::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<h%d>", m_level);
    builder.append(m_text.render_to_html());
    builder.appendf("</h%d>\n", m_level);
    return builder.build();
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
        return nullptr;

    const StringView& line = *lines;
    size_t level;

    for (level = 0; level < line.length(); level++)
        if (line[level] != '#')
            break;

    if (level >= line.length() || line[level] != ' ')
        return nullptr;

    StringView title_view = line.substring_view(level + 1, line.length() - level - 1);
    auto text = Text::parse(title_view);
    if (!text.has_value())
        return nullptr;

    auto heading = make<Heading>(move(text.value()), level);

    ++lines;
    return heading;
}

}
