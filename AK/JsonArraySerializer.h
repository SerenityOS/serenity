/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
        (void)m_builder.append('[');
    }

    JsonArraySerializer(const JsonArraySerializer&) = delete;
    JsonArraySerializer(JsonArraySerializer&&) = delete;

    ~JsonArraySerializer()
    {
        if (!m_finished)
            finish();
    }

#ifndef KERNEL
    void add(const JsonValue& value)
    {
        begin_item();
        value.serialize(m_builder);
    }
#endif

    void add(StringView value)
    {
        begin_item();
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(const String& value)
    {
        begin_item();
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(const char* value)
    {
        begin_item();
        (void)m_builder.append('"');
        (void)m_builder.append_escaped_for_json(value);
        (void)m_builder.append('"');
    }

    void add(bool value)
    {
        begin_item();
        (void)m_builder.append(value ? "true"sv : "false"sv);
    }

    void add(int value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
    }

    void add(unsigned value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
    }

    void add(long value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
    }

    void add(long unsigned value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
    }

    void add(long long value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
    }

    void add(long long unsigned value)
    {
        begin_item();
        (void)m_builder.appendff("{}", value);
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
        VERIFY(!m_finished);
        m_finished = true;
        (void)m_builder.append(']');
    }

private:
    void begin_item()
    {
        if (!m_empty)
            (void)m_builder.append(',');
        m_empty = false;
    }

    Builder& m_builder;
    bool m_empty { true };
    bool m_finished { false };
};

}

using AK::JsonArraySerializer;
