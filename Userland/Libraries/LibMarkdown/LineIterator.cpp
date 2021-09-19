/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMarkdown/LineIterator.h>

namespace Markdown {

bool LineIterator::is_indented(StringView const& line) const
{
    if (line.is_whitespace())
        return true;

    if (line.length() < m_indent)
        return false;

    for (size_t i = 0; i < m_indent; ++i) {
        if (line[i] != ' ')
            return false;
    }

    return true;
}

bool LineIterator::is_end() const
{
    return m_iterator.is_end() || (!m_ignore_prefix_mode && !is_indented(*m_iterator));
}

StringView LineIterator::operator*() const
{
    VERIFY(m_ignore_prefix_mode || is_indented(*m_iterator));
    if (m_iterator->is_whitespace())
        return *m_iterator;

    return m_iterator->substring_view(m_indent);
}

}
