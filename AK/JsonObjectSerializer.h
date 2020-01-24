/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#pragma once

#include <AK/JsonArraySerializer.h>
#include <AK/JsonValue.h>

namespace AK {

template<typename Builder>
class JsonObjectSerializer {
public:
    explicit JsonObjectSerializer(Builder& builder)
        : m_builder(builder)
    {
        m_builder.append('{');
    }

    JsonObjectSerializer(const JsonObjectSerializer&) = delete;
    JsonObjectSerializer(JsonObjectSerializer&&) = delete;

    ~JsonObjectSerializer()
    {
        if (!m_finished)
            finish();
    }

    void add(const StringView& key, const JsonValue& value)
    {
        begin_item(key);
        value.serialize(m_builder);
    }

    void add(const StringView& key, const StringView& value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append(value);
        m_builder.append('"');
    }

    void add(const StringView& key, const String& value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append(value);
        m_builder.append('"');
    }

    void add(const StringView& key, const char* value)
    {
        begin_item(key);
        m_builder.append('"');
        m_builder.append(value);
        m_builder.append('"');
    }

    JsonArraySerializer<Builder> add_array(const StringView& key)
    {
        begin_item(key);
        return JsonArraySerializer(m_builder);
    }

    JsonObjectSerializer<Builder> add_object(const StringView& key)
    {
        begin_item(key);
        return JsonObjectSerializer(m_builder);
    }

    void finish()
    {
        ASSERT(!m_finished);
        m_finished = true;
        m_builder.append('}');
    }

private:
    void begin_item(const StringView& key)
    {
        if (!m_empty)
            m_builder.append(',');
        m_empty = false;

        m_builder.append('"');
        m_builder.append(key);
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
