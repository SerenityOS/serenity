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
#include <LibMarkdown/MDHeading.h>

String MDHeading::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<h%d>", m_level);
    builder.append(m_text.render_to_html());
    builder.appendf("</h%d>\n", m_level);
    return builder.build();
}

String MDHeading::render_for_terminal() const
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

bool MDHeading::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    const StringView& line = *lines;

    for (m_level = 0; m_level < (int)line.length(); m_level++)
        if (line[(size_t)m_level] != '#')
            break;

    if (m_level >= (int)line.length() || line[(size_t)m_level] != ' ')
        return false;

    StringView title_view = line.substring_view((size_t)m_level + 1, line.length() - (size_t)m_level - 1);
    bool success = m_text.parse(title_view);
    ASSERT(success);

    ++lines;
    return true;
}
