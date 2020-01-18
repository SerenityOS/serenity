/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/LogStream.h>
#include <LibGUI/GTextPosition.h>

class GTextRange {
public:
    GTextRange() {}
    GTextRange(const GTextPosition& start, const GTextPosition& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.is_valid() && m_end.is_valid(); }
    void clear()
    {
        m_start = {};
        m_end = {};
    }

    GTextPosition& start() { return m_start; }
    GTextPosition& end() { return m_end; }
    const GTextPosition& start() const { return m_start; }
    const GTextPosition& end() const { return m_end; }

    GTextRange normalized() const { return GTextRange(normalized_start(), normalized_end()); }

    void set_start(const GTextPosition& position) { m_start = position; }
    void set_end(const GTextPosition& position) { m_end = position; }

    void set(const GTextPosition& start, const GTextPosition& end)
    {
        m_start = start;
        m_end = end;
    }

    bool operator==(const GTextRange& other) const
    {
        return m_start == other.m_start && m_end == other.m_end;
    }

    bool contains(const GTextPosition& position) const
    {
        if (!(position.line() > m_start.line() || (position.line() == m_start.line() && position.column() >= m_start.column())))
            return false;
        if (!(position.line() < m_end.line() || (position.line() == m_end.line() && position.column() <= m_end.column())))
            return false;
        return true;
    }

private:
    GTextPosition normalized_start() const { return m_start < m_end ? m_start : m_end; }
    GTextPosition normalized_end() const { return m_start < m_end ? m_end : m_start; }

    GTextPosition m_start;
    GTextPosition m_end;
};

inline const LogStream& operator<<(const LogStream& stream, const GTextRange& value)
{
    if (!value.is_valid())
        return stream << "GTextRange(Invalid)";
    return stream << value.start() << '-' << value.end();
}
