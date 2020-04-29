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

#include <AK/StreamJsonParser.h>

namespace {

static constexpr inline bool isspace(char c)
{
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

static constexpr inline bool isdigit(char c)
{
    return (c >= '0' && c <= '9') || c == '-';
}

}

namespace AK {

bool StreamJsonParser::should_ignore_spaces() const
{
    return m_state.state != ParserState::InString
        && m_state.state != ParserState::UnicodeEscape
        && m_state.state != ParserState::EscapeStart
        && m_state.state != ParserState::InNumber
        && m_state.state != ParserState::DocumentStart;
}

bool StreamJsonParser::feed(char c)
{
    ++m_stream_position;

    if (isspace(c) && should_ignore_spaces())
        return true;

    switch (m_state.state) {
    case ParserState::AfterKey:
        begin_parsing_value(c);
        break;
    case ParserState::AfterValue: {
        ASSERT(m_state.stack.size());

        auto inside_element = m_state.stack.last();
        if (inside_element == ElementKind::Object) {
            if (c == '}') {
                end_parsing_object();
            } else if (c == ',') {
                m_state.state = ParserState::InObject;
            } else {
                log_expectation(',', c, "continue elements inside an object");
                if (m_leniency_mode >= LeniencyMode::AllowMissingDelimiters) {
                    // pretend there was a comma and continue parsing, maybe it would fix itself?
                    --m_stream_position;
                    feed(',');
                    if (!feed(c))
                        return false;
                }
            }
        } else if (inside_element == ElementKind::Array) {
            if (c == ']') {
                end_parsing_array();
            } else if (c == ',') {
                m_state.state = ParserState::InArray;
            } else {
                log_expectation(',', c, "continue elements inside an array");
                if (m_leniency_mode >= LeniencyMode::AllowMissingDelimiters) {
                    --m_stream_position;
                    feed(',');
                    if (!feed(c))
                        return false;
                } else {
                    // we can't contiue with the current leniency setting
                    return false;
                }
            }
        } else {
            dbg() << "An element was completed, but it made no sense in the context";
            if (m_leniency_mode < LeniencyMode::AllowInvalidElements) {
                return false;
            }
        }
        break;
    }
    case ParserState::DocumentStart:
        // start a document
        if (c == '[') {
            begin_parsing_array(c);
        } else if (c == '{') {
            begin_parsing_object(c);
        } else {
            log_unexpected(c, "document must be either an object or an array");
            if (m_leniency_mode < LeniencyMode::Speculative)
                return false;

            // try to figure out what we can do with this character
            auto state = save_state();

            feed('[');
            if (!feed(c)) {
                load_state(state);

                feed('{');
                if (!feed(c)) {
                    // drop the character
                    load_state(state);
                }
            }
        }
        break;
    case ParserState::Done:
        // the document is complete, we're not accepting more input
        log_unexpected(c, "outside a document");
        // we can't fix this
        return false;
    case ParserState::EscapeStart:
        read_escape_character(c);
        break;
    case ParserState::InArray:
        if (c == ']')
            end_parsing_array();
        else
            begin_parsing_value(c);
        break;
    case ParserState::InFalse:
        m_state.buffer.append(c);
        if (m_state.buffer.size() == 5)
            end_parsing_false();
        break;
    case ParserState::InNull:
        m_state.buffer.append(c);
        if (m_state.buffer.size() == 4)
            end_parsing_null();
        break;
    case ParserState::InNumber:
        if (c >= '0' && c <= '9') {
            m_state.buffer.append(c);
        } else if (c == '.') {
            if (m_state.buffer.contains_slow('.')) {
                log_unexpected(c, "malformed number");
                if (m_leniency_mode <= LeniencyMode::AllowInvalidElements)
                    return false;

                // ignore the dot
            } else if (m_state.buffer.contains_slow('e')) {
                log_unexpected(c, "decimal with fractional exponent");
                if (m_leniency_mode <= LeniencyMode::AllowInvalidElements)
                    return false;

                // ignore the dot
            } else {
                m_state.buffer.append(c);
            }
        } else if (c == 'e' || c == 'E') {
            if (m_state.buffer.contains_slow('e')) {
                log_unexpected(c, "decimal with multple exponent parts");
                if (m_leniency_mode <= LeniencyMode::AllowInvalidElements)
                    return false;

                // ignore the 'e'
            } else {
                m_state.buffer.append('e');
            }
        } else {
            end_parsing_number();
            if (!feed(c))
                return false;
        }
        break;
    case ParserState::InObject:
        if (c == '}') {
            end_parsing_object();
        } else if (c == '"') {
            begin_parsing_key(c);
        } else {
            log_expectation('"', c, "start a key");
            // pretend there was a dquote, and continue parsing
            --m_stream_position;
            feed('"');
            begin_parsing_key(c);
        }
        break;
    case ParserState::InString:
        switch (c) {
        case '"':
            end_parsing_string();
            break;
        case '\\':
            m_state.state = ParserState::EscapeStart;
            break;
        default:
            if (c < 0x1f) {
                log_unexpected("control character in string");
                // ignore the invalid character
            } else {
                m_state.buffer.append(c);
            }
            break;
        }
        break;
    case ParserState::InTrue:
        m_state.buffer.append(c);
        if (m_state.buffer.size() == 4)
            end_parsing_true();
        break;
    case ParserState::InUnicodeSurrogate:
        // FIXME: Someone that understands unicoded surrogates should implement this
        ASSERT_NOT_REACHED();
        break;
    case ParserState::KeyEnd:
        if (c != ':') {
            log_expectation(':', c, "come after a key");
            // pretend it was there and continue parsing
            --m_stream_position;
            feed(':');
            if (!feed(c))
                return false;
        }
        m_state.state = ParserState::AfterKey;
        break;
    case ParserState::UnicodeEscape:
        read_unicode_escape_character(c);
        break;
    }
    return true;
}

void StreamJsonParser::reset()
{
    m_state.stack.clear();
    m_state.state = ParserState::DocumentStart;
    m_state.buffer.clear();
    m_stream_position = 0;
}

void StreamJsonParser::begin_parsing_value(char c)
{
    if (c == '[')
        return begin_parsing_array(c);

    if (c == '{')
        return begin_parsing_object(c);

    if (c == '"')
        return begin_parsing_string(c);

    if (isdigit(c))
        return begin_parsing_number(c);

    if (c == 't') {
        m_state.state = ParserState::InTrue;
        m_state.buffer.append(c);
        return;
    }

    if (c == 'f') {
        m_state.state = ParserState::InFalse;
        m_state.buffer.append(c);
        return;
    }

    if (c == 'n') {
        m_state.state = ParserState::InNull;
        m_state.buffer.append(c);
        return;
    }

    log_unexpected(c, "unknown value");
}

void StreamJsonParser::begin_parsing_key(char)
{
    m_state.stack.append(ElementKind::Key);
    m_state.state = ParserState::InString;
}

void StreamJsonParser::begin_parsing_array(char)
{
    if (on_array_started)
        on_array_started();

    m_state.state = ParserState::InArray;
    m_state.stack.append(ElementKind::Array);
}

void StreamJsonParser::begin_parsing_object(char)
{
    if (on_object_started)
        on_object_started();

    m_state.state = ParserState::InObject;
    m_state.stack.append(ElementKind::Object);
}

void StreamJsonParser::begin_parsing_string(char)
{
    m_state.state = ParserState::InString;
    m_state.stack.append(ElementKind::String);
}

void StreamJsonParser::begin_parsing_number(char c)
{
    m_state.buffer.clear();
    m_state.buffer.append(c);
    m_state.state = ParserState::InNumber;
}

void StreamJsonParser::end_parsing_document()
{
    if (on_document_parsed)
        on_document_parsed();
    m_state.state = ParserState::Done;
}

void StreamJsonParser::end_parsing_array()
{
    ASSERT(m_state.stack.size());

    auto element_kind = m_state.stack.take_last();
    if (element_kind != ElementKind::Array) {
        // what the hell, how did we get here?
        log_unexpected("end of array");
        return;
    }

    if (on_array_parsed)
        on_array_parsed();

    m_state.state = ParserState::AfterValue;
    if (m_state.stack.size() == 0) {
        end_parsing_document();
    }
}

void StreamJsonParser::end_parsing_string()
{
    ASSERT(m_state.stack.size());

    auto element_kind = m_state.stack.take_last();
    if (element_kind == ElementKind::Key) {
        if (on_key_parsed)
            on_key_parsed(String(m_state.buffer.data(), m_state.buffer.size()));
        m_state.state = ParserState::KeyEnd;
    } else if (element_kind == ElementKind::String) {
        if (on_value_parsed)
            on_value_parsed({ String(m_state.buffer.data(), m_state.buffer.size()) });
        m_state.state = ParserState::AfterValue;
    } else {
        dbg() << "Invalid string position";
        // ignore the string
    }
    m_state.buffer.clear();
}

void StreamJsonParser::end_parsing_object()
{
    ASSERT(m_state.stack.size());

    auto element_kind = m_state.stack.take_last();
    if (element_kind != ElementKind::Object) {
        // what the hell, how did we get here?
        log_unexpected("end of object");
        return;
    }

    if (on_object_parsed)
        on_object_parsed();

    m_state.state = ParserState::AfterValue;
    if (m_state.stack.size() == 0) {
        end_parsing_document();
    }
}

void StreamJsonParser::end_parsing_number()
{
    String string { m_state.buffer.data(), m_state.buffer.size() };
    m_state.buffer.clear();
    m_state.state = ParserState::AfterValue;

    if (!on_value_parsed)
        return;

    if (string.contains(".")) {
        // is a double
        auto dbl = strtod(string.characters(), nullptr);
        on_value_parsed(dbl);
    } else {
        // int -- might not be the best choice, numbers in json are doubles
        bool ok;
        auto number = string.to_int(ok);
        ASSERT(ok);
        on_value_parsed(number);
    }
}

void StreamJsonParser::end_parsing_true()
{
    String string { m_state.buffer.data(), m_state.buffer.size() };
    m_state.buffer.clear();
    m_state.state = ParserState::AfterValue;

    if (string != "true") {
        log_unexpected("expected 'true'");
        if (m_leniency_mode < LeniencyMode::AllowInvalidElements)
            return;
    }

    if (on_value_parsed)
        on_value_parsed(true);
}

void StreamJsonParser::end_parsing_false()
{
    String string { m_state.buffer.data(), m_state.buffer.size() };
    m_state.buffer.clear();
    m_state.state = ParserState::AfterValue;

    if (string != "false") {
        log_unexpected("expected 'false'");
        if (m_leniency_mode < LeniencyMode::AllowInvalidElements)
            return;
    }

    if (on_value_parsed)
        on_value_parsed(false);
}

void StreamJsonParser::end_parsing_null()
{
    String string { m_state.buffer.data(), m_state.buffer.size() };
    m_state.buffer.clear();
    m_state.state = ParserState::AfterValue;

    if (string != "false") {
        log_unexpected("expected 'null'");
        if (m_leniency_mode < LeniencyMode::AllowInvalidElements)
            return;
    }

    if (on_value_parsed)
        on_value_parsed(JsonValue {});
}

void StreamJsonParser::read_escape_character(char c)
{
    switch (c) {
    case '"':
    case '\\':
    case '/':
        m_state.buffer.append(c);
        break;
    case 'b':
        m_state.buffer.append('\b');
        break;
    case 'f':
        m_state.buffer.append('\f');
        break;
    case 'r':
        m_state.buffer.append('\r');
        break;
    case 'n':
        m_state.buffer.append('\n');
        break;
    case 't':
        m_state.buffer.append('\t');
        break;
    case 'u':
        m_state.state = ParserState::UnicodeEscape;
        break;
    default:
        log_unexpected(c, "invalid escaped character");
        break;
    }

    if (m_state.state != ParserState::UnicodeEscape)
        m_state.state = ParserState::InString;
}
void StreamJsonParser::read_unicode_escape_character(char)
{
    m_state.unicode_index++;
    if (m_state.unicode_index == 4) {
        // FIXME: we should properly handle unicode, but JsonParser doesn't do so either...
        m_state.unicode_index = 0;
        m_state.buffer.append('?');
        m_state.state = ParserState::InString;
    }
}

void StreamJsonParser::log_expectation(char expected, char got, const StringView& reason)
{
    dbg() << "Expected '" << expected << "' to " << reason << " but got '" << got << "' (at stream position " << m_stream_position << ")";
}

void StreamJsonParser::log_unexpected(char got, const StringView& reason)
{
    dbg() << "Unexpected '" << got << "': " << reason << " (at stream position " << m_stream_position << ")";
}

void StreamJsonParser::log_unexpected(const StringView& reason)
{
    dbg() << reason << " (at stream position " << m_stream_position << ")";
}
}
