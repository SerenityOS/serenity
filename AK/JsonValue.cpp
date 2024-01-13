/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringView.h>

#ifndef KERNEL
#    include <AK/JsonParser.h>
#endif

namespace AK {

JsonValue::JsonValue(JsonValue const& other)
{
    copy_from(other);
}

JsonValue& JsonValue::operator=(JsonValue const& other)
{
    if (this != &other) {
        clear();
        copy_from(other);
    }
    return *this;
}

void JsonValue::copy_from(JsonValue const& other)
{
    m_type = other.m_type;
    switch (m_type) {
    case Type::String:
        VERIFY(!m_value.as_string);
        m_value.as_string = other.m_value.as_string;
        m_value.as_string->ref();
        break;
    case Type::Object:
        m_value.as_object = new JsonObject(*other.m_value.as_object);
        break;
    case Type::Array:
        m_value.as_array = new JsonArray(*other.m_value.as_array);
        break;
    default:
        m_value.as_u64 = other.m_value.as_u64;
        break;
    }
}

JsonValue::JsonValue(JsonValue&& other)
{
    m_type = exchange(other.m_type, Type::Null);
    m_value.as_u64 = exchange(other.m_value.as_u64, 0);
}

JsonValue& JsonValue::operator=(JsonValue&& other)
{
    if (this != &other) {
        clear();
        m_type = exchange(other.m_type, Type::Null);
        m_value.as_u64 = exchange(other.m_value.as_u64, 0);
    }
    return *this;
}

bool JsonValue::equals(JsonValue const& other) const
{
    if (is_null() && other.is_null())
        return true;

    if (is_bool() && other.is_bool() && as_bool() == other.as_bool())
        return true;

    if (is_string() && other.is_string() && as_string() == other.as_string())
        return true;

    if (is_number() && other.is_number()) {
        auto normalize = [](Variant<u64, i64, double> representation, bool& is_negative) {
            return representation.visit(
                [&](u64& value) -> Variant<u64, double> {
                    is_negative = false;
                    return value;
                },
                [&](i64& value) -> Variant<u64, double> {
                    is_negative = value < 0;
                    return static_cast<u64>(abs(value));
                },
                [&](double& value) -> Variant<u64, double> {
                    is_negative = value < 0;
                    value = abs(value);
                    if (static_cast<double>(static_cast<u64>(value)) == value)
                        return static_cast<u64>(value);
                    return value;
                });
        };
        bool is_this_negative;
        auto normalized_this = normalize(as_number(), is_this_negative);
        bool is_that_negative;
        auto normalized_that = normalize(other.as_number(), is_that_negative);
        return is_this_negative == is_that_negative && normalized_this == normalized_that;
    }

    if (is_array() && other.is_array() && as_array().size() == other.as_array().size()) {
        bool result = true;
        for (size_t i = 0; i < as_array().size(); ++i) {
            result &= as_array().at(i).equals(other.as_array().at(i));
        }
        return result;
    }

    if (is_object() && other.is_object() && as_object().size() == other.as_object().size()) {
        bool result = true;
        as_object().for_each_member([&](auto& key, auto& value) {
            auto other_value = other.as_object().get(key);
            if (other_value.has_value())
                result &= value.equals(*other_value);
            else
                result = false;
        });
        return result;
    }

    return false;
}

JsonValue::JsonValue(int value)
    : m_type(Type::Int32)
{
    m_value.as_i32 = value;
}

JsonValue::JsonValue(unsigned value)
    : m_type(Type::UnsignedInt32)
{
    m_value.as_u32 = value;
}

JsonValue::JsonValue(long value)
    : m_type(sizeof(long) == 8 ? Type::Int64 : Type::Int32)
{
    if constexpr (sizeof(long) == 8)
        m_value.as_i64 = value;
    else
        m_value.as_i32 = value;
}

JsonValue::JsonValue(unsigned long value)
    : m_type(sizeof(long) == 8 ? Type::UnsignedInt64 : Type::UnsignedInt32)
{
    if constexpr (sizeof(long) == 8)
        m_value.as_u64 = value;
    else
        m_value.as_u32 = value;
}

JsonValue::JsonValue(long long value)
    : m_type(Type::Int64)
{
    static_assert(sizeof(long long unsigned) == 8);
    m_value.as_i64 = value;
}

JsonValue::JsonValue(long long unsigned value)
    : m_type(Type::UnsignedInt64)
{
    static_assert(sizeof(long long unsigned) == 8);
    m_value.as_u64 = value;
}

JsonValue::JsonValue(char const* cstring)
    : JsonValue(ByteString(cstring))
{
}

#if !defined(KERNEL)
JsonValue::JsonValue(double value)
    : m_type(Type::Double)
{
    m_value.as_double = value;
}
#endif

JsonValue::JsonValue(ByteString const& value)
{
    m_type = Type::String;
    m_value.as_string = const_cast<StringImpl*>(value.impl());
    m_value.as_string->ref();
}

JsonValue::JsonValue(StringView value)
    : JsonValue(value.to_byte_string())
{
}

JsonValue::JsonValue(JsonObject const& value)
    : m_type(Type::Object)
{
    m_value.as_object = new JsonObject(value);
}

JsonValue::JsonValue(JsonArray const& value)
    : m_type(Type::Array)
{
    m_value.as_array = new JsonArray(value);
}

JsonValue::JsonValue(JsonObject&& value)
    : m_type(Type::Object)
{
    m_value.as_object = new JsonObject(move(value));
}

JsonValue::JsonValue(JsonArray&& value)
    : m_type(Type::Array)
{
    m_value.as_array = new JsonArray(move(value));
}

void JsonValue::clear()
{
    switch (m_type) {
    case Type::String:
        m_value.as_string->unref();
        break;
    case Type::Object:
        delete m_value.as_object;
        break;
    case Type::Array:
        delete m_value.as_array;
        break;
    default:
        break;
    }
    m_type = Type::Null;
    m_value.as_string = nullptr;
}

#ifndef KERNEL
ErrorOr<JsonValue> JsonValue::from_string(StringView input)
{
    return JsonParser(input).parse();
}
#endif

}
