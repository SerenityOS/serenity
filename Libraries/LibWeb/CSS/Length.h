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

#include <AK/String.h>
#include <LibWeb/Forward.h>

namespace Web {

class Length {
public:
    enum class Type {
        Undefined,
        Auto,
        Px,
        Em,
        Rem,
    };

    Length() { }
    Length(int value, Type type)
        : m_type(type)
        , m_value(value)
    {
    }
    Length(float value, Type type)
        : m_type(type)
        , m_value(value)
    {
    }

    static Length make_auto() { return Length(0, Type::Auto); }
    static Length make_px(float value) { return Length(value, Type::Px); }

    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_auto() const { return m_type == Type::Auto; }
    bool is_absolute() const { return m_type == Type::Px; }
    bool is_relative() const { return m_type == Type::Em || m_type == Type::Rem; }

    float raw_value() const { return m_value; }
    ALWAYS_INLINE float to_px(const LayoutNode& layout_node) const
    {
        if (is_relative())
            return relative_length_to_px(layout_node);
        switch (m_type) {
        case Type::Auto:
            return 0;
        case Type::Px:
            return m_value;
        case Type::Undefined:
        default:
            ASSERT_NOT_REACHED();
        }
    }

    String to_string() const
    {
        if (is_auto())
            return "[auto]";
        return String::format("[%g %s]", m_value, unit_name());
    }

private:
    float relative_length_to_px(const LayoutNode&) const;

    const char* unit_name() const;

    Type m_type { Type::Undefined };
    float m_value { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const Length& value)
{
    return stream << value.to_string();
}

}
