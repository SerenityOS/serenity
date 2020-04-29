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
#include <AK/JsonValue.h>

namespace AK {

class StreamJsonParser {
public:
    enum class LeniencyMode {
        Strict,
        AllowMissingDelimiters,
        AllowInvalidElements,
        Speculative
    };
    explicit StreamJsonParser(LeniencyMode mode = LeniencyMode::Strict)
        : m_leniency_mode(mode)
    {
    }

    ~StreamJsonParser()
    {
    }

    Function<void()> on_document_started;
    Function<void()> on_document_parsed;
    Function<void()> on_object_started;
    Function<void()> on_object_parsed;
    Function<void()> on_array_started;
    Function<void()> on_array_parsed;
    Function<void(String&&)> on_key_parsed;
    Function<void(JsonValue&&)> on_value_parsed;

    void reset();
    bool feed(char);
    bool feed(const StringView& string)
    {
        for (auto& c : string)
            if (!feed(c))
                return false;
        return true;
    }

private:
    void begin_parsing_value(char);
    void begin_parsing_key(char);
    void begin_parsing_array(char);
    void begin_parsing_object(char);
    void begin_parsing_string(char);
    void begin_parsing_number(char);

    void end_parsing_array();
    void end_parsing_document();
    void end_parsing_string();
    void end_parsing_object();
    void end_parsing_number();
    void end_parsing_true();
    void end_parsing_false();
    void end_parsing_null();

    void read_escape_character(char);
    void read_unicode_escape_character(char);

    bool should_ignore_spaces() const;

    void log_expectation(char expected, char got, const StringView& reason);
    void log_unexpected(char got, const StringView& reason);
    void log_unexpected(const StringView& reason);

    enum class ParserState {
        DocumentStart,
        InArray,
        InObject,
        KeyEnd,
        AfterKey,
        InString,
        EscapeStart,
        UnicodeEscape,
        InNumber,
        InTrue,
        InFalse,
        InNull,
        AfterValue,
        InUnicodeSurrogate,
        Done,
    };

    enum class ElementKind {
        Object,
        Array,
        Key,
        String,
    };

    struct State {
        ParserState state { ParserState::DocumentStart };
        Vector<ElementKind, 16> stack;

        Vector<char, 512> buffer;
        size_t unicode_index { 0 };
    };

    State save_state() const { return m_state; }
    void load_state(const State& state) { m_state = state; }

    size_t m_stream_position { 0 };
    LeniencyMode m_leniency_mode;
    State m_state;
};

}

using AK::StreamJsonParser;
