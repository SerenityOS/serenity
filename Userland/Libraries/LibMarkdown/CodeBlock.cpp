/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <LibMarkdown/CodeBlock.h>

namespace Markdown {

String CodeBlock::render_to_html() const
{
    StringBuilder builder;

    builder.append("<pre>");

    if (m_language.is_empty())
        builder.append("<code>");
    else
        builder.appendff("<code class=\"{}\">", escape_html_entities(m_language));

    if (m_language == "js")
        builder.append(JS::MarkupGenerator::html_from_source(m_code));
    else
        builder.append(escape_html_entities(m_code));

    builder.append("\n</code>");

    builder.append("</pre>\n");

    return builder.build();
}

String CodeBlock::render_for_terminal(size_t) const
{
    StringBuilder builder;

    builder.append(m_code);
    builder.append("\n\n");

    return builder.build();
}

OwnPtr<CodeBlock> CodeBlock::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return {};

    constexpr auto tick_tick_tick = "```";

    StringView line = *lines;
    if (!line.starts_with(tick_tick_tick))
        return {};

    StringView style_spec = line.substring_view(3, line.length() - 3);

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

    return make<CodeBlock>(style_spec, builder.build());
}

}
