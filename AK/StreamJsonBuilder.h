/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Function.h>
#include <AK/JsonPathElement.h>
#include <AK/JsonValue.h>
#include <AK/StreamJsonParser.h>

namespace AK {

class StreamJsonBuilder {
public:
    explicit StreamJsonBuilder(StreamJsonParser&& parser)
        : m_parser(parser)
    {
        m_parser.on_document_started = [&] { document_started(); };
        m_parser.on_document_parsed = [&] { document_parsed(); };
        m_parser.on_object_started = [&] { object_started(); };
        m_parser.on_object_parsed = [&] { object_parsed(); };
        m_parser.on_array_started = [&] { array_started(); };
        m_parser.on_array_parsed = [&] { array_parsed(); };
        m_parser.on_key_parsed = [&](auto key) { key_parsed(move(key)); };
        m_parser.on_value_parsed = [&](auto&& value) { value_parsed(move(value)); };
    }

    void append(char c) { m_parser.feed(c); }
    void append(const StringView& string)
    {
        for (auto& c : string) {
            if (m_stop)
                break;
            append(c);
        }
    }
    void append(const ByteBuffer& buffer)
    {
        append({ buffer.data(), buffer.size() });
    }

    const JsonValue& get() const { return m_object_stack.last(); }
    JsonValue& get() { return m_object_stack.last(); }

    const JsonValue& document() const { return m_object_stack.first(); }

    enum class VisitDecision {
        LeaveAlone,
        Discard,
        Store,
        Stop,
    };

    void stream(Vector<JsonPathElement> path, Function<VisitDecision(const JsonValue&)> callback)
    {
        m_callbacks.append({ move(path), move(callback) });
    }

protected:
    void document_started();
    void document_parsed();
    void object_started();
    void object_parsed();
    void array_started();
    void array_parsed();
    void key_parsed(String);
    void value_parsed(JsonValue&&);

    VisitDecision apply_streams(const JsonValue&);
    void handle_insertion(JsonValue&&, bool discard);

private:
    struct Callback {
        Vector<JsonPathElement> path;
        Function<VisitDecision(const JsonValue&)> function;
    };

    Vector<Callback> m_callbacks;
    StreamJsonParser& m_parser;
    Vector<JsonValue> m_object_stack;
    Vector<JsonPathElement> m_path;
    bool m_stop { false };
};

}

using AK::StreamJsonBuilder;
