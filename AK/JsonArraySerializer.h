#pragma once

#include <AK/JsonValue.h>

namespace AK {

template<typename Builder>
class JsonObjectSerializer;

template<typename Builder>
class JsonArraySerializer {
public:
    explicit JsonArraySerializer(Builder& builder)
        : m_builder(builder)
    {
        m_builder.append('[');
    }

    JsonArraySerializer(const JsonArraySerializer&) = delete;
    JsonArraySerializer(JsonArraySerializer&&) = delete;

    ~JsonArraySerializer()
    {
        if (!m_finished)
            finish();
    }

    void add(const JsonValue& value)
    {
        begin_item();
        value.serialize(m_builder);
    }

    JsonArraySerializer<Builder> add_array()
    {
        begin_item();
        return JsonArraySerializer(m_builder);
    }

    // Implemented in JsonObjectSerializer.h
    JsonObjectSerializer<Builder> add_object();

    void finish()
    {
        ASSERT(!m_finished);
        m_finished = true;
        m_builder.append(']');
    }

private:
    void begin_item()
    {
        if (!m_empty)
            m_builder.append(',');
        m_empty = false;
    }

    Builder& m_builder;
    bool m_empty { true };
    bool m_finished { false };
};

}

using AK::JsonArraySerializer;
