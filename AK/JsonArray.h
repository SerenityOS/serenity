#pragma once

#include <AK/JsonArraySerializer.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>

namespace AK {

class JsonArray {
public:
    JsonArray() {}
    ~JsonArray() {}

    JsonArray(const JsonArray& other)
        : m_values(other.m_values)
    {
    }

    JsonArray(JsonArray&& other)
        : m_values(move(other.m_values))
    {
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

    const JsonValue& at(int index) const { return m_values.at(index); }
    const JsonValue& operator[](int index) const { return at(index); }

    void clear() { m_values.clear(); }
    void append(const JsonValue& value) { m_values.append(value); }
    void append(JsonValue&& value) { m_values.append(move(value)); }

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
