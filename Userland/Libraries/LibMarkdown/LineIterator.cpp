/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMarkdown/LineIterator.h>

namespace Markdown {

void LineIterator::reset_ignore_prefix()
{
    for (auto& context : m_context_stack) {
        context.ignore_prefix = false;
    }
}

Optional<StringView> LineIterator::match_context(StringView line) const
{
    bool is_ws = line.is_whitespace();
    size_t offset = 0;
    for (auto& context : m_context_stack) {
        switch (context.type) {
        case Context::Type::ListItem:
            if (is_ws)
                break;

            if (offset + context.indent > line.length())
                return {};

            if (!context.ignore_prefix && !line.substring_view(offset, context.indent).is_whitespace())
                return {};

            offset += context.indent;

            break;
        case Context::Type::BlockQuote:
            for (; offset < line.length() && line[offset] == ' '; ++offset) { }
            if (offset >= line.length() || line[offset] != '>') {
                return {};
            }
            ++offset;
            break;
        }

        if (offset > line.length())
            return {};
    }
    return line.substring_view(offset);
}

bool LineIterator::is_end() const
{
    return m_iterator.is_end() || !match_context(*m_iterator).has_value();
}

StringView LineIterator::operator*() const
{
    auto line = match_context(*m_iterator);
    VERIFY(line.has_value());
    return line.value();
}

}
