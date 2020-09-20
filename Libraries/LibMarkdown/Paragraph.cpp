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
#include <LibMarkdown/Paragraph.h>

namespace Markdown {

String Paragraph::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<p>");
    bool first = true;
    for (auto& line : m_lines) {
        if (!first)
            builder.append(' ');
        first = false;
        builder.append(line.text().render_to_html());
    }
    builder.appendf("</p>\n");
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
    builder.appendf("\n\n");
    return builder.build();
}

OwnPtr<Paragraph::Line> Paragraph::Line::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return nullptr;

    auto text = Text::parse(*lines++);
    if (!text.has_value())
        return nullptr;

    return make<Paragraph::Line>(text.release_value());
}
}
