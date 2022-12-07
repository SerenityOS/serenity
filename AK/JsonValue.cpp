/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>

#ifndef KERNEL
#    include <AK/JsonParser.h>
#endif

#ifdef AK_OS_SERENITY
extern "C" size_t strlen(char const*);
#else
#    include <string.h>
#endif

namespace AK {

JsonValue::JsonValue(Type type)
    : m_type(type)
{
}

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
        m_value.set(String(other.m_value.get<String>()));
        break;
    case Type::Double:
        m_value.set(other.m_value.get<double>());
        break;
    case Type::Array:
        m_value.set(new JsonArray(*other.m_value.get<JsonArray*>()));
        break;
    case Type::Object:
        m_value.set(new JsonObject(*other.m_value.get<JsonObject*>()));
        break;
    case Type::Int32:
        m_value.set(other.m_value.get<i32>());
        break;
    case Type::UnsignedInt32:
        m_value.set(other.m_value.get<u32>());
        break;
    case Type::Int64:
        m_value.set(other.m_value.get<i64>());
        break;
    case Type::UnsignedInt64:
        m_value.set(other.m_value.get<u64>());
        break;
    case Type::Bool:
        m_value.set(other.m_value.get<bool>());
        break;
    default:
        m_value.set(Empty {});
        break;
    }
}

JsonValue::JsonValue(JsonValue&& other)
{
    m_type = exchange(other.m_type, Type::Null);
    m_value = exchange(other.m_value, Variant<Empty>());
}

JsonValue& JsonValue::operator=(JsonValue&& other)
{
    if (this != &other) {
        clear();
        m_type = exchange(other.m_type, Type::Null);
        m_value = exchange(other.m_value, Variant<Empty>());
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

#if !defined(KERNEL)
    if (is_number() && other.is_number() && to_number<double>() == other.to_number<double>()) {
        return true;
    }
#else
    if (is_number() && other.is_number() && to_number<i64>() == other.to_number<i64>()) {
        return true;
    }
#endif

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
            result &= value.equals(other.as_object().get(key));
        });
        return result;
    }

    return false;
}

JsonValue::JsonValue(i32 value)
    : m_type(Type::Int32)
    , m_value(value)
{
}

JsonValue::JsonValue(u32 value)
    : m_type(Type::UnsignedInt32)
    , m_value(value)
{
}

JsonValue::JsonValue(i64 value)
    : m_type(Type::Int64)
    , m_value(value)
{
}

JsonValue::JsonValue(u64 value)
    : m_type(Type::UnsignedInt64)
    , m_value(value)
{
}

JsonValue::JsonValue(char const* cstring)
    : JsonValue(StringView(cstring, strlen(cstring)))
{
}

#if !defined(KERNEL)
JsonValue::JsonValue(double value)
    : m_type(Type::Double)
{
    m_value.set(value);
}
#endif

JsonValue::JsonValue(DeprecatedString const& value)
{
    if (value.is_null()) {
        m_type = Type::Null;
    } else {
        m_type = Type::String;
        m_value.set(MUST(String::from_deprecated_string(value)));
    }
}

JsonValue::JsonValue(String const& value)
{
    m_type = Type::String;
    m_value.set(String(value));
}

JsonValue::JsonValue(StringView value)
    : JsonValue(MUST(String::from_utf8(value)))
{
}

JsonValue::JsonValue(JsonObject const& value)
    : m_type(Type::Object)
{
    m_value.set(new JsonObject(value));
}

JsonValue::JsonValue(JsonArray const& value)
    : m_type(Type::Array)
{
    m_value.set(new JsonArray(value));
}

JsonValue::JsonValue(JsonObject&& value)
    : m_type(Type::Object)
{
    m_value.set(new JsonObject(move(value)));
}

JsonValue::JsonValue(JsonArray&& value)
    : m_type(Type::Array)
{
    m_value.set(new JsonArray(move(value)));
}

void JsonValue::clear()
{
    switch (m_type) {
    case Type::Object:
        delete m_value.get<JsonObject*>();
        break;
    case Type::Array:
        delete m_value.get<JsonArray*>();
        break;
    default:
        break;
    }
    m_type = Type::Null;
    m_value.set(Empty {});
}

#ifndef KERNEL
ErrorOr<JsonValue> JsonValue::from_string(StringView input)
{
    return JsonParser(input).parse();
}
#endif

}
