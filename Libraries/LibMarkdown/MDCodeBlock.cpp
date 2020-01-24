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
#include <LibMarkdown/MDCodeBlock.h>

MDText::Style MDCodeBlock::style() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].style;
}

String MDCodeBlock::style_language() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].text;
}

String MDCodeBlock::render_to_html() const
{
    StringBuilder builder;

    String style_language = this->style_language();
    MDText::Style style = this->style();

    if (style.strong)
        builder.append("<b>");
    if (style.emph)
        builder.append("<i>");

    if (style_language.is_null())
        builder.append("<code style=\"white-space: pre;\">");
    else
        builder.appendf("<code style=\"white-space: pre;\" class=\"%s\">", style_language.characters());

    // TODO: This should also be done in other places.
    for (size_t i = 0; i < m_code.length(); i++)
        if (m_code[i] == '<')
            builder.append("&lt;");
        else if (m_code[i] == '>')
            builder.append("&gt;");
        else if (m_code[i] == '&')
            builder.append("&amp;");
        else
            builder.append(m_code[i]);

    builder.append("</code>");

    if (style.emph)
        builder.append("</i>");
    if (style.strong)
        builder.append("</b>");

    builder.append('\n');

    return builder.build();
}

String MDCodeBlock::render_for_terminal() const
{
    StringBuilder builder;

    MDText::Style style = this->style();
    bool needs_styling = style.strong || style.emph;
    if (needs_styling) {
        builder.append("\033[");
        bool first = true;
        if (style.strong) {
            builder.append('1');
            first = false;
        }
        if (style.emph) {
            if (!first)
                builder.append(';');
            builder.append('4');
        }
        builder.append('m');
    }

    builder.append(m_code);

    if (needs_styling)
        builder.append("\033[0m");

    builder.append("\n\n");

    return builder.build();
}

bool MDCodeBlock::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    constexpr auto tick_tick_tick = "```";

    StringView line = *lines;
    if (!line.starts_with(tick_tick_tick))
        return false;

    // Our Markdown extension: we allow
    // specifying a style and a language
    // for a code block, like so:
    //
    // ```**sh**
    // $ echo hello friends!
    // ````
    //
    // The code block will be made bold,
    // and if possible syntax-highlighted
    // as appropriate for a shell script.
    StringView style_spec = line.substring_view(3, line.length() - 3);
    bool success = m_style_spec.parse(style_spec);
    ASSERT(success);

    ++lines;

    bool first = true;
    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        line = *lines;
        ++lines;
        if (line == tick_tick_tick)
            break;
        if (!first)
            builder.append('\n');
        builder.append(line);
        first = false;
    }

    m_code = builder.build();
    return true;
}
