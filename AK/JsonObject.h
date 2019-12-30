#pragma once

#include <AK/String.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>

namespace AK {

class JsonObject {
public:
    JsonObject() {}
    ~JsonObject() {}

    JsonObject(const JsonObject& other)
        : m_members(other.m_members)
    {
    }

    JsonObject(JsonObject&& other)
        : m_members(move(other.m_members))
    {
    }

    JsonObject& operator=(const JsonObject& other)
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

    int size() const { return m_members.size(); }
    bool is_empty() const { return m_members.is_empty(); }

    JsonValue get(const String& key) const
    {
        auto* value = get_ptr(key);
        return value ? *value : JsonValue(JsonValue::Type::Undefined);
    }

    const JsonValue* get_ptr(const String& key) const
    {
        auto it = m_members.find(key);
        if (it == m_members.end())
            return nullptr;
        return &(*it).value;
    }

    bool has(const String& key) const
    {
        return m_members.contains(key);
    }

    void set(const String& key, JsonValue&& value)
    {
        m_members.set(key, move(value));
    }

    void set(const String& key, const JsonValue& value)
    {
        m_members.set(key, JsonValue(value));
    }

    template<typename Callback>
    void for_each_member(Callback callback) const
    {
        for (auto& it : m_members)
            callback(it.key, it.value);
    }

    template<typename Builder>
    typename Builder::OutputType serialized() const;

    template<typename Builder>
    void serialize(Builder&) const;

    String to_string() const { return serialized<StringBuilder>(); }

private:
    HashMap<String, JsonValue> m_members;
};

template<typename Builder>
inline void JsonObject::serialize(Builder& builder) const
{
    JsonObjectSerializer serializer { builder };
    for_each_member([&](auto& key, auto& value) {
            serializer.add(key, value);
    });
}

template<typename Builder>
inline typename Builder::OutputType JsonObject::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

template<typename Builder>
void JsonValue::serialize(Builder& builder) const
{
    switch (m_type) {
    case Type::String:
        builder.appendf("\"%s\"", m_value.as_string->characters());
        break;
    case Type::Array:
        m_value.as_array->serialize(builder);
        break;
    case Type::Object:
        m_value.as_object->serialize(builder);
        break;
    case Type::Bool:
        builder.append(m_value.as_bool ? "true" : "false");
        break;
#ifndef KERNEL
    case Type::Double:
        builder.appendf("%g", m_value.as_double);
        break;
#endif
    case Type::Int32:
        builder.appendf("%d", as_i32());
        break;
    case Type::Int64:
        builder.appendf("%lld", as_i64());
        break;
    case Type::UnsignedInt32:
        builder.appendf("%u", as_u32());
        break;
    case Type::UnsignedInt64:
        builder.appendf("%llu", as_u64());
        break;
    case Type::Undefined:
        builder.append("undefined");
        break;
    case Type::Null:
        builder.append("null");
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

template<typename Builder>
typename Builder::OutputType JsonValue::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

}

using AK::JsonObject;
