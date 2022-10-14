/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/StringBuilder.h>

#ifndef KERNEL
#    include <AK/DeprecatedString.h>
#endif

namespace AK {

class JsonValue {
public:
    enum class Type {
        Null,
        Int32,
        UnsignedInt32,
        Int64,
        UnsignedInt64,
#if !defined(KERNEL)
        Double,
#endif
        Bool,
        String,
        Array,
        Object,
    };

    static ErrorOr<JsonValue> from_string(StringView);

    JsonValue() = default;
    explicit JsonValue(Type);
    ~JsonValue() { clear(); }

    JsonValue(JsonValue const&);
    JsonValue(JsonValue&&);

    JsonValue& operator=(JsonValue const&);
    JsonValue& operator=(JsonValue&&);

    JsonValue(int);
    JsonValue(unsigned);
    JsonValue(long);
    JsonValue(long unsigned);
    JsonValue(long long);
    JsonValue(long long unsigned);

#if !defined(KERNEL)
    JsonValue(double);
#endif
    JsonValue(char const*);
#ifndef KERNEL
    JsonValue(DeprecatedString const&);
#endif
    JsonValue(StringView);

    template<typename T>
    requires(SameAs<RemoveCVReference<T>, bool>)
    JsonValue(T value)
        : m_type(Type::Bool)
        , m_value { .as_bool = value }
    {
    }

    JsonValue(JsonArray const&);
    JsonValue(JsonObject const&);

    JsonValue(JsonArray&&);
    JsonValue(JsonObject&&);

    // FIXME: Implement these
    JsonValue& operator=(JsonArray&&) = delete;
    JsonValue& operator=(JsonObject&&) = delete;

    template<typename Builder>
    typename Builder::OutputType serialized() const;
    template<typename Builder>
    void serialize(Builder&) const;

#ifndef KERNEL
    DeprecatedString as_string_or(DeprecatedString const& alternative) const
    {
        if (is_string())
            return as_string();
        return alternative;
    }

    DeprecatedString to_deprecated_string() const
    {
        if (is_string())
            return as_string();
        return serialized<StringBuilder>();
    }
#endif

    int to_int(int default_value = 0) const
    {
        return to_i32(default_value);
    }
    i32 to_i32(i32 default_value = 0) const { return to_number<i32>(default_value); }
    i64 to_i64(i64 default_value = 0) const { return to_number<i64>(default_value); }

    unsigned to_uint(unsigned default_value = 0) const { return to_u32(default_value); }
    u32 to_u32(u32 default_value = 0) const { return to_number<u32>(default_value); }
    u64 to_u64(u64 default_value = 0) const { return to_number<u64>(default_value); }
#if !defined(KERNEL)
    float to_float(float default_value = 0) const
    {
        return to_number<float>(default_value);
    }
    double to_double(double default_value = 0) const { return to_number<double>(default_value); }
#endif

    FlatPtr to_addr(FlatPtr default_value = 0) const
    {
#ifdef __LP64__
        return to_u64(default_value);
#else
        return to_u32(default_value);
#endif
    }

    bool to_bool(bool default_value = false) const
    {
        if (!is_bool())
            return default_value;
        return as_bool();
    }

    i32 as_i32() const
    {
        VERIFY(is_i32());
        return m_value.as_i32;
    }

    u32 as_u32() const
    {
        VERIFY(is_u32());
        return m_value.as_u32;
    }

    i64 as_i64() const
    {
        VERIFY(is_i64());
        return m_value.as_i64;
    }

    u64 as_u64() const
    {
        VERIFY(is_u64());
        return m_value.as_u64;
    }

    bool as_bool() const
    {
        VERIFY(is_bool());
        return m_value.as_bool;
    }

#ifndef KERNEL
    DeprecatedString as_string() const
    {
        VERIFY(is_string());
        return *m_value.as_string;
    }
#endif

    JsonObject& as_object()
    {
        VERIFY(is_object());
        return *m_value.as_object;
    }

    JsonObject const& as_object() const
    {
        VERIFY(is_object());
        return *m_value.as_object;
    }

    JsonArray& as_array()
    {
        VERIFY(is_array());
        return *m_value.as_array;
    }

    JsonArray const& as_array() const
    {
        VERIFY(is_array());
        return *m_value.as_array;
    }

#if !defined(KERNEL)
    double as_double() const
    {
        VERIFY(is_double());
        return m_value.as_double;
    }
#endif

    Type type() const
    {
        return m_type;
    }

    bool is_null() const { return m_type == Type::Null; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_string() const { return m_type == Type::String; }
    bool is_i32() const { return m_type == Type::Int32; }
    bool is_u32() const { return m_type == Type::UnsignedInt32; }
    bool is_i64() const { return m_type == Type::Int64; }
    bool is_u64() const { return m_type == Type::UnsignedInt64; }
#if !defined(KERNEL)
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
#if !defined(KERNEL)
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
#if !defined(KERNEL)
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

    bool equals(JsonValue const& other) const;

private:
    void clear();
    void copy_from(JsonValue const&);

    Type m_type { Type::Null };

    union {
#ifndef KERNEL
        StringImpl* as_string { nullptr };
#endif
        JsonArray* as_array;
        JsonObject* as_object;
#if !defined(KERNEL)
        double as_double;
#endif
        i32 as_i32;
        u32 as_u32;
        i64 as_i64;
        u64 as_u64;
        bool as_bool;
    } m_value;
};

#ifndef KERNEL
template<>
struct Formatter<JsonValue> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, JsonValue const& value)
    {
        return Formatter<StringView>::format(builder, value.to_deprecated_string());
    }
};
#endif

}

#if USING_AK_GLOBALLY
using AK::JsonValue;
#endif
