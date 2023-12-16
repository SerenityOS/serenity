/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/Visitor.h>
#include <LibRegex/Regex.h>

namespace Markdown {

ByteString HorizontalRule::render_to_html(bool) const
{
    return "<hr />\n";
}

Vector<ByteString> HorizontalRule::render_lines_for_terminal(size_t view_width) const
{
    StringBuilder builder(view_width + 1);
    for (size_t i = 0; i < view_width; ++i)
        builder.append('-');
    builder.append("\n\n"sv);
    return Vector<ByteString> { builder.to_byte_string() };
}

RecursionDecision HorizontalRule::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;
    // Normalize return value.
    return RecursionDecision::Continue;
}

static Regex<ECMA262> thematic_break_re("^ {0,3}([\\*\\-_])\\s*(\\1\\s*){2,}$");

OwnPtr<HorizontalRule> HorizontalRule::parse(LineIterator& lines)
{
    if (lines.is_end())
        return {};

    StringView line = *lines;

    auto match = thematic_break_re.match(line);
    if (!match.success)
        return {};

    ++lines;
    return make<HorizontalRule>();
}

}
