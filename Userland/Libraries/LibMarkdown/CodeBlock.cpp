/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Forward.h>
#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/Visitor.h>
#include <LibRegex/Regex.h>

namespace Markdown {

ByteString CodeBlock::render_to_html(bool) const
{
    StringBuilder builder;

    builder.append("<pre>"sv);

    if (m_style.length() >= 2)
        builder.append("<strong>"sv);
    else if (m_style.length() >= 2)
        builder.append("<em>"sv);

    if (m_language.is_empty())
        builder.append("<code>"sv);
    else
        builder.appendff("<code class=\"language-{}\">", escape_html_entities(m_language));

    if (m_language == "js") {
        auto html_or_error = JS::MarkupGenerator::html_from_source(m_code);
        if (html_or_error.is_error()) {
            warnln("Could not render js code to html: {}", html_or_error.error());
            builder.append(escape_html_entities(m_code));
        } else {
            builder.append(html_or_error.release_value());
        }
    } else {
        builder.append(escape_html_entities(m_code));
    }

    builder.append("</code>"sv);

    if (m_style.length() >= 2)
        builder.append("</strong>"sv);
    else if (m_style.length() >= 2)
        builder.append("</em>"sv);

    builder.append("</pre>\n"sv);

    return builder.to_byte_string();
}

Vector<ByteString> CodeBlock::render_lines_for_terminal(size_t) const
{
    Vector<ByteString> lines;

    // Do not indent too much if we are in the synopsis
    auto indentation = "    "sv;
    if (m_current_section != nullptr) {
        auto current_section_name = m_current_section->render_lines_for_terminal()[0];
        if (current_section_name.contains("SYNOPSIS"sv))
            indentation = "  "sv;
    }

    for (auto const& line : m_code.split('\n', SplitBehavior::KeepEmpty))
        lines.append(ByteString::formatted("{}{}", indentation, line));

    return lines;
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

static Regex<ECMA262> open_fence_re("^ {0,3}(([\\`\\~])\\2{2,})\\s*([\\*_]*)\\s*([^\\*_\\s]*).*$");
static Regex<ECMA262> close_fence_re("^ {0,3}(([\\`\\~])\\2{2,})\\s*$");

static Optional<int> line_block_prefix(StringView const& line)
{
    int characters = 0;
    int indents = 0;

    for (char ch : line) {
        if (indents == 4)
            break;

        if (ch == ' ') {
            ++characters;
            ++indents;
        } else if (ch == '\t') {
            ++characters;
            indents = 4;
        } else {
            break;
        }
    }

    if (indents == 4)
        return characters;

    return {};
}

OwnPtr<CodeBlock> CodeBlock::parse(LineIterator& lines, Heading* current_section)
{
    if (lines.is_end())
        return {};

    StringView line = *lines;
    if (open_fence_re.match(line).success)
        return parse_backticks(lines, current_section);

    if (line_block_prefix(line).has_value())
        return parse_indent(lines);

    return {};
}

OwnPtr<CodeBlock> CodeBlock::parse_backticks(LineIterator& lines, Heading* current_section)
{
    StringView line = *lines;

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

    auto matches = open_fence_re.match(line).capture_group_matches[0];
    auto fence = matches[0].view.string_view();
    auto style = matches[2].view.string_view();
    auto language = matches[3].view.string_view();

    ++lines;

    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        line = *lines;
        ++lines;

        auto close_match = close_fence_re.match(line);
        if (close_match.success) {
            auto close_fence = close_match.capture_group_matches[0][0].view.string_view();
            if (close_fence[0] == fence[0] && close_fence.length() >= fence.length())
                break;
        }
        builder.append(line);
        builder.append('\n');
    }

    return make<CodeBlock>(language, style, builder.to_byte_string(), current_section);
}

OwnPtr<CodeBlock> CodeBlock::parse_indent(LineIterator& lines)
{
    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        StringView line = *lines;

        auto prefix_length = line_block_prefix(line);
        if (!prefix_length.has_value())
            break;

        line = line.substring_view(prefix_length.value());
        ++lines;

        builder.append(line);
        builder.append('\n');
    }

    return make<CodeBlock>("", "", builder.to_byte_string(), nullptr);
}
}
