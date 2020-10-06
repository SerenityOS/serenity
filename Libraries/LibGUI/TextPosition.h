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
#include <AK/String.h>

namespace GUI {

class TextPosition {
public:
    TextPosition() { }
    TextPosition(size_t line, size_t column)
        : m_line(line)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_line != 0xffffffffu && m_column != 0xffffffffu; }

    size_t line() const { return m_line; }
    size_t column() const { return m_column; }

    void set_line(size_t line) { m_line = line; }
    void set_column(size_t column) { m_column = column; }

    bool operator==(const TextPosition& other) const { return m_line == other.m_line && m_column == other.m_column; }
    bool operator!=(const TextPosition& other) const { return m_line != other.m_line || m_column != other.m_column; }
    bool operator<(const TextPosition& other) const { return m_line < other.m_line || (m_line == other.m_line && m_column < other.m_column); }

private:
    size_t m_line { 0xffffffff };
    size_t m_column { 0xffffffff };
};

inline const LogStream& operator<<(const LogStream& stream, const TextPosition& value)
{
    if (!value.is_valid())
        return stream << "GUI::TextPosition(Invalid)";
    return stream << String::format("(%zu,%zu)", value.line(), value.column());
}

}

namespace AK {

template<>
struct Formatter<GUI::TextPosition> : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const GUI::TextPosition& value)
    {
        if (value.is_valid())
            Formatter<StringView>::format(params, builder, String::formatted("({},{})", value.line(), value.column()));
        else
            Formatter<StringView>::format(params, builder, "GUI::TextPosition(Invalid)");
    }
};

}
