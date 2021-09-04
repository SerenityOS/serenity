/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonArraySerializer.h>

#ifndef KERNEL
#    include <AK/JsonValue.h>
#endif

namespace AK {

template<typename Builder>
class JsonObjectSerializer {
public:
    explicit JsonObjectSerializer(Builder& builder)
        : m_builder(builder)
    {
        m_builder.append('{');
    }

    JsonObjectSerializer(JsonObjectSerializer const&) = delete;
    JsonObjectSerializer(JsonObjectSerializer&&) = delete;

    ~JsonObjectSerializer()
    {
        if (!m_finished)
            finish();
    }

#ifndef KERNEL
    void add(StringView const& key, JsonValue const& value)
    {
        begin_item(key);
        value.serialize(m_builder);
    }
#endif

    void add(StringView const& key, StringView const& value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append_escaped_for_json(value);
        m_builder.append('"');
    }

    void add(StringView const& key, String const& value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append_escaped_for_json(value);
        m_builder.append('"');
    }

    void add(StringView const& key, char const* value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append_escaped_for_json(value);
        m_builder.append('"');
    }

    void add(StringView const& key, bool value)
    {
        begin_item(key);
        m_builder.append(value ? "true" : "false");
    }

    void add(StringView const& key, int value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

    void add(StringView const& key, unsigned value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

    void add(StringView const& key, long value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

    void add(StringView const& key, long unsigned value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

    void add(StringView const& key, long long value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

    void add(StringView const& key, long long unsigned value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }

#ifndef KERNEL
    void add(StringView const& key, double value)
    {
        begin_item(key);
        m_builder.appendff("{}", value);
    }
#endif

    JsonArraySerializer<Builder> add_array(StringView const& key)
    {
        begin_item(key);
        return JsonArraySerializer(m_builder);
    }

    JsonObjectSerializer<Builder> add_object(StringView const& key)
    {
        begin_item(key);
        return JsonObjectSerializer(m_builder);
    }

    void finish()
    {
        VERIFY(!m_finished);
        m_finished = true;
        m_builder.append('}');
    }

private:
    void begin_item(StringView const& key)
    {
        if (!m_empty)
            m_builder.append(',');
        m_empty = false;

        m_builder.append('"');
        m_builder.append_escaped_for_json(key);
        m_builder.append("\":");
    }

    Builder& m_builder;
    bool m_empty { true };
    bool m_finished { false };
};

template<typename Builder>
JsonObjectSerializer<Builder> JsonArraySerializer<Builder>::add_object()
{
    begin_item();
    return JsonObjectSerializer(m_builder);
}

}

using AK::JsonObjectSerializer;
