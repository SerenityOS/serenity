/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/Visitor.h>
#include <LibRegex/Regex.h>

namespace Markdown {

String CodeBlock::render_to_html(bool) const
{
    StringBuilder builder;

    builder.append("<pre>");

    if (m_style.length() >= 2)
        builder.append("<strong>");
    else if (m_style.length() >= 2)
        builder.append("<em>");

    if (m_language.is_empty())
        builder.append("<code>");
    else
        builder.appendff("<code class=\"language-{}\">", escape_html_entities(m_language));

    if (m_language == "js")
        builder.append(JS::MarkupGenerator::html_from_source(m_code));
    else
        builder.append(escape_html_entities(m_code));

    builder.append("\n</code>");

    if (m_style.length() >= 2)
        builder.append("</strong>");
    else if (m_style.length() >= 2)
        builder.append("</em>");

    builder.append("</pre>\n");

    return builder.build();
}

String CodeBlock::render_for_terminal(size_t) const
{
    StringBuilder builder;

    for (auto line : m_code.split('\n')) {
        builder.append("  ");
        builder.append(line);
        builder.append("\n");
    }

    return builder.build();
}

RecursionDecision CodeBlock::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    rd = visitor.visit(m_code);
    if (rd != RecursionDecision::Recurse)
        return rd;

    // Don't recurse on m_language and m_style.

    // Normalize return value.
    return RecursionDecision::Continue;
}

static Regex<ECMA262> style_spec_re("\\s*([\\*_]*)\\s*([^\\*_\\s]*).*");

OwnPtr<CodeBlock> CodeBlock::parse(LineIterator& lines)
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
    auto matches = style_spec_re.match(style_spec);
    auto style = matches.capture_group_matches[0][0].view.string_view();
    auto language = matches.capture_group_matches[0][1].view.string_view();

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

    return make<CodeBlock>(language, style, builder.build());
}

}
