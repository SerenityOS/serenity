/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Slugify.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/Visitor.h>
#include <LibUnicode/Normalize.h>

namespace Markdown {

ByteString Heading::render_to_html(bool) const
{
    auto input = Unicode::normalize(m_text.render_for_raw_print(), Unicode::NormalizationForm::NFD);
    auto slugified = MUST(AK::slugify(input));
    return ByteString::formatted("<h{} id='{}'><a href='#{}'>#</a> {}</h{}>\n", m_level, slugified, slugified, m_text.render_to_html(), m_level);
}

Vector<ByteString> Heading::render_lines_for_terminal(size_t) const
{
    StringBuilder builder;

    builder.append("\n\033[0;31;1m"sv);
    switch (m_level) {
    case 1:
    case 2:
        builder.append(m_text.render_for_terminal().to_uppercase());
        builder.append("\033[0m"sv);
        break;
    default:
        builder.append(m_text.render_for_terminal());
        builder.append("\033[0m"sv);
        break;
    }

    return Vector<ByteString> { builder.to_byte_string() };
}

RecursionDecision Heading::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_text.walk(visitor);
}

OwnPtr<Heading> Heading::parse(LineIterator& lines)
{
    if (lines.is_end())
        return {};

    StringView line = *lines;
    size_t indent = 0;

    // Allow for up to 3 spaces of indentation.
    // https://spec.commonmark.org/0.30/#example-68
    for (size_t i = 0; i < 3; ++i) {
        if (line[i] != ' ')
            break;

        ++indent;
    }

    size_t level;

    for (level = 0; indent + level < line.length(); level++) {
        if (line[indent + level] != '#')
            break;
    }

    // Only the opening sequence of #s is an empty ATX heading (example 79)
    if (indent + level == line.length()) {
        ++lines;
        return make<Heading>(Text::parse(""sv), level);
    }

    // At least one space or tab is required between the # characters and the
    // heading’s contents, unless the heading is empty. (example 64)
    if (!level
        || (line[indent + level] != ' ' && line[indent + level] != '\t')
        || level > 6)
        return {};

    size_t last = line.length() - 1;
    for (; last > indent + level; --last) {
        if (line[last] != '#' && line[last] != ' ' && line[last] != '\t')
            break;
    }

    // Only whitespace between the opening and closing sequence of #s is an
    // empty ATX heading (example 79)
    if (last == indent + level) {
        ++lines;
        return make<Heading>(Text::parse(""sv), level);
    }

    // The closing sequence must be preceded by a space or tab (example 75)
    if (last != line.length() - 1 && line[last + 1] != ' ' && line[last + 1] != '\t')
        last = line.length() - 1;

    StringView title_view = line.substring_view(indent + level + 1, last - indent - level);
    auto text = Text::parse(title_view);
    auto heading = make<Heading>(move(text), level);

    ++lines;
    return heading;
}

}
