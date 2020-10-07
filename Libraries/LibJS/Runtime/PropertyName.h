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

#include <AK/FlyString.h>
#include <LibJS/Runtime/StringOrSymbol.h>

namespace JS {

class PropertyName {
public:
    enum class Type {
        Invalid,
        Number,
        String,
        Symbol,
    };

    static PropertyName from_value(GlobalObject& global_object, Value value)
    {
        if (value.is_empty())
            return {};
        if (value.is_symbol())
            return &value.as_symbol();
        if (value.is_integer() && value.as_i32() >= 0)
            return value.as_i32();
        return value.to_string(global_object);
    }

    PropertyName() { }

    PropertyName(i32 index)
        : m_type(Type::Number)
        , m_number(index)
    {
        ASSERT(index >= 0);
    }

    PropertyName(const char* chars)
        : m_type(Type::String)
        , m_string(FlyString(chars))
    {
    }

    PropertyName(const String& string)
        : m_type(Type::String)
        , m_string(FlyString(string))
    {
    }

    PropertyName(const FlyString& string)
        : m_type(Type::String)
        , m_string(string)
    {
    }

    PropertyName(Symbol* symbol)
        : m_type(Type::Symbol)
        , m_symbol(symbol)
    {
    }

    PropertyName(const StringOrSymbol& string_or_symbol)
    {
        if (string_or_symbol.is_string()) {
            m_string = string_or_symbol.as_string();
            m_type = Type::String;
        } else if (string_or_symbol.is_symbol()) {
            m_symbol = const_cast<Symbol*>(string_or_symbol.as_symbol());
            m_type = Type::Symbol;
        }
    }

    bool is_valid() const { return m_type != Type::Invalid; }
    bool is_number() const { return m_type == Type::Number; }
    bool is_string() const { return m_type == Type::String; }
    bool is_symbol() const { return m_type == Type::Symbol; }

    i32 as_number() const
    {
        ASSERT(is_number());
        return m_number;
    }

    const FlyString& as_string() const
    {
        ASSERT(is_string());
        return m_string;
    }

    const Symbol* as_symbol() const
    {
        ASSERT(is_symbol());
        return m_symbol;
    }

    String to_string() const
    {
        ASSERT(is_valid());
        ASSERT(!is_symbol());
        if (is_string())
            return as_string();
        return String::number(as_number());
    }

    StringOrSymbol to_string_or_symbol() const
    {
        ASSERT(is_valid());
        ASSERT(!is_number());
        if (is_string())
            return StringOrSymbol(as_string());
        return StringOrSymbol(as_symbol());
    }

    Value to_value(VM& vm) const
    {
        if (is_string())
            return js_string(vm, m_string);
        if (is_number())
            return Value(m_number);
        if (is_symbol())
            return m_symbol;
        return js_undefined();
    }

private:
    Type m_type { Type::Invalid };
    FlyString m_string;
    Symbol* m_symbol { nullptr };
    u32 m_number { 0 };
};

}
