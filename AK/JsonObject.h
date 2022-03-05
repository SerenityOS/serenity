/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/String.h>

namespace AK {

class JsonObject {
public:
    JsonObject() = default;
    ~JsonObject() = default;

    JsonObject(JsonObject const& other)
        : m_members(other.m_members)
    {
    }

    JsonObject(JsonObject&& other)
        : m_members(move(other.m_members))
    {
    }

    JsonObject& operator=(JsonObject const& other)
    {
        if (this != &other)
            m_members = other.m_members;
        return *this;
    }

    JsonObject& operator=(JsonObject&& other)
    {
        if (this != &other)
            m_members = move(other.m_members);
        return *this;
    }

    [[nodiscard]] size_t size() const { return m_members.size(); }
    [[nodiscard]] bool is_empty() const { return m_members.is_empty(); }

    [[nodiscard]] JsonValue const& get(StringView key) const
    {
        auto const* value = get_ptr(key);
        static JsonValue* s_null_value { nullptr };
        if (!value) {
            if (!s_null_value)
                s_null_value = new JsonValue;
            return *s_null_value;
        }
        return *value;
    }

    [[nodiscard]] JsonValue const* get_ptr(StringView key) const
    {
        auto it = m_members.find(key);
        if (it == m_members.end())
            return nullptr;
        return &(*it).value;
    }

    [[nodiscard]] [[nodiscard]] bool has(StringView key) const
    {
        return m_members.contains(key);
    }

    [[nodiscard]] bool has_null(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_null();
    }
    [[nodiscard]] bool has_bool(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_bool();
    }
    [[nodiscard]] bool has_string(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_string();
    }
    [[nodiscard]] bool has_i32(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_i32();
    }
    [[nodiscard]] bool has_u32(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_u32();
    }
    [[nodiscard]] bool has_i64(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_i64();
    }
    [[nodiscard]] bool has_u64(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_u64();
    }
    [[nodiscard]] bool has_number(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_number();
    }
    [[nodiscard]] bool has_array(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_array();
    }
    [[nodiscard]] bool has_object(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_object();
    }
#ifndef KERNEL
    [[nodiscard]] [[nodiscard]] bool has_double(StringView key) const
    {
        auto const* value = get_ptr(key);
        return value && value->is_double();
    }
#endif

    void set(String const& key, JsonValue value)
    {
        m_members.set(key, move(value));
    }

    template<typename Callback>
    void for_each_member(Callback callback) const
    {
        for (auto const& member : m_members)
            callback(member.key, member.value);
    }

    bool remove(StringView key)
    {
        return m_members.remove(key);
    }

    template<typename Builder>
    typename Builder::OutputType serialized() const;

    template<typename Builder>
    void serialize(Builder&) const;

    [[nodiscard]] String to_string() const { return serialized<StringBuilder>(); }

private:
    OrderedHashMap<String, JsonValue> m_members;
};

template<typename Builder>
inline void JsonObject::serialize(Builder& builder) const
{
    auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
    for_each_member([&](auto& key, auto& value) {
        MUST(serializer.add(key, value));
    });
    MUST(serializer.finish());
}

template<typename Builder>
inline typename Builder::OutputType JsonObject::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

template<typename Builder>
inline void JsonValue::serialize(Builder& builder) const
{
    switch (m_type) {
    case Type::String: {
        builder.append("\"");
        builder.append_escaped_for_json({ m_value.as_string->characters(), m_value.as_string->length() });
        builder.append("\"");
    } break;
    case Type::Array:
        m_value.as_array->serialize(builder);
        break;
    case Type::Object:
        m_value.as_object->serialize(builder);
        break;
    case Type::Bool:
        builder.append(m_value.as_bool ? "true" : "false");
        break;
#if !defined(KERNEL)
    case Type::Double:
        builder.appendff("{}", m_value.as_double);
        break;
#endif
    case Type::Int32:
        builder.appendff("{}", as_i32());
        break;
    case Type::Int64:
        builder.appendff("{}", as_i64());
        break;
    case Type::UnsignedInt32:
        builder.appendff("{}", as_u32());
        break;
    case Type::UnsignedInt64:
        builder.appendff("{}", as_u64());
        break;
    case Type::Null:
        builder.append("null");
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename Builder>
inline typename Builder::OutputType JsonValue::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

}

using AK::JsonObject;
