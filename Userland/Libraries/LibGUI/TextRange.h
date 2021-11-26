/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/TextPosition.h>

namespace GUI {

class TextRange {
public:
    TextRange() { }
    TextRange(const TextPosition& start, const TextPosition& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.is_valid() && m_end.is_valid() && m_start != m_end; }
    void clear()
    {
        m_start = {};
        m_end = {};
    }

    TextPosition& start() { return m_start; }
    TextPosition& end() { return m_end; }
    const TextPosition& start() const { return m_start; }
    const TextPosition& end() const { return m_end; }

    TextRange normalized() const { return TextRange(normalized_start(), normalized_end()); }

    void set_start(const TextPosition& position) { m_start = position; }
    void set_end(const TextPosition& position) { m_end = position; }

    void set(const TextPosition& start, const TextPosition& end)
    {
        m_start = start;
        m_end = end;
    }

    bool operator==(const TextRange& other) const
    {
        return m_start == other.m_start && m_end == other.m_end;
    }

    bool contains(const TextPosition& position) const
    {
        if (!(position.line() > m_start.line() || (position.line() == m_start.line() && position.column() >= m_start.column())))
            return false;
        if (!(position.line() < m_end.line() || (position.line() == m_end.line() && position.column() <= m_end.column())))
            return false;
        return true;
    }

private:
    TextPosition normalized_start() const { return m_start < m_end ? m_start : m_end; }
    TextPosition normalized_end() const { return m_start < m_end ? m_end : m_start; }

    TextPosition m_start;
    TextPosition m_end;
};

}

template<>
struct AK::Formatter<GUI::TextRange> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, GUI::TextRange const& value)
    {
        if (value.is_valid())
            return Formatter<FormatString>::format(builder, "{}-{}", value.start(), value.end());
        return Formatter<FormatString>::format(builder, "GUI::TextRange(Invalid)");
    }
};
