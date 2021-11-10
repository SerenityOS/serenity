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
        (void)m_builder.append('{');
    }

    JsonObjectSerializer(const JsonObjectSerializer&) = delete;
    JsonObjectSerializer(JsonObjectSerializer&&) = delete;

    ~JsonObjectSerializer()
    {
        if (!m_finished)
            finish();
    }

#ifndef KERNEL
    void add(StringView key, const JsonValue& value)
    {
        begin_item(key);
        value.serialize(m_builder);
    }
#endif

    void add(StringView key, StringView value)
    {
        begin_item(key);
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(StringView key, const String& value)
    {
        begin_item(key);
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(StringView key, const char* value)
    {
        begin_item(key);
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(StringView key, bool value)
    {
        begin_item(key);
        (void)m_builder.append(value ? "true" : "false");
    }

    void add(StringView key, int value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

    void add(StringView key, unsigned value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

    void add(StringView key, long value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

    void add(StringView key, long unsigned value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

    void add(StringView key, long long value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

    void add(StringView key, long long unsigned value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }

#ifndef KERNEL
    void add(StringView key, double value)
    {
        begin_item(key);
        (void)m_builder.appendff("{}", value);
    }
#endif

    JsonArraySerializer<Builder> add_array(StringView key)
    {
        begin_item(key);
        return JsonArraySerializer(m_builder);
    }

    JsonObjectSerializer<Builder> add_object(StringView key)
    {
        begin_item(key);
        return JsonObjectSerializer(m_builder);
    }

    void finish()
    {
        VERIFY(!m_finished);
        m_finished = true;
        (void)m_builder.append('}');
    }

private:
    void begin_item(StringView key)
    {
        if (!m_empty)
            (void)m_builder.append(',');
        m_empty = false;

        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(key);
        (void)m_builder.append("\":");
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
