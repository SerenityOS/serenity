/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef KERNEL
#    error "JsonValue does not propagate allocation failures, so it is not safe to use in the kernel."
#endif

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/StringBuilder.h>

namespace AK {

class JsonValue {
public:
    enum class Type {
        Null,
        Int32,
        UnsignedInt32,
        Int64,
        UnsignedInt64,
        Double,
        Bool,
        String,
        Array,
        Object,
    };

    static ErrorOr<JsonValue> from_string(StringView);

    JsonValue() = default;
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

    JsonValue(double);
    JsonValue(char const*);
    JsonValue(ByteString const&);
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

    ByteString as_string_or(ByteString const& alternative) const
    {
        if (is_string())
            return as_string();
        return alternative;
    }

    ByteString deprecated_to_byte_string() const
    {
        if (is_string())
            return as_string();
        return serialized<StringBuilder>();
    }

    Optional<int> get_int() const { return get_integer<int>(); }
    Optional<i32> get_i32() const { return get_integer<i32>(); }
    Optional<i64> get_i64() const { return get_integer<i64>(); }

    Optional<unsigned> get_uint() const { return get_integer<unsigned>(); }
    Optional<u32> get_u32() const { return get_integer<u32>(); }
    Optional<u64> get_u64() const { return get_integer<u64>(); }
    Optional<float> get_float_with_precision_loss() const { return get_number_with_precision_loss<float>(); }
    Optional<double> get_double_with_precision_loss() const { return get_number_with_precision_loss<double>(); }

    Optional<FlatPtr> get_addr() const
    {
        // Note: This makes the lambda dependent on the template parameter, which is necessary
        //       for the `if constexpr` to not evaluate both branches.
        auto fn = [&]<typename T>() -> Optional<T> {
            if constexpr (IsSame<T, u64>) {
                return get_u64();
            } else {
                return get_u32();
            }
        };

        return fn.operator()<FlatPtr>();
    }

    Optional<bool> get_bool() const
    {
        if (!is_bool())
            return {};
        return as_bool();
    }

    bool as_bool() const
    {
        VERIFY(is_bool());
        return m_value.as_bool;
    }

    ByteString as_string() const
    {
        VERIFY(is_string());
        return *m_value.as_string;
    }

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

    Variant<u64, i64, double> as_number() const
    {
        VERIFY(is_number());

        switch (m_type) {
        case Type::Int32:
            return static_cast<i64>(m_value.as_i32);
        case Type::UnsignedInt32:
            return static_cast<i64>(m_value.as_u32);
        case Type::Int64:
            return m_value.as_i64;
        case Type::UnsignedInt64:
            return m_value.as_u64;
        case Type::Double:
            return m_value.as_double;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    Type type() const
    {
        return m_type;
    }

    bool is_null() const { return m_type == Type::Null; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_string() const { return m_type == Type::String; }
    bool is_array() const { return m_type == Type::Array; }
    bool is_object() const { return m_type == Type::Object; }
    bool is_number() const
    {
        switch (m_type) {
        case Type::Int32:
        case Type::UnsignedInt32:
        case Type::Int64:
        case Type::UnsignedInt64:
        case Type::Double:
            return true;
        default:
            return false;
        }
    }

    template<typename T>
    Optional<T> get_number_with_precision_loss() const
    {
        switch (m_type) {
        case Type::Double:
            return static_cast<T>(m_value.as_double);
        case Type::Int32:
            return static_cast<T>(m_value.as_i32);
        case Type::UnsignedInt32:
            return static_cast<T>(m_value.as_u32);
        case Type::Int64:
            return static_cast<T>(m_value.as_i64);
        case Type::UnsignedInt64:
            return static_cast<T>(m_value.as_u64);
        default:
            return {};
        }
    }

    template<Integral T>
    bool is_integer() const
    {
        switch (m_type) {
        case Type::Int32:
            return is_within_range<T>(m_value.as_i32);
        case Type::UnsignedInt32:
            return is_within_range<T>(m_value.as_u32);
        case Type::Int64:
            return is_within_range<T>(m_value.as_i64);
        case Type::UnsignedInt64:
            return is_within_range<T>(m_value.as_u64);
        default:
            return false;
        }
    }

    template<Integral T>
    T as_integer() const
    {
        VERIFY(is_integer<T>());

        switch (m_type) {
        case Type::Int32:
            return static_cast<T>(m_value.as_i32);
        case Type::UnsignedInt32:
            return static_cast<T>(m_value.as_u32);
        case Type::Int64:
            return static_cast<T>(m_value.as_i64);
        case Type::UnsignedInt64:
            return static_cast<T>(m_value.as_u64);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    template<Integral T>
    Optional<T> get_integer() const
    {
        if (!is_integer<T>())
            return {};
        return as_integer<T>();
    }

    bool equals(JsonValue const& other) const;

private:
    void clear();
    void copy_from(JsonValue const&);

    Type m_type { Type::Null };

    union {
        StringImpl* as_string { nullptr };
        JsonArray* as_array;
        JsonObject* as_object;
        double as_double;
        i32 as_i32;
        u32 as_u32;
        i64 as_i64;
        u64 as_u64;
        bool as_bool;
    } m_value;
};

template<>
struct Formatter<JsonValue> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, JsonValue const& value)
    {
        return Formatter<StringView>::format(builder, value.serialized<StringBuilder>());
    }
};

}

#if USING_AK_GLOBALLY
using AK::JsonValue;
#endif
