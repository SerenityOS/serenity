/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/JsonArraySerializer.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>

namespace AK {

class JsonArray {
public:
    JsonArray() = default;
    ~JsonArray() = default;

    JsonArray(JsonArray const& other)
        : m_values(other.m_values)
    {
    }

    JsonArray(JsonArray&& other)
        : m_values(move(other.m_values))
    {
    }

    template<IterableContainer ContainerT>
    JsonArray(ContainerT const& source)
    {
        for (auto& value : source)
            m_values.append(move(value));
    }

    JsonArray& operator=(JsonArray const& other)
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

    [[nodiscard]] size_t size() const { return m_values.size(); }
    [[nodiscard]] bool is_empty() const { return m_values.is_empty(); }

    [[nodiscard]] JsonValue const& at(size_t index) const { return m_values.at(index); }
    [[nodiscard]] JsonValue const& operator[](size_t index) const { return at(index); }

    void clear() { m_values.clear(); }
    void append(JsonValue value) { m_values.append(move(value)); }
    void set(size_t index, JsonValue value) { m_values[index] = move(value); }

    template<typename Builder>
    typename Builder::OutputType serialized() const;

    template<typename Builder>
    void serialize(Builder&) const;

    [[nodiscard]] String to_string() const { return serialized<StringBuilder>(); }

    template<typename Callback>
    void for_each(Callback callback) const
    {
        for (auto const& value : m_values)
            callback(value);
    }

    [[nodiscard]] Vector<JsonValue> const& values() const { return m_values; }

    void ensure_capacity(size_t capacity) { m_values.ensure_capacity(capacity); }

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
