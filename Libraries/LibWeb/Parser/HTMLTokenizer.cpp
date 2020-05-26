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

#include <LibWeb/Parser/HTMLToken.h>
#include <LibWeb/Parser/HTMLTokenizer.h>
#include <ctype.h>

#pragma GCC diagnostic ignored "-Wunused-label"

//#define TOKENIZER_TRACE

#define TODO()                                                                                              \
    do {                                                                                                    \
        dbg() << "[TODO: " << state_name(m_state) << "] '" << (char)current_input_character.value() << "'"; \
        ASSERT_NOT_REACHED();                                                                               \
    } while (0)

#define SWITCH_TO(new_state)                        \
    do {                                            \
        will_switch_to(State::new_state);           \
        m_state = State::new_state;                 \
        current_input_character = next_codepoint(); \
        goto new_state;                             \
    } while (0)

#define RECONSUME_IN(new_state)              \
    do {                                     \
        will_reconsume_in(State::new_state); \
        m_state = State::new_state;          \
        goto new_state;                      \
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

#define DONT_CONSUME_NEXT_INPUT_CHARACTER --m_cursor;

#define ON(codepoint) \
    if (current_input_character.has_value() && current_input_character.value() == codepoint)

#define ON_EOF \
    if (!current_input_character.has_value())

#define ON_ASCII_ALPHA \
    if (current_input_character.has_value() && isalpha(current_input_character.value()))

#define ON_ASCII_UPPER_ALPHA \
    if (current_input_character.has_value() && current_input_character.value() >= 'A' && current_input_character.value() <= 'Z')

#define ON_ASCII_LOWER_ALPHA \
    if (current_input_character.has_value() && current_input_character.value() >= 'a' && current_input_character.value() <= 'z')

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
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
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
                    TODO();
                }
                ANYTHING_ELSE
                {
                    TODO();
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
                    TODO();
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
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(RCDATAEndTagName)
            {
                ON_WHITESPACE
                {
                    TODO();
                }
                ON('/')
                {
                    TODO();
                }
                ON('>')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        // FIXME: Otherwise, treat it as per the "anything else" entry below.
                        TODO();
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
                    TODO();
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
                    TODO();
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
                    // FIXME: Emit a U+003C LESS-THAN SIGN character token and a U+002F SOLIDUS character token. Reconsume in the RAWTEXT state.
                    TODO();
                }
            }
            END_STATE

            BEGIN_STATE(RAWTEXTEndTagName)
            {
                ON_WHITESPACE
                {
                    TODO();
                }
                ON('/')
                {
                    TODO();
                }
                ON('>')
                {
                    if (!current_end_tag_token_is_appropriate()) {
                        // FIXME: Otherwise, treat it as per the "anything else" entry below.
                        TODO();
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
                    TODO();
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
                    TODO();
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
                    TODO();
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', ScriptData);
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

HTMLTokenizer::HTMLTokenizer(const StringView& input)
    : m_input(input)
{
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
}
