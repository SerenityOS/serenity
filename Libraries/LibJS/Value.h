/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Forward.h>
#include <AK/LogStream.h>
#include <LibJS/Forward.h>

namespace JS {

class Value {
public:
    enum class Type {
        Undefined,
        Null,
        Number,
        String,
        Object,
        Boolean,
    };

    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_null() const { return m_type == Type::Null; }
    bool is_number() const { return m_type == Type::Number; }
    bool is_string() const { return m_type == Type::String; }
    bool is_object() const { return m_type == Type::Object; }
    bool is_boolean() const { return m_type == Type::Boolean; }

    explicit Value(bool value)
        : m_type(Type::Boolean)
    {
        m_value.as_bool = value;
    }

    explicit Value(double value)
        : m_type(Type::Number)
    {
        m_value.as_double = value;
    }

    explicit Value(i32 value)
        : m_type(Type::Number)
    {
        m_value.as_double = value;
    }

    explicit Value(Object* object)
        : m_type(Type::Object)
    {
        m_value.as_object = object;
    }

    explicit Value(Type type)
        : m_type(type)
    {
    }

    Type type() const { return m_type; }

    double as_double() const
    {
        ASSERT(type() == Type::Number);
        return m_value.as_double;
    }

    bool as_bool() const
    {
        ASSERT(type() == Type::Boolean);
        return m_value.as_bool;
    }

    Object* as_object()
    {
        ASSERT(type() == Type::Object);
        return m_value.as_object;
    }

    const Object* as_object() const
    {
        ASSERT(type() == Type::Object);
        return m_value.as_object;
    }

    const StringImpl* as_string() const
    {
        ASSERT(is_string());
        return m_value.as_string;
    }

    String to_string() const;

private:
    Type m_type { Type::Undefined };

    union {
        bool as_bool;
        double as_double;
        StringImpl* as_string;
        Object* as_object;
    } m_value;
};

inline Value js_undefined()
{
    return Value(Value::Type::Undefined);
}

inline Value js_null()
{
    return Value(Value::Type::Null);
}

const LogStream& operator<<(const LogStream&, const Value&);

}
