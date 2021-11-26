/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String HorizontalRule::render_to_html(bool) const
{
    return "<hr />\n";
}

String HorizontalRule::render_for_terminal(size_t view_width) const
{
    StringBuilder builder(view_width + 1);
    for (size_t i = 0; i < view_width; ++i)
        builder.append('-');
    builder.append("\n\n");
    return builder.to_string();
}

RecursionDecision HorizontalRule::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;
    // Normalize return value.
    return RecursionDecision::Continue;
}

OwnPtr<HorizontalRule> HorizontalRule::parse(LineIterator& lines)
{
    if (lines.is_end())
        return {};

    StringView line = *lines;

    if (line.length() < 3)
        return {};
    if (!line.starts_with('-') && !line.starts_with('_') && !line.starts_with('*'))
        return {};

    auto first_character = line.characters_without_null_termination()[0];
    for (auto ch : line) {
        if (ch != first_character)
            return {};
    }

    ++lines;
    return make<HorizontalRule>();
}

}
