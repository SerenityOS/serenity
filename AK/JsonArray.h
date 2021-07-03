/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonArraySerializer.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>

namespace AK {

class JsonArray {
public:
    JsonArray() = default;
    ~JsonArray() = default;

    JsonArray(const JsonArray& other)
        : m_values(other.m_values)
    {
    }

    JsonArray(JsonArray&& other)
        : m_values(move(other.m_values))
    {
    }

    template<typename T>
    JsonArray(const Vector<T>& vector)
    {
        for (auto& value : vector)
            m_values.append(move(value));
    }

    JsonArray& operator=(const JsonArray& other)
    {
        if (this != &other)
            m_values = other.m_values;
        return *this;
    }

    JsonArray& operator=(JsonArray&& other)
    {
        if (this != &other)
            m_values = move(other.m_values);
        return *this;
    }

    int size() const { return m_values.size(); }
    bool is_empty() const { return m_values.is_empty(); }

    const JsonValue& at(size_t index) const { return m_values.at(index); }
    const JsonValue& operator[](size_t index) const { return at(index); }

    void clear() { m_values.clear(); }
    void append(JsonValue value) { m_values.append(move(value)); }
    void set(int index, JsonValue value) { m_values[index] = move(value); }

    template<typename Builder>
    typename Builder::OutputType serialized() const;

    template<typename Builder>
    void serialize(Builder&) const;

    String to_string() const { return serialized<StringBuilder>(); }

    template<typename Callback>
    void for_each(Callback callback) const
    {
        for (auto& value : m_values)
            callback(value);
    }

    const Vector<JsonValue>& values() const { return m_values; }

    void ensure_capacity(int capacity) { m_values.ensure_capacity(capacity); }

private:
    Vector<JsonValue> m_values;
};

template<typename Builder>
inline void JsonArray::serialize(Builder& builder) const
{
    JsonArraySerializer serializer { builder };
    for_each([&](auto& value) { serializer.add(value); });
}

template<typename Builder>
inline typename Builder::OutputType JsonArray::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

}

using AK::JsonArray;
