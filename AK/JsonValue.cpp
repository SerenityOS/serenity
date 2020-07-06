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

#include <AK/Function.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>

namespace AK {

JsonValue::JsonValue(Type type)
    : m_type(type)
{
}

JsonValue::JsonValue(const JsonValue& other)
{
    copy_from(other);
}

JsonValue& JsonValue::operator=(const JsonValue& other)
{
    if (this != &other) {
        clear();
        copy_from(other);
    }
    return *this;
}

void JsonValue::copy_from(const JsonValue& other)
{
    m_type = other.m_type;
    switch (m_type) {
    case Type::String:
        ASSERT(!m_value.as_string);
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

bool JsonValue::equals(const JsonValue& other) const
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
        for (int i = 0; i < as_array().size(); ++i) {
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

JsonValue::JsonValue(const char* cstring)
    : JsonValue(String(cstring))
{
}

#if !defined(KERNEL)
JsonValue::JsonValue(double value)
    : m_type(Type::Double)
{
    m_value.as_double = value;
}
#endif

JsonValue::JsonValue(bool value)
    : m_type(Type::Bool)
{
    m_value.as_bool = value;
}

JsonValue::JsonValue(const String& value)
{
    if (value.is_null()) {
        m_type = Type::Null;
    } else {
        m_type = Type::String;
        m_value.as_string = const_cast<StringImpl*>(value.impl());
        m_value.as_string->ref();
    }
}

JsonValue::JsonValue(const IPv4Address& value)
    : JsonValue(value.to_string())
{
}

JsonValue::JsonValue(const JsonObject& value)
    : m_type(Type::Object)
{
    m_value.as_object = new JsonObject(value);
}

JsonValue::JsonValue(const JsonArray& value)
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

Optional<JsonValue> JsonValue::from_string(const StringView& input)
{
    return JsonParser(input).parse();
}

}
