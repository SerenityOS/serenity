/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <LibMarkdown/CodeBlock.h>

namespace Markdown {

Text::Style CodeBlock::style() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].style;
}

String CodeBlock::style_language() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].text;
}

String CodeBlock::render_to_html() const
{
    StringBuilder builder;

    String style_language = this->style_language();
    Text::Style style = this->style();

    if (style.strong)
        builder.append("<b>");
    if (style.emph)
        builder.append("<i>");

    if (style_language.is_empty())
        builder.append("<code>");
    else
        builder.appendff("<code class=\"{}\">", escape_html_entities(style_language));

    if (style_language == "js")
        builder.append(JS::MarkupGenerator::html_from_source(m_code));
    else
        builder.append(escape_html_entities(m_code));

    builder.append("</code>");

    if (style.emph)
        builder.append("</i>");
    if (style.strong)
        builder.append("</b>");

    builder.append('\n');

    return builder.build();
}

String CodeBlock::render_for_terminal(size_t) const
{
    StringBuilder builder;

    Text::Style style = this->style();
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

OwnPtr<CodeBlock> CodeBlock::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return {};

    constexpr auto tick_tick_tick = "```";

    StringView line = *lines;
    if (!line.starts_with(tick_tick_tick))
        return {};

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
    auto spec = Text::parse(style_spec);
    if (!spec.has_value())
        return {};

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

    return make<CodeBlock>(move(spec.value()), builder.build());
}

}
