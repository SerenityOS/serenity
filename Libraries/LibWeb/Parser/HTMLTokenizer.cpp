/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibTextCodec/Decoder.h>
#include <LibWeb/Parser/Entities.h>
#include <LibWeb/Parser/HTMLToken.h>
#include <LibWeb/Parser/HTMLTokenizer.h>
#include <ctype.h>

#pragma GCC diagnostic ignored "-Wunused-label"

//#define TOKENIZER_TRACE

#define PARSE_ERROR()                                                                      \
    do {                                                                                   \
        dbg() << "Parse error (tokenization)" << __PRETTY_FUNCTION__ << " @ " << __LINE__; \
    } while (0)

#define CONSUME_NEXT_INPUT_CHARACTER \
    current_input_character = next_codepoint();

#define SWITCH_TO(new_state)              \
    do {                                  \
        will_switch_to(State::new_state); \
        m_state = State::new_state;       \
        CONSUME_NEXT_INPUT_CHARACTER;     \
        goto new_state;                   \
    } while (0)

#define RECONSUME_IN(new_state)              \
    do {                                     \
        will_reconsume_in(State::new_state); \
        m_state = State::new_state;          \
        goto new_state;                      \
    } while (0)

#define SWITCH_TO_RETURN_STATE          \
    do {                                \
        will_switch_to(m_return_state); \
        m_state = m_return_state;       \
        goto _StartOfFunction;          \
    } while (0)

#define RECONSUME_IN_RETURN_STATE          \
    do {                                   \
        will_reconsume_in(m_return_state); \
        m_state = m_return_state;          \
        goto _StartOfFunction;             \
    } while (0)

#define SWITCH_TO_AND_EMIT_CURRENT_TOKEN(new_state) \
    do {                                            \
        will_switch_to(State::new_state);           \
        m_state = State::new_state;                 \
        will_emit(m_current_token);                 \
        m_queued_tokens.enqueue(m_current_token);   \
        return m_queued_tokens.dequeue();           \
    } while (0)

#define EMIT_CHARACTER_AND_RECONSUME_IN(codepoint, new_state) \
    do {                                                      \
        m_queued_tokens.enqueue(m_current_token);             \
        will_reconsume_in(State::new_state);                  \
        m_state = State::new_state;                           \
        goto new_state;                                       \
    } while (0)

#define FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE                               \
    do {                                                                                 \
        for (auto codepoint : m_temporary_buffer) {                                      \
            if (consumed_as_part_of_an_attribute()) {                                    \
                m_current_token.m_tag.attributes.last().value_builder.append(codepoint); \
            } else {                                                                     \
                create_new_token(HTMLToken::Type::Character);                            \
                m_current_token.m_comment_or_character.data.append(codepoint);           \
                m_queued_tokens.enqueue(m_current_token);                                \
            }                                                                            \
        }                                                                                \
    } while (0)

#define DONT_CONSUME_NEXT_INPUT_CHARACTER --m_cursor;

#define ON(codepoint) \
    if (current_input_character.has_value() && current_input_character.value() == codepoint)

#define ON_EOF \
    if (!current_input_character.has_value())

#define ON_ASCII_ALPHA \
    if (current_input_character.has_value() && isalpha(current_input_character.value()))

#define ON_ASCII_ALPHANUMERIC \
    if (current_input_character.has_value() && isalnum(current_input_character.value()))

#define ON_ASCII_UPPER_ALPHA \
    if (current_input_character.has_value() && current_input_character.value() >= 'A' && current_input_character.value() <= 'Z')

#define ON_ASCII_LOWER_ALPHA \
    if (current_input_character.has_value() && current_input_character.value() >= 'a' && current_input_character.value() <= 'z')

#define ON_ASCII_DIGIT \
    if (current_input_character.has_value() && isxdigit(current_input_character.value()))

#define ON_ASCII_HEX_DIGIT \
    if (current_input_character.has_value() && isxdigit(current_input_character.value()))

#define ON_WHITESPACE \
    if (current_input_character.has_value() && (current_input_character.value() == '\t' || current_input_character.value() == '\n' || current_input_character.value() == '\f' || current_input_character.value() == ' '))

#define ANYTHING_ELSE if (1)

#define EMIT_EOF                                      \
    do {                                              \
        if (m_has_emitted_eof)                        \
            return {};                                \
        m_has_emitted_eof = true;                     \
        create_new_token(HTMLToken::Type::EndOfFile); \
        will_emit(m_current_token);                   \
        m_queued_tokens.enqueue(m_current_token);     \
        return m_queued_tokens.dequeue();             \
    } while (0)

#define EMIT_CURRENT_TOKEN                        \
    do {                                          \
        will_emit(m_current_token);               \
        m_queued_tokens.enqueue(m_current_token); \
        return m_queued_tokens.dequeue();         \
    } while (0)

#define EMIT_CHARACTER(codepoint)                                      \
    do {                                                               \
        create_new_token(HTMLToken::Type::Character);                  \
        m_current_token.m_comment_or_character.data.append(codepoint); \
        m_queued_tokens.enqueue(m_current_token);                      \
        return m_queued_tokens.dequeue();                              \
    } while (0)

#define EMIT_CURRENT_CHARACTER \
    EMIT_CHARACTER(current_input_character.value());

#define BEGIN_STATE(state) \
    state:                 \
    case State::state: {   \
        {                  \
            {

#define END_STATE         \
    ASSERT_NOT_REACHED(); \
    break;                \
    }                     \
    }                     \
    }

static inline bool is_surrogate(u32 codepoint)
{
    return (codepoint & 0xfffff800) == 0xd800;
}

static inline bool is_noncharacter(u32 codepoint)
{
    return codepoint >= 0xfdd0 && (codepoint <= 0xfdef || (codepoint & 0xfffe) == 0xfffe) && codepoint <= 0x10ffff;
}

static inline bool is_c0_control(u32 codepoint)
{
    return codepoint <= 0x1f;
}

static inline bool is_control(u32 codepoint)
{
    return is_c0_control(codepoint) || (codepoint >= 0x7f && codepoint <= 0x9f);
}

namespace Web {

Optional<u32> HTMLTokenizer::next_codepoint()
{
    if (m_cursor >= m_input.length())
        return {};
    return m_input[m_cursor++];
}

Optional<u32> HTMLTokenizer::peek_codepoint(size_t offset) const
{
    if ((m_cursor + offset) >= m_input.length())
        return {};
    return m_input[m_cursor + offset];
}

Optional<HTMLToken> HTMLTokenizer::next_token()
{
_StartOfFunction:
    if (!m_queued_tokens.is_empty())
        return m_queued_tokens.dequeue();

    for (;;) {
        auto current_input_character = next_codepoint();
        switch (m_state) {
            BEGIN_STATE(Data)
            {
                ON('&')
                {
                    m_return_state = State::Data;
                    SWITCH_TO(CharacterReference);
                }
                ON('<')
                {
                    SWITCH_TO(TagOpen);
                }
                ON(0)
                {
                    PARSE_ERROR();
                    EMIT_CURRENT_CHARACTER;
                }
                ON_EOF
                {
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(TagOpen)
            {
                ON('!')
                {
                    SWITCH_TO(MarkupDeclarationOpen);
                }
                ON('/')
                {
                    SWITCH_TO(EndTagOpen);
                }
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::StartTag);
                    RECONSUME_IN(TagName);
                }
                ON('?')
                {
                    PARSE_ERROR();
                    create_new_token(HTMLToken::Type::Comment);
                    RECONSUME_IN(BogusComment);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    PARSE_ERROR();
                    EMIT_CHARACTER('<');
                    RECONSUME_IN(Data);
                }
            }
            END_STATE

            BEGIN_STATE(TagName)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(tolower(current_input_character.value()));
                    continue;
                }
                ON(0)
                {
                    PARSE_ERROR();
                    m_current_token.m_tag.tag_name.append("\uFFFD");
                    continue;
                }
                ON_EOF
                {
                    PARSE_ERROR();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(EndTagOpen)
            {
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::EndTag);
                    RECONSUME_IN(TagName);
                }
                ON('>')
                {
                    PARSE_ERROR();
                    SWITCH_TO(Data);
                }
                ON_EOF
                {
                    PARSE_ERROR();
                    // FIXME: Emit a U+003C LESS-THAN SIGN character token, a U+002F SOLIDUS character token and an end-of-file token.
                    continue;
                }
                ANYTHING_ELSE
                {
                    PARSE_ERROR();
                    create_new_token(HTMLToken::Type::Comment);
                    RECONSUME_IN(BogusComment);
                }
            }
            END_STATE

            BEGIN_STATE(MarkupDeclarationOpen)
            {
                DONT_CONSUME_NEXT_INPUT_CHARACTER;
                if (consume_next_if_match("--")) {
                    create_new_token(HTMLToken::Type::Comment);
                    SWITCH_TO(CommentStart);
                }
                if (consume_next_if_match("DOCTYPE", CaseSensitivity::CaseInsensitive)) {
                    SWITCH_TO(DOCTYPE);
                }
            }
            END_STATE

            BEGIN_STATE(BogusComment)
            {
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ON(0)
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPE)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeDOCTYPEName);
                }
                ON('>')
                {
                    RECONSUME_IN(BeforeDOCTYPEName);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(BeforeDOCTYPEName)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON_ASCII_UPPER_ALPHA
                {
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_token.m_doctype.name.append(tolower(current_input_character.value()));
                    SWITCH_TO(DOCTYPEName);
                }
                ON(0)
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_token.m_doctype.name.append(current_input_character.value());
                    SWITCH_TO(DOCTYPEName);
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEName)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(AfterDOCTYPEName);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_doctype.name.append(tolower(current_input_character.value()));
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.name.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AfterDOCTYPEName)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    if (toupper(current_input_character.value()) == 'P' && consume_next_if_match("UBLIC", CaseSensitivity::CaseInsensitive)) {
                        SWITCH_TO(AfterDOCTYPEPublicKeyword);
                    }
                    if (toupper(current_input_character.value()) == 'S' && consume_next_if_match("YSTEM", CaseSensitivity::CaseInsensitive)) {
                        SWITCH_TO(AfterDOCTYPESystemKeyword);
                    }
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(AfterDOCTYPEPublicKeyword)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeDOCTYPEPublicIdentifier);
                }
                ON('"')
                {
                    TODO();
                }
                ON('\'')
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(AfterDOCTYPESystemKeyword)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeDOCTYPESystemIdentifier);
                }
                ON('"')
                {
                    TODO();
                }
                ON('\'')
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(BeforeDOCTYPEPublicIdentifier)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('"')
                {
                    m_current_token.m_doctype.public_identifier.clear();
                    SWITCH_TO(DOCTYPEPublicIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.public_identifier.clear();
                    SWITCH_TO(DOCTYPEPublicIdentifierSingleQuoted);
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(BeforeDOCTYPESystemIdentifier)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('"')
                {
                    m_current_token.m_doctype.system_identifier.clear();
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.system_identifier.clear();
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEPublicIdentifierDoubleQuoted)
            {
                ON('"')
                {
                    SWITCH_TO(AfterDOCTYPEPublicIdentifier);
                }
                ON(0)
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.public_identifier.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEPublicIdentifierSingleQuoted)
            {
                ON('\'')
                {
                    SWITCH_TO(AfterDOCTYPEPublicIdentifier);
                }
                ON(0)
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.public_identifier.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPESystemIdentifierDoubleQuoted)
            {
                ON('"')
                {
                    SWITCH_TO(AfterDOCTYPESystemIdentifier);
                }
                ON(0)
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.system_identifier.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPESystemIdentifierSingleQuoted)
            {
                ON('\'')
                {
                    SWITCH_TO(AfterDOCTYPESystemIdentifier);
                }
                ON(0)
                {
                    TODO();
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.system_identifier.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AfterDOCTYPEPublicIdentifier)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BetweenDOCTYPEPublicAndSystemIdentifiers);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON('"')
                {
                    TODO();
                }
                ON('\'')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(BetweenDOCTYPEPublicAndSystemIdentifiers)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON('"')
                {
                    m_current_token.m_doctype.system_identifier.clear();
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.system_identifier.clear();
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(AfterDOCTYPESystemIdentifier)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(BeforeAttributeName)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('/')
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('>')
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON_EOF
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('=')
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.append(HTMLToken::AttributeBuilder());
                    RECONSUME_IN(AttributeName);
                }
            }
            END_STATE

            BEGIN_STATE(SelfClosingStartTag)
            {
                ON('>')
                {
                    m_current_token.m_tag.self_closing = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(AttributeName)
            {
                ON_WHITESPACE
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('/')
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('>')
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON_EOF
                {
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('=')
                {
                    SWITCH_TO(BeforeAttributeValue);
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.last().name_builder.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AfterAttributeName)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('/')
                {
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('=')
                {
                    SWITCH_TO(BeforeAttributeValue);
                }
                ON('>')
                {
                    SWITCH_TO(Data);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.append(HTMLToken::AttributeBuilder());
                    RECONSUME_IN(AttributeName);
                }
            }
            END_STATE

            BEGIN_STATE(BeforeAttributeValue)
            {
                ON_WHITESPACE
                {
                    continue;
                }
                ON('"')
                {
                    SWITCH_TO(AttributeValueDoubleQuoted);
                }
                ON('\'')
                {
                    SWITCH_TO(AttributeValueSingleQuoted);
                }
                ON('>')
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(AttributeValueUnquoted);
                }
            }
            END_STATE

            BEGIN_STATE(AttributeValueDoubleQuoted)
            {
                ON('"')
                {
                    SWITCH_TO(AfterAttributeValueQuoted);
                }
                ON('&')
                {
                    m_return_state = State::AttributeValueDoubleQuoted;
                    SWITCH_TO(CharacterReference);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.last().value_builder.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AttributeValueSingleQuoted)
            {
                ON('\'')
                {
                    SWITCH_TO(AfterAttributeValueQuoted);
                }
                ON('&')
                {
                    m_return_state = State::AttributeValueSingleQuoted;
                    SWITCH_TO(CharacterReference);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.last().value_builder.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AttributeValueUnquoted)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('&')
                {
                    m_return_state = State::AttributeValueUnquoted;
                    SWITCH_TO(CharacterReference);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.last().value_builder.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AfterAttributeValueQuoted)
            {
                ON_WHITESPACE
                {
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    PARSE_ERROR();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    PARSE_ERROR();
                    RECONSUME_IN(BeforeAttributeName);
                }
            }
            END_STATE

            BEGIN_STATE(CommentStart)
            {
                ON('-')
                {
                    SWITCH_TO(CommentStartDash);
                }
                ON('>')
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentStartDash)
            {
                ON('-')
                {
                    SWITCH_TO(CommentEnd);
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_comment_or_character.data.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(Comment)
            {
                ON('<')
                {
                    m_current_token.m_comment_or_character.data.append(current_input_character.value());
                    SWITCH_TO(CommentLessThanSign);
                }
                ON('-')
                {
                    SWITCH_TO(CommentEndDash);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_comment_or_character.data.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(CommentEnd)
            {
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON('!')
                {
                    SWITCH_TO(CommentEndBang);
                }
                ON('-')
                {
                    m_current_token.m_comment_or_character.data.append('-');
                    continue;
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_comment_or_character.data.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentEndBang)
            {
                ON('-')
                {
                    m_current_token.m_comment_or_character.data.append("--!");
                    SWITCH_TO(CommentEndDash);
                }
                ON('>')
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_comment_or_character.data.append("--!");
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentEndDash)
            {
                ON('-')
                {
                    SWITCH_TO(CommentEnd);
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_comment_or_character.data.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSign)
            {
                ON('!')
                {
                    m_current_token.m_comment_or_character.data.append(current_input_character.value());
                    SWITCH_TO(CommentLessThanSignBang);
                }
                ON('<')
                {
                    m_current_token.m_comment_or_character.data.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSignBang)
            {
                ON('-')
                {
                    SWITCH_TO(CommentLessThanSignBangDash);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSignBangDash)
            {
                ON('-')
                {
                    SWITCH_TO(CommentLessThanSignBangDashDash);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSignBangDashDash)
            {
                ON('>')
                {
                    SWITCH_TO(CommentEnd);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(CharacterReference)
            {
                m_temporary_buffer.clear();
                m_temporary_buffer.append('&');

                ON_ASCII_ALPHANUMERIC
                {
                    RECONSUME_IN(NamedCharacterReference);
                }
                ON('#')
                {
                    m_temporary_buffer.append(current_input_character.value());
                    SWITCH_TO(NumericCharacterReference);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN_RETURN_STATE;
                }
            }
            END_STATE

            BEGIN_STATE(NamedCharacterReference)
            {
                auto match = HTML::codepoints_from_entity(m_input.substring_view(m_cursor - 1, m_input.length() - m_cursor + 1));

                if (match.has_value()) {
                    m_cursor += match.value().entity.length();
                    for (auto ch : match.value().entity)
                        m_temporary_buffer.append(ch);

                    if (consumed_as_part_of_an_attribute() && match.value().entity.ends_with(';')) {
                        auto next_codepoint = peek_codepoint(0);
                        if (next_codepoint.has_value() && next_codepoint.value() == '=') {
                            FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                            SWITCH_TO_RETURN_STATE;
                        }
                    }

                    if (!match.value().entity.ends_with(';')) {
                        PARSE_ERROR();
                    }

                    m_temporary_buffer.clear();
                    m_temporary_buffer.append(match.value().codepoints);

                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    SWITCH_TO_RETURN_STATE;
                } else {
                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    SWITCH_TO(AmbiguousAmpersand);
                }
            }
            END_STATE

            BEGIN_STATE(AmbiguousAmpersand)
            {
                ON_ASCII_ALPHANUMERIC
                {
                    if (consumed_as_part_of_an_attribute()) {
                        m_current_token.m_tag.attributes.last().value_builder.append(current_input_character.value());
                        continue;
                    } else {
                        EMIT_CURRENT_CHARACTER;
                    }
                }
                ON(';')
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN_RETURN_STATE;
                }
            }
            END_STATE

            BEGIN_STATE(NumericCharacterReference)
            {
                m_character_reference_code = 0;

                ON('X')
                {
                    m_temporary_buffer.append(current_input_character.value());
                    SWITCH_TO(HexadecimalCharacterReferenceStart);
                }
                ON('x')
                {
                    m_temporary_buffer.append(current_input_character.value());
                    SWITCH_TO(HexadecimalCharacterReferenceStart);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(DecimalCharacterReferenceStart);
                }
            }
            END_STATE

            BEGIN_STATE(HexadecimalCharacterReferenceStart)
            {
                ON_ASCII_HEX_DIGIT
                {
                    RECONSUME_IN(HexadecimalCharacterReference);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(DecimalCharacterReferenceStart)
            {
                ON_ASCII_DIGIT
                {
                    RECONSUME_IN(DecimalCharacterReference);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(HexadecimalCharacterReference)
            {
                ON_ASCII_DIGIT
                {
                    m_character_reference_code *= 16;
                    m_character_reference_code += current_input_character.value() - 0x30;
                    continue;
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_character_reference_code *= 16;
                    m_character_reference_code += current_input_character.value() - 0x37;
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_character_reference_code *= 16;
                    m_character_reference_code += current_input_character.value() - 0x57;
                    continue;
                }
                ON(';')
                {
                    SWITCH_TO(NumericCharacterReferenceEnd);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(DecimalCharacterReference)
            {
                ON_ASCII_DIGIT
                {
                    m_character_reference_code *= 10;
                    m_character_reference_code += current_input_character.value() - 0x30;
                    continue;
                }
                ON(';')
                {
                    SWITCH_TO(NumericCharacterReferenceEnd);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(NumericCharacterReferenceEnd)
            {
                if (m_character_reference_code == 0) {
                    TODO();
                }
                if (m_character_reference_code > 0x10ffff) {
                    TODO();
                }
                if (is_surrogate(m_character_reference_code)) {
                    TODO();
                }
                if (is_noncharacter(m_character_reference_code)) {
                    TODO();
                }
                if (m_character_reference_code == 0xd || (is_control(m_character_reference_code) && !isspace(m_character_reference_code))) {
                    TODO();
                }

                if (is_control(m_character_reference_code)) {
                    constexpr struct {
                        u32 number;
                        u32 codepoint;
                    } conversion_table[] = {
                        { 0x80, 0x20AC },
                        { 0x82, 0x201A },
                        { 0x83, 0x0192 },
                        { 0x84, 0x201E },
                        { 0x85, 0x2026 },
                        { 0x86, 0x2020 },
                        { 0x87, 0x2021 },
                        { 0x88, 0x02C6 },
                        { 0x89, 0x2030 },
                        { 0x8A, 0x0160 },
                        { 0x8B, 0x2039 },
                        { 0x8C, 0x0152 },
                        { 0x8E, 0x017D },
                        { 0x91, 0x2018 },
                        { 0x92, 0x2019 },
                        { 0x93, 0x201C },
                        { 0x94, 0x201D },
                        { 0x95, 0x2022 },
                        { 0x96, 0x2013 },
                        { 0x97, 0x2014 },
                        { 0x98, 0x02DC },
                        { 0x99, 0x2122 },
                        { 0x9A, 0x0161 },
                        { 0x9B, 0x203A },
                        { 0x9C, 0x0153 },
                        { 0x9E, 0x017E },
                        { 0x9F, 0x0178 },
                    };
                    for (auto& entry : conversion_table) {
                        if (m_character_reference_code == entry.number) {
                            m_character_reference_code = entry.codepoint;
                            break;
                        }
                    }
                }

                m_temporary_buffer.clear();
                m_temporary_buffer.append(m_character_reference_code);
                FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                SWITCH_TO_RETURN_STATE;
            }
            END_STATE

            BEGIN_STATE(RCDATA)
            {
                ON('&')
                {
                    m_return_state = State::RCDATA;
                    SWITCH_TO(CharacterReference);
                }
                ON('<')
                {
                    SWITCH_TO(RCDATALessThanSign);
                }
                ON(0)
                {
                    PARSE_ERROR();
                    EMIT_CHARACTER("\uFFFD");
                }
                ON_EOF
                {
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(RCDATALessThanSign)
            {
                ON('/')
                {
                    m_temporary_buffer.clear();
                    SWITCH_TO(RCDATAEndTagOpen);
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER('<');
                    RECONSUME_IN(RCDATA);
                }
            }
            END_STATE

            BEGIN_STATE(RCDATAEndTagOpen)
            {
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::EndTag);
                    RECONSUME_IN(RCDATAEndTagName);
                }
                ANYTHING_ELSE
                {
                    // FIXME: Emit a U+003C LESS-THAN SIGN character token and a U+002F SOLIDUS character token. Reconsume in the RCDATA state.
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    RECONSUME_IN(RCDATA);
                }
            }
            END_STATE

            BEGIN_STATE(RCDATAEndTagName)
            {
                ON_WHITESPACE
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto codepoint : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                    RECONSUME_IN(RCDATA);
                }
            }
            END_STATE

            BEGIN_STATE(RAWTEXT)
            {
                ON('<')
                {
                    SWITCH_TO(RAWTEXTLessThanSign);
                }
                ON(0)
                {
                    PARSE_ERROR();
                    EMIT_CHARACTER("\uFFFD");
                }
                ON_EOF
                {
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(RAWTEXTLessThanSign)
            {
                ON('/')
                {
                    m_temporary_buffer.clear();
                    SWITCH_TO(RAWTEXTEndTagOpen);
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER('<');
                    RECONSUME_IN(RAWTEXT);
                }
            }
            END_STATE

            BEGIN_STATE(RAWTEXTEndTagOpen)
            {
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::EndTag);
                    RECONSUME_IN(RAWTEXTEndTagName);
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    RECONSUME_IN(RAWTEXT);
                }
            }
            END_STATE

            BEGIN_STATE(RAWTEXTEndTagName)
            {
                ON_WHITESPACE
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto codepoint : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto codepoint : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                    RECONSUME_IN(RAWTEXT);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptData)
            {
                ON('<')
                {
                    SWITCH_TO(ScriptDataLessThanSign);
                }
                ON(0)
                {
                    PARSE_ERROR();
                    EMIT_CHARACTER("\uFFFD");
                }
                ON_EOF
                {
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(PLAINTEXT)
            {
                ON(0)
                {
                    PARSE_ERROR();
                    EMIT_CHARACTER("\uFFFD");
                }
                ON_EOF
                {
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataLessThanSign)
            {
                ON('/')
                {
                    m_temporary_buffer.clear();
                    SWITCH_TO(ScriptDataEndTagOpen);
                }
                ON('!')
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('!'));
                    SWITCH_TO(ScriptDataEscapeStart);
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', ScriptData);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapeStart)
            {
                ON('-')
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('-'));
                    SWITCH_TO(ScriptDataEscapeStartDash);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(ScriptData);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapeStartDash)
            {
                ON('-')
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('-'));
                    SWITCH_TO(ScriptDataEscapedDashDash);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(ScriptData);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedDashDash)
            {
                ON('-')
                {
                    EMIT_CHARACTER('-');
                }
                ON('<')
                {
                    SWITCH_TO(ScriptDataEscapedLessThanSign);
                }
                ON('>')
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('>'));
                    SWITCH_TO(ScriptData);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedLessThanSign)
            {
                ON('/')
                {
                    m_temporary_buffer.clear();
                    SWITCH_TO(ScriptDataEscapedEndTagOpen);
                }
                ON_ASCII_ALPHA
                {
                    m_temporary_buffer.clear();
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', ScriptDataDoubleEscapeStart);
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedEndTagOpen)
            {
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::EndTag);
                    RECONSUME_IN(ScriptDataEscapedEndTagName);
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    RECONSUME_IN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedEndTagName)
            {
                ON_WHITESPACE
                {
                    if (current_end_tag_token_is_appropriate()) {
                        SWITCH_TO(BeforeAttributeName);
                    } else {
                        TODO();
                    }
                }
                ON('/')
                {
                    if (current_end_tag_token_is_appropriate()) {
                        SWITCH_TO(SelfClosingStartTag);
                    } else {
                        TODO();
                    }
                }
                ON('>')
                {
                    if (current_end_tag_token_is_appropriate()) {
                        SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                    } else {
                        TODO();
                    }
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto codepoint : m_temporary_buffer) {
                        m_queued_tokens.enqueue(HTMLToken::make_character(codepoint));
                    }
                    RECONSUME_IN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapeStart)
            {
                TODO();
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedDash)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(ScriptDataEscapedDashDash);
                }
                ON('<')
                {
                    SWITCH_TO(ScriptDataEscapedLessThanSign);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscaped)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(ScriptDataEscapedDash);
                }
                ON('<')
                {
                    SWITCH_TO(ScriptDataEscapedLessThanSign);
                }
                ON(0)
                {
                    TODO();
                }
                ON_EOF
                {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEndTagOpen)
            {
                ON_ASCII_ALPHA
                {
                    create_new_token(HTMLToken::Type::EndTag);
                    RECONSUME_IN(ScriptDataEndTagName);
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEndTagName)
            {
                ON_WHITESPACE
                {
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(BeforeAttributeName);
                    // FIXME: Otherwise, treat it as per the "anything else" entry below.
                    TODO();
                }
                ON('/')
                {
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(SelfClosingStartTag);
                    // FIXME: Otherwise, treat it as per the "anything else" entry below.
                    TODO();
                }
                ON('>')
                {
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                    // FIXME: Otherwise, treat it as per the "anything else" entry below.
                    TODO();
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    TODO();
                }
            }
            END_STATE

        default:
            TODO();
        }
    }
}

bool HTMLTokenizer::consume_next_if_match(const StringView& string, CaseSensitivity case_sensitivity)
{
    for (size_t i = 0; i < string.length(); ++i) {
        auto codepoint = peek_codepoint(i);
        if (!codepoint.has_value())
            return false;
        // FIXME: This should be more Unicode-aware.
        if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
            if (codepoint.value() < 0x80) {
                if (tolower(codepoint.value()) != tolower(string[i]))
                    return false;
                continue;
            }
        }
        if (codepoint.value() != (u32)string[i])
            return false;
    }
    m_cursor += string.length();
    return true;
}

void HTMLTokenizer::create_new_token(HTMLToken::Type type)
{
    m_current_token = {};
    m_current_token.m_type = type;
}

HTMLTokenizer::HTMLTokenizer(const StringView& input, const String& encoding)
{
    auto* decoder = TextCodec::decoder_for(encoding);
    ASSERT(decoder);
    m_decoded_input = decoder->to_utf8(input);
    m_input = m_decoded_input;
}

void HTMLTokenizer::will_switch_to([[maybe_unused]] State new_state)
{
#ifdef TOKENIZER_TRACE
    dbg() << "[" << state_name(m_state) << "] Switch to " << state_name(new_state);
#endif
}

void HTMLTokenizer::will_reconsume_in([[maybe_unused]] State new_state)
{
#ifdef TOKENIZER_TRACE
    dbg() << "[" << state_name(m_state) << "] Reconsume in " << state_name(new_state);
#endif
}

void HTMLTokenizer::switch_to(Badge<HTMLDocumentParser>, State new_state)
{
#ifdef TOKENIZER_TRACE
    dbg() << "[" << state_name(m_state) << "] Parser switches tokenizer state to " << state_name(new_state);
#endif
    m_state = new_state;
}

void HTMLTokenizer::will_emit(HTMLToken& token)
{
    if (token.is_start_tag())
        m_last_emitted_start_tag = token;
}

bool HTMLTokenizer::current_end_tag_token_is_appropriate() const
{
    ASSERT(m_current_token.is_end_tag());
    if (!m_last_emitted_start_tag.is_start_tag())
        return false;
    return m_current_token.tag_name() == m_last_emitted_start_tag.tag_name();
}

bool HTMLTokenizer::consumed_as_part_of_an_attribute() const
{
    return m_return_state == State::AttributeValueUnquoted || m_return_state == State::AttributeValueSingleQuoted || m_return_state == State::AttributeValueDoubleQuoted;
}

}
