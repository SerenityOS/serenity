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

#include <AK/IPv4Address.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace AK {

class JsonArray;
class JsonObject;
class StringBuilder;

class JsonValue {
public:
    enum class Type {
        Undefined,
        Null,
        Int32,
        UnsignedInt32,
        Int64,
        UnsignedInt64,
#ifndef KERNEL
        Double,
#endif
        Bool,
        String,
        Array,
        Object,
    };

    static JsonValue from_string(const StringView&);

    explicit JsonValue(Type = Type::Null);
    ~JsonValue() { clear(); }

    JsonValue(const JsonValue&);
    JsonValue(JsonValue&&);

    JsonValue& operator=(const JsonValue&);
    JsonValue& operator=(JsonValue&&);

    JsonValue(i32);
    JsonValue(u32);
    JsonValue(i64);
    JsonValue(u64);

#ifndef KERNEL
    JsonValue(double);
#endif
    JsonValue(bool);
    JsonValue(const char*);
    JsonValue(const String&);
    JsonValue(const IPv4Address&);
    JsonValue(const JsonArray&);
    JsonValue(const JsonObject&);

    JsonValue(JsonArray&&);
    JsonValue(JsonObject&&);

    // FIXME: Implement these
    JsonValue& operator=(JsonArray&&) = delete;
    JsonValue& operator=(JsonObject&&) = delete;

    template<typename Builder>
    typename Builder::OutputType serialized() const;
    template<typename Builder>
    void serialize(Builder&) const;

    String as_string_or(const String& alternative)
    {
        if (is_string())
            return as_string();
        return alternative;
    }

    String to_string() const
    {
        if (is_string())
            return as_string();
        return serialized<StringBuilder>();
    }

    Optional<IPv4Address> to_ipv4_address() const
    {
        if (!is_string())
            return {};
        return IPv4Address::from_string(as_string());
    }

    int to_int(int default_value = 0) const { return to_i32(default_value); }
    i32 to_i32(i32 default_value = 0) const { return to_number<i32>(default_value); }

    unsigned to_uint(unsigned default_value = 0) const { return to_u32(default_value); }
    u32 to_u32(u32 default_value = 0) const { return to_number<u32>(default_value); }

    bool to_bool(bool default_value = false) const
    {
        if (!is_bool())
            return default_value;
        return as_bool();
    }

    i32 as_i32() const
    {
        ASSERT(is_i32());
        return m_value.as_i32;
    }

    u32 as_u32() const
    {
        ASSERT(is_u32());
        return m_value.as_u32;
    }

    i64 as_i64() const
    {
        ASSERT(is_i64());
        return m_value.as_i64;
    }

    u64 as_u64() const
    {
        ASSERT(is_u64());
        return m_value.as_u64;
    }

    int as_bool() const
    {
        ASSERT(is_bool());
        return m_value.as_bool;
    }

    String as_string() const
    {
        ASSERT(is_string());
        return *m_value.as_string;
    }

    const JsonObject& as_object() const
    {
        ASSERT(is_object());
        return *m_value.as_object;
    }

    const JsonArray& as_array() const
    {
        ASSERT(is_array());
        return *m_value.as_array;
    }

#ifndef KERNEL
    double as_double() const
    {
        ASSERT(is_double());
        return m_value.as_double;
    }
#endif

    Type type() const
    {
        return m_type;
    }

    bool is_null() const { return m_type == Type::Null; }
    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_string() const { return m_type == Type::String; }
    bool is_i32() const { return m_type == Type::Int32; }
    bool is_u32() const { return m_type == Type::UnsignedInt32; }
    bool is_i64() const { return m_type == Type::Int64; }
    bool is_u64() const { return m_type == Type::UnsignedInt64; }
#ifndef KERNEL
    bool is_double() const
    {
        return m_type == Type::Double;
    }
#endif
    bool is_array() const
    {
        return m_type == Type::Array;
    }
    bool is_object() const { return m_type == Type::Object; }
    bool is_number() const
    {
        switch (m_type) {
        case Type::Int32:
        case Type::UnsignedInt32:
        case Type::Int64:
        case Type::UnsignedInt64:
#ifndef KERNEL
        case Type::Double:
#endif
            return true;
        default:
            return false;
        }
    }

    template<typename T>
    T to_number(T default_value = 0) const
    {
#ifndef KERNEL
        if (is_double())
            return (T)as_double();
#endif
        if (type() == Type::Int32)
            return (T)as_i32();
        if (type() == Type::UnsignedInt32)
            return (T)as_u32();
        if (type() == Type::Int64)
            return (T)as_i64();
        if (type() == Type::UnsignedInt64)
            return (T)as_u64();
        return default_value;
    }

private:
    void clear();
    void copy_from(const JsonValue&);

    Type m_type { Type::Undefined };

    union {
        StringImpl* as_string { nullptr };
        JsonArray* as_array;
        JsonObject* as_object;
#ifndef KERNEL
        double as_double;
#endif
        i32 as_i32;
        u32 as_u32;
        i64 as_i64;
        u64 as_u64;
        bool as_bool;
    } m_value;
};

}

using AK::JsonValue;
