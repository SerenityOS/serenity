/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/SourceLocation.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/HTML/Parser/Entities.h>
#include <LibWeb/HTML/Parser/HTMLToken.h>
#include <LibWeb/HTML/Parser/HTMLTokenizer.h>
#include <ctype.h>
#include <string.h>

namespace Web::HTML {

#pragma GCC diagnostic ignored "-Wunused-label"

#define CONSUME_NEXT_INPUT_CHARACTER \
    current_input_character = next_code_point();

#define SWITCH_TO(new_state)                       \
    do {                                           \
        VERIFY(m_current_builder.is_empty());      \
        SWITCH_TO_WITH_UNCLEAN_BUILDER(new_state); \
    } while (0)

#define SWITCH_TO_WITH_UNCLEAN_BUILDER(new_state) \
    do {                                          \
        will_switch_to(State::new_state);         \
        m_state = State::new_state;               \
        CONSUME_NEXT_INPUT_CHARACTER;             \
        goto new_state;                           \
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

#define RECONSUME_IN_RETURN_STATE                \
    do {                                         \
        will_reconsume_in(m_return_state);       \
        m_state = m_return_state;                \
        if (current_input_character.has_value()) \
            restore_to(m_prev_utf8_iterator);    \
        goto _StartOfFunction;                   \
    } while (0)

#define SWITCH_TO_AND_EMIT_CURRENT_TOKEN(new_state)     \
    do {                                                \
        will_switch_to(State::new_state);               \
        m_state = State::new_state;                     \
        will_emit(m_current_token);                     \
        m_queued_tokens.enqueue(move(m_current_token)); \
        return m_queued_tokens.dequeue();               \
    } while (0)

#define EMIT_CHARACTER_AND_RECONSUME_IN(code_point, new_state)          \
    do {                                                                \
        m_queued_tokens.enqueue(HTMLToken::make_character(code_point)); \
        will_reconsume_in(State::new_state);                            \
        m_state = State::new_state;                                     \
        goto new_state;                                                 \
    } while (0)

#define FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE                               \
    do {                                                                                 \
        for (auto code_point : m_temporary_buffer) {                                     \
            if (consumed_as_part_of_an_attribute()) {                                    \
                m_current_builder.append_code_point(code_point);                         \
            } else {                                                                     \
                create_new_token(HTMLToken::Type::Character);                            \
                m_current_builder.append_code_point(code_point);                         \
                m_current_token.m_comment_or_character.data = consume_current_builder(); \
                m_queued_tokens.enqueue(move(m_current_token));                          \
            }                                                                            \
        }                                                                                \
    } while (0)

#define DONT_CONSUME_NEXT_INPUT_CHARACTER \
    do {                                  \
        restore_to(m_prev_utf8_iterator); \
    } while (0)

#define ON(code_point) \
    if (current_input_character.has_value() && current_input_character.value() == code_point)

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
    if (current_input_character.has_value() && isdigit(current_input_character.value()))

#define ON_ASCII_HEX_DIGIT \
    if (current_input_character.has_value() && isxdigit(current_input_character.value()))

#define ON_WHITESPACE \
    if (current_input_character.has_value() && strchr("\t\n\f ", current_input_character.value()))

#define ANYTHING_ELSE if (1)

#define EMIT_EOF                                        \
    do {                                                \
        if (m_has_emitted_eof)                          \
            return {};                                  \
        m_has_emitted_eof = true;                       \
        create_new_token(HTMLToken::Type::EndOfFile);   \
        will_emit(m_current_token);                     \
        m_queued_tokens.enqueue(move(m_current_token)); \
        return m_queued_tokens.dequeue();               \
    } while (0)

#define EMIT_CURRENT_TOKEN                              \
    do {                                                \
        will_emit(m_current_token);                     \
        m_queued_tokens.enqueue(move(m_current_token)); \
        return m_queued_tokens.dequeue();               \
    } while (0)

#define EMIT_CHARACTER(code_point)                                               \
    do {                                                                         \
        create_new_token(HTMLToken::Type::Character);                            \
        m_current_builder.append_code_point(code_point);                         \
        m_current_token.m_comment_or_character.data = consume_current_builder(); \
        m_queued_tokens.enqueue(move(m_current_token));                          \
        return m_queued_tokens.dequeue();                                        \
    } while (0)

#define EMIT_CURRENT_CHARACTER \
    EMIT_CHARACTER(current_input_character.value());

#define SWITCH_TO_AND_EMIT_CHARACTER(code_point, new_state) \
    do {                                                    \
        will_switch_to(State::new_state);                   \
        m_state = State::new_state;                         \
        EMIT_CHARACTER(code_point);                         \
    } while (0)

#define SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(new_state) \
    SWITCH_TO_AND_EMIT_CHARACTER(current_input_character.value(), new_state)

#define BEGIN_STATE(state) \
    state:                 \
    case State::state: {   \
        {                  \
            {

#define END_STATE         \
    VERIFY_NOT_REACHED(); \
    break;                \
    }                     \
    }                     \
    }

static inline void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln_if(TOKENIZER_TRACE_DEBUG, "Parse error (tokenization) {}", location);
}

static inline bool is_surrogate(u32 code_point)
{
    return (code_point & 0xfffff800) == 0xd800;
}

static inline bool is_noncharacter(u32 code_point)
{
    return code_point >= 0xfdd0 && (code_point <= 0xfdef || (code_point & 0xfffe) == 0xfffe) && code_point <= 0x10ffff;
}

static inline bool is_c0_control(u32 code_point)
{
    return code_point <= 0x1f;
}

static inline bool is_control(u32 code_point)
{
    return is_c0_control(code_point) || (code_point >= 0x7f && code_point <= 0x9f);
}

Optional<u32> HTMLTokenizer::next_code_point()
{
    if (m_utf8_iterator == m_utf8_view.end())
        return {};
    skip(1);
    dbgln_if(TOKENIZER_TRACE_DEBUG, "(Tokenizer) Next code_point: {}", (char)*m_prev_utf8_iterator);
    return *m_prev_utf8_iterator;
}

void HTMLTokenizer::skip(size_t count)
{
    m_source_positions.append(m_source_positions.last());
    for (size_t i = 0; i < count; ++i) {
        m_prev_utf8_iterator = m_utf8_iterator;
        auto code_point = *m_utf8_iterator;
        if (code_point == '\n') {
            m_source_positions.last().column = 0;
            m_source_positions.last().line++;
        } else {
            m_source_positions.last().column++;
        }
        ++m_utf8_iterator;
    }
}

Optional<u32> HTMLTokenizer::peek_code_point(size_t offset) const
{
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < offset && it != m_utf8_view.end(); ++i)
        ++it;
    if (it == m_utf8_view.end())
        return {};
    return *it;
}

Optional<HTMLToken> HTMLTokenizer::next_token()
{
    {
        auto last_position = m_source_positions.last();
        m_source_positions.clear();
        m_source_positions.append(move(last_position));
    }
_StartOfFunction:
    if (!m_queued_tokens.is_empty())
        return m_queued_tokens.dequeue();

    for (;;) {
        auto current_input_character = next_code_point();
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
                    log_parse_error();
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
                    log_parse_error();
                    create_new_token(HTMLToken::Type::Comment);
                    RECONSUME_IN(BogusComment);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', Data);
                }
            }
            END_STATE

            BEGIN_STATE(TagName)
            {
                ON_WHITESPACE
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    m_current_token.m_end_position = nth_last_position(1);
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    m_current_token.m_end_position = nth_last_position(1);
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    m_current_token.m_end_position = nth_last_position(1);
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_current_token.m_end_position = nth_last_position(0);
                    continue;
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    m_current_token.m_end_position = nth_last_position(0);
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_end_position = nth_last_position(1);
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    m_current_token.m_end_position = nth_last_position(0);
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
                    log_parse_error();
                    SWITCH_TO(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
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
                if (consume_next_if_match("[CDATA[")) {
                    TODO();
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    create_new_token(HTMLToken::Type::Comment);
                    SWITCH_TO(BogusComment);
                }
            }
            END_STATE

            BEGIN_STATE(BogusComment)
            {
                ON('>')
                {
                    m_current_token.m_comment_or_character.data = consume_current_builder();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
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
                    log_parse_error();
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    RECONSUME_IN(BeforeDOCTYPEName);
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
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_current_token.m_doctype.missing_name = false;
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(DOCTYPEName);
                }
                ON(0)
                {
                    log_parse_error();
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_builder.append_code_point(0xFFFD);
                    m_current_token.m_doctype.missing_name = false;
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(DOCTYPEName);
                }
                ON('>')
                {
                    log_parse_error();
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    create_new_token(HTMLToken::Type::DOCTYPE);
                    m_current_builder.append_code_point(current_input_character.value());
                    m_current_token.m_doctype.missing_name = false;
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(DOCTYPEName);
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEName)
            {
                ON_WHITESPACE
                {
                    m_current_token.m_doctype.name = consume_current_builder();
                    SWITCH_TO(AfterDOCTYPEName);
                }
                ON('>')
                {
                    m_current_token.m_doctype.name = consume_current_builder();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    continue;
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
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
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    if (toupper(current_input_character.value()) == 'P' && consume_next_if_match("UBLIC", CaseSensitivity::CaseInsensitive)) {
                        SWITCH_TO(AfterDOCTYPEPublicKeyword);
                    }
                    if (toupper(current_input_character.value()) == 'S' && consume_next_if_match("YSTEM", CaseSensitivity::CaseInsensitive)) {
                        SWITCH_TO(AfterDOCTYPESystemKeyword);
                    }
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    log_parse_error();
                    m_current_token.m_doctype.missing_public_identifier = false;
                    SWITCH_TO(DOCTYPEPublicIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    log_parse_error();
                    m_current_token.m_doctype.missing_public_identifier = false;
                    SWITCH_TO(DOCTYPEPublicIdentifierSingleQuoted);
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    log_parse_error();
                    m_current_token.m_doctype.system_identifier = {};
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    log_parse_error();
                    m_current_token.m_doctype.system_identifier = {};
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    m_current_token.m_doctype.missing_public_identifier = false;
                    SWITCH_TO(DOCTYPEPublicIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.missing_public_identifier = false;
                    SWITCH_TO(DOCTYPEPublicIdentifierSingleQuoted);
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEPublicIdentifierDoubleQuoted)
            {
                ON('"')
                {
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    SWITCH_TO(AfterDOCTYPEPublicIdentifier);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPEPublicIdentifierSingleQuoted)
            {
                ON('\'')
                {
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    SWITCH_TO(AfterDOCTYPEPublicIdentifier);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPESystemIdentifierDoubleQuoted)
            {
                ON('"')
                {
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    SWITCH_TO(AfterDOCTYPESystemIdentifier);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.public_identifier = consume_current_builder();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(DOCTYPESystemIdentifierSingleQuoted)
            {
                ON('\'')
                {
                    m_current_token.m_doctype.system_identifier = consume_current_builder();
                    SWITCH_TO(AfterDOCTYPESystemIdentifier);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('>')
                {
                    log_parse_error();
                    m_current_token.m_doctype.system_identifier = consume_current_builder();
                    m_current_token.m_doctype.force_quirks = true;
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
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
                    log_parse_error();
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    log_parse_error();
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierDoubleQuoted);
                }
                ON('\'')
                {
                    m_current_token.m_doctype.missing_system_identifier = false;
                    SWITCH_TO(DOCTYPESystemIdentifierSingleQuoted);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    RECONSUME_IN(BogusDOCTYPE);
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
                    log_parse_error();
                    m_current_token.m_doctype.force_quirks = true;
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    RECONSUME_IN(BogusDOCTYPE);
                }
            }
            END_STATE

            BEGIN_STATE(BogusDOCTYPE)
            {
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON(0)
                {
                    log_parse_error();
                    continue;
                }
                ON_EOF
                {
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    continue;
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
                    if (!m_current_token.m_tag.attributes.is_empty())
                        m_current_token.m_tag.attributes.last().name_end_position = nth_last_position(1);
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
                    log_parse_error();
                    auto new_attribute = HTMLToken::AttributeBuilder();
                    new_attribute.name_start_position = nth_last_position(1);
                    m_current_builder.append_code_point(current_input_character.value());
                    m_current_token.m_tag.attributes.append(new_attribute);
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(AttributeName);
                }
                ANYTHING_ELSE
                {
                    auto new_attribute = HTMLToken::AttributeBuilder();
                    new_attribute.name_start_position = nth_last_position(1);
                    m_current_token.m_tag.attributes.append(move(new_attribute));
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
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    RECONSUME_IN(BeforeAttributeName);
                }
            }
            END_STATE

            BEGIN_STATE(AttributeName)
            {
                ON_WHITESPACE
                {
                    m_current_token.m_tag.attributes.last().local_name = consume_current_builder();
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('/')
                {
                    m_current_token.m_tag.attributes.last().local_name = consume_current_builder();
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('>')
                {
                    m_current_token.m_tag.attributes.last().local_name = consume_current_builder();
                    RECONSUME_IN(AfterAttributeName);
                }
                ON_EOF
                {
                    m_current_token.m_tag.attributes.last().local_name = consume_current_builder();
                    RECONSUME_IN(AfterAttributeName);
                }
                ON('=')
                {
                    m_current_token.m_tag.attributes.last().local_name = consume_current_builder();
                    SWITCH_TO(BeforeAttributeValue);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append_code_point(tolower(current_input_character.value()));
                    continue;
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('"')
                {
                    log_parse_error();
                    goto AnythingElseAttributeName;
                }
                ON('\'')
                {
                    log_parse_error();
                    goto AnythingElseAttributeName;
                }
                ON('<')
                {
                    log_parse_error();
                    goto AnythingElseAttributeName;
                }
                ANYTHING_ELSE
                {
                AnythingElseAttributeName:
                    m_current_builder.append_code_point(current_input_character.value());
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
                    m_current_token.m_tag.attributes.last().name_end_position = nth_last_position(1);
                    SWITCH_TO(BeforeAttributeValue);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.attributes.append(HTMLToken::AttributeBuilder());
                    m_current_token.m_tag.attributes.last().name_start_position = m_source_positions.last();
                    RECONSUME_IN(AttributeName);
                }
            }
            END_STATE

            BEGIN_STATE(BeforeAttributeValue)
            {
                m_current_token.m_tag.attributes.last().value_start_position = nth_last_position(1);
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
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
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
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    SWITCH_TO(AfterAttributeValueQuoted);
                }
                ON('&')
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    m_return_state = State::AttributeValueDoubleQuoted;
                    SWITCH_TO(CharacterReference);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AttributeValueSingleQuoted)
            {
                ON('\'')
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    SWITCH_TO(AfterAttributeValueQuoted);
                }
                ON('&')
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    m_return_state = State::AttributeValueSingleQuoted;
                    SWITCH_TO(CharacterReference);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AttributeValueUnquoted)
            {
                ON_WHITESPACE
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    m_current_token.m_tag.attributes.last().value_end_position = nth_last_position(2);
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('&')
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    m_return_state = State::AttributeValueUnquoted;
                    SWITCH_TO(CharacterReference);
                }
                ON('>')
                {
                    m_current_token.m_tag.attributes.last().value = consume_current_builder();
                    m_current_token.m_tag.attributes.last().value_end_position = nth_last_position(2);
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON('"')
                {
                    log_parse_error();
                    goto AnythingElseAttributeValueUnquoted;
                }
                ON('\'')
                {
                    log_parse_error();
                    goto AnythingElseAttributeValueUnquoted;
                }
                ON('<')
                {
                    log_parse_error();
                    goto AnythingElseAttributeValueUnquoted;
                }
                ON('=')
                {
                    log_parse_error();
                    goto AnythingElseAttributeValueUnquoted;
                }
                ON('`')
                {
                    log_parse_error();
                    goto AnythingElseAttributeValueUnquoted;
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                AnythingElseAttributeValueUnquoted:
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(AfterAttributeValueQuoted)
            {
                m_current_token.m_tag.attributes.last().value_end_position = nth_last_position(2);
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
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
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
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
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
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(CommentEnd);
                }
                ON('>')
                {
                    log_parse_error();
                    consume_current_builder();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(Comment)
            {
                ON('<')
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(CommentLessThanSign);
                }
                ON('-')
                {
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(CommentEndDash);
                }
                ON(0)
                {
                    log_parse_error();
                    m_current_builder.append_code_point(0xFFFD);
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(CommentEnd)
            {
                ON('>')
                {
                    m_current_token.m_comment_or_character.data = consume_current_builder();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON('!')
                {
                    SWITCH_TO(CommentEndBang);
                }
                ON('-')
                {
                    m_current_builder.append('-');
                    continue;
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentEndBang)
            {
                ON('-')
                {
                    m_current_builder.append("--!");
                    SWITCH_TO(CommentEndDash);
                }
                ON('>')
                {
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append("--!");
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentEndDash)
            {
                ON('-')
                {
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(CommentEnd);
                }
                ON_EOF
                {
                    log_parse_error();
                    m_queued_tokens.enqueue(move(m_current_token));
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    m_current_builder.append('-');
                    RECONSUME_IN(Comment);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSign)
            {
                ON('!')
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    SWITCH_TO_WITH_UNCLEAN_BUILDER(CommentLessThanSignBang);
                }
                ON('<')
                {
                    m_current_builder.append_code_point(current_input_character.value());
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
                    RECONSUME_IN(CommentEndDash);
                }
            }
            END_STATE

            BEGIN_STATE(CommentLessThanSignBangDashDash)
            {
                ON('>')
                {
                    RECONSUME_IN(CommentEnd);
                }
                ON_EOF
                {
                    RECONSUME_IN(CommentEnd);
                }
                ANYTHING_ELSE
                {
                    log_parse_error();
                    RECONSUME_IN(CommentEnd);
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
                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    RECONSUME_IN_RETURN_STATE;
                }
            }
            END_STATE

            BEGIN_STATE(NamedCharacterReference)
            {
                size_t byte_offset = m_utf8_view.byte_offset_of(m_prev_utf8_iterator);

                auto match = HTML::code_points_from_entity(m_decoded_input.substring_view(byte_offset, m_decoded_input.length() - byte_offset - 1));

                if (match.has_value()) {
                    skip(match->entity.length() - 1);
                    for (auto ch : match.value().entity)
                        m_temporary_buffer.append(ch);

                    if (consumed_as_part_of_an_attribute() && !match.value().entity.ends_with(';')) {
                        auto next_code_point = peek_code_point(0);
                        if (next_code_point.has_value() && (next_code_point.value() == '=' || isalnum(next_code_point.value()))) {
                            FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                            SWITCH_TO_RETURN_STATE;
                        }
                    }

                    if (!match.value().entity.ends_with(';')) {
                        log_parse_error();
                    }

                    m_temporary_buffer.clear();
                    m_temporary_buffer.append(match.value().code_points);

                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    SWITCH_TO_RETURN_STATE;
                } else {
                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    // FIXME: This should be SWITCH_TO, but we always lose the first character on this path, so just reconsume it.
                    //        I can't wrap my head around how to do it as the spec says.
                    RECONSUME_IN(AmbiguousAmpersand);
                }
            }
            END_STATE

            BEGIN_STATE(AmbiguousAmpersand)
            {
                ON_ASCII_ALPHANUMERIC
                {
                    if (consumed_as_part_of_an_attribute()) {
                        m_current_builder.append_code_point(current_input_character.value());
                        continue;
                    } else {
                        EMIT_CURRENT_CHARACTER;
                    }
                }
                ON(';')
                {
                    log_parse_error();
                    RECONSUME_IN_RETURN_STATE;
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
                    log_parse_error();
                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    RECONSUME_IN_RETURN_STATE;
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
                    log_parse_error();
                    FLUSH_CODEPOINTS_CONSUMED_AS_A_CHARACTER_REFERENCE;
                    RECONSUME_IN_RETURN_STATE;
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
                    log_parse_error();
                    RECONSUME_IN(NumericCharacterReferenceEnd);
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
                    log_parse_error();
                    RECONSUME_IN(NumericCharacterReferenceEnd);
                }
            }
            END_STATE

            BEGIN_STATE(NumericCharacterReferenceEnd)
            {
                DONT_CONSUME_NEXT_INPUT_CHARACTER;

                if (m_character_reference_code == 0) {
                    log_parse_error();
                    m_character_reference_code = 0xFFFD;
                }
                if (m_character_reference_code > 0x10ffff) {
                    log_parse_error();
                    m_character_reference_code = 0xFFFD;
                }
                if (is_surrogate(m_character_reference_code)) {
                    log_parse_error();
                    m_character_reference_code = 0xFFFD;
                }
                if (is_noncharacter(m_character_reference_code)) {
                    log_parse_error();
                }
                if (m_character_reference_code == 0xd || (is_control(m_character_reference_code) && !isspace(m_character_reference_code))) {
                    log_parse_error();
                    constexpr struct {
                        u32 number;
                        u32 code_point;
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
                            m_character_reference_code = entry.code_point;
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
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
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
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', RCDATA);
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
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RCDATA);
                    }
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_builder.append_code_point(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
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
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
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
                    EMIT_CHARACTER_AND_RECONSUME_IN('<', RAWTEXT);
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
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO(BeforeAttributeName);
                }
                ON('/')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO(SelfClosingStartTag);
                }
                ON('>')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (!current_end_tag_token_is_appropriate()) {
                        m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                        m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                        for (auto code_point : m_temporary_buffer)
                            m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                        RECONSUME_IN(RAWTEXT);
                    }
                    SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_builder.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
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
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
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
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
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
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataEscapeStartDash);
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
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataEscapedDashDash);
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
                    SWITCH_TO_AND_EMIT_CHARACTER('>', ScriptData);
                }
                ON(0)
                {
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CHARACTER(0xFFFD, ScriptDataEscaped);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
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
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(BeforeAttributeName);

                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer) {
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    }
                    RECONSUME_IN(ScriptDataEscaped);
                }
                ON('/')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(SelfClosingStartTag);

                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer) {
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    }
                    RECONSUME_IN(ScriptDataEscaped);
                }
                ON('>')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);

                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer) {
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    }
                    RECONSUME_IN(ScriptDataEscaped);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_builder.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer) {
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    }
                    RECONSUME_IN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapeStart)
            {
                auto temporary_buffer_equal_to_script = [this]() -> bool {
                    if (m_temporary_buffer.size() != 6)
                        return false;

                    // FIXME: Is there a better way of doing this?
                    return m_temporary_buffer[0] == 's' && m_temporary_buffer[1] == 'c' && m_temporary_buffer[2] == 'r' && m_temporary_buffer[3] == 'i' && m_temporary_buffer[4] == 'p' && m_temporary_buffer[5] == 't';
                };
                ON_WHITESPACE
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                }
                ON('/')
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                }
                ON('>')
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_temporary_buffer.append(tolower(current_input_character.value()));
                    EMIT_CURRENT_CHARACTER;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_temporary_buffer.append(current_input_character.value());
                    EMIT_CURRENT_CHARACTER;
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscaped)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataDoubleEscapedDash);
                }
                ON('<')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('<', ScriptDataDoubleEscapedLessThanSign);
                }
                ON(0)
                {
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapedDash)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataDoubleEscapedDashDash);
                }
                ON('<')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('<', ScriptDataDoubleEscapedLessThanSign);
                }
                ON(0)
                {
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CHARACTER(0xFFFD, ScriptDataDoubleEscaped);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapedDashDash)
            {
                ON('-')
                {
                    EMIT_CHARACTER('-');
                }
                ON('<')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('<', ScriptDataDoubleEscapedLessThanSign);
                }
                ON('>')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('>', ScriptData);
                }
                ON(0)
                {
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CHARACTER(0xFFFD, ScriptDataDoubleEscaped);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapedLessThanSign)
            {
                ON('/')
                {
                    m_temporary_buffer.clear();
                    SWITCH_TO_AND_EMIT_CHARACTER('/', ScriptDataDoubleEscapeEnd);
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(ScriptDataDoubleEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataDoubleEscapeEnd)
            {
                auto temporary_buffer_equal_to_script = [this]() -> bool {
                    if (m_temporary_buffer.size() != 6)
                        return false;

                    // FIXME: Is there a better way of doing this?
                    return m_temporary_buffer[0] == 's' && m_temporary_buffer[1] == 'c' && m_temporary_buffer[2] == 'r' && m_temporary_buffer[3] == 'i' && m_temporary_buffer[4] == 'p' && m_temporary_buffer[5] == 't';
                };
                ON_WHITESPACE
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                }
                ON('/')
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                }
                ON('>')
                {
                    if (temporary_buffer_equal_to_script())
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                    else
                        SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataDoubleEscaped);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_temporary_buffer.append(tolower(current_input_character.value()));
                    EMIT_CURRENT_CHARACTER;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_temporary_buffer.append(current_input_character.value());
                    EMIT_CURRENT_CHARACTER;
                }
                ANYTHING_ELSE
                {
                    RECONSUME_IN(ScriptDataDoubleEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscapedDash)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataEscapedDashDash);
                }
                ON('<')
                {
                    SWITCH_TO(ScriptDataEscapedLessThanSign);
                }
                ON(0)
                {
                    log_parse_error();
                    SWITCH_TO_AND_EMIT_CHARACTER(0xFFFD, ScriptDataEscaped);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    SWITCH_TO_AND_EMIT_CURRENT_CHARACTER(ScriptDataEscaped);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEscaped)
            {
                ON('-')
                {
                    SWITCH_TO_AND_EMIT_CHARACTER('-', ScriptDataEscapedDash);
                }
                ON('<')
                {
                    SWITCH_TO(ScriptDataEscapedLessThanSign);
                }
                ON(0)
                {
                    log_parse_error();
                    EMIT_CHARACTER(0xFFFD);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
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
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    RECONSUME_IN(ScriptData);
                }
            }
            END_STATE

            BEGIN_STATE(ScriptDataEndTagName)
            {
                ON_WHITESPACE
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(BeforeAttributeName);
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    RECONSUME_IN(ScriptData);
                }
                ON('/')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO(SelfClosingStartTag);
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    RECONSUME_IN(ScriptData);
                }
                ON('>')
                {
                    m_current_token.m_tag.tag_name = consume_current_builder();
                    if (current_end_tag_token_is_appropriate())
                        SWITCH_TO_AND_EMIT_CURRENT_TOKEN(Data);
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    RECONSUME_IN(ScriptData);
                }
                ON_ASCII_UPPER_ALPHA
                {
                    m_current_builder.append(tolower(current_input_character.value()));
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ON_ASCII_LOWER_ALPHA
                {
                    m_current_builder.append(current_input_character.value());
                    m_temporary_buffer.append(current_input_character.value());
                    continue;
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character('<'));
                    m_queued_tokens.enqueue(HTMLToken::make_character('/'));
                    for (auto code_point : m_temporary_buffer)
                        m_queued_tokens.enqueue(HTMLToken::make_character(code_point));
                    RECONSUME_IN(ScriptData);
                }
            }
            END_STATE

            BEGIN_STATE(CDATASection)
            {
                ON(']')
                {
                    SWITCH_TO(CDATASectionBracket);
                }
                ON_EOF
                {
                    log_parse_error();
                    EMIT_EOF;
                }
                ANYTHING_ELSE
                {
                    EMIT_CURRENT_CHARACTER;
                }
            }
            END_STATE

            BEGIN_STATE(CDATASectionBracket)
            {
                ON(']')
                {
                    SWITCH_TO(CDATASectionEnd);
                }
                ANYTHING_ELSE
                {
                    EMIT_CHARACTER_AND_RECONSUME_IN(']', CDATASection);
                }
            }
            END_STATE

            BEGIN_STATE(CDATASectionEnd)
            {
                ON(']')
                {
                    EMIT_CHARACTER(']');
                }
                ON('>')
                {
                    SWITCH_TO(Data);
                }
                ANYTHING_ELSE
                {
                    m_queued_tokens.enqueue(HTMLToken::make_character(']'));
                    m_queued_tokens.enqueue(HTMLToken::make_character(']'));
                    RECONSUME_IN(CDATASection);
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
        auto code_point = peek_code_point(i);
        if (!code_point.has_value())
            return false;
        // FIXME: This should be more Unicode-aware.
        if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
            if (code_point.value() < 0x80) {
                if (tolower(code_point.value()) != tolower(string[i]))
                    return false;
                continue;
            }
        }
        if (code_point.value() != (u32)string[i])
            return false;
    }
    skip(string.length());
    return true;
}

void HTMLTokenizer::create_new_token(HTMLToken::Type type)
{
    m_current_token = {};
    m_current_token.m_type = type;
    size_t offset = 0;
    switch (type) {
    case HTMLToken::Type::StartTag:
        offset = 1;
        break;
    case HTMLToken::Type::EndTag:
        offset = 2;
        break;
    default:
        break;
    }

    m_current_token.m_start_position = nth_last_position(offset);
}

HTMLTokenizer::HTMLTokenizer(const StringView& input, const String& encoding)
{
    auto* decoder = TextCodec::decoder_for(encoding);
    VERIFY(decoder);
    m_decoded_input = decoder->to_utf8(input);
    m_utf8_view = Utf8View(m_decoded_input);
    m_utf8_iterator = m_utf8_view.begin();
    m_source_positions.empend(0u, 0u);
}

void HTMLTokenizer::will_switch_to([[maybe_unused]] State new_state)
{
    dbgln_if(TOKENIZER_TRACE_DEBUG, "[{}] Switch to {}", state_name(m_state), state_name(new_state));
}

void HTMLTokenizer::will_reconsume_in([[maybe_unused]] State new_state)
{
    dbgln_if(TOKENIZER_TRACE_DEBUG, "[{}] Reconsume in {}", state_name(m_state), state_name(new_state));
}

void HTMLTokenizer::switch_to(Badge<HTMLDocumentParser>, State new_state)
{
    dbgln_if(TOKENIZER_TRACE_DEBUG, "[{}] Parser switches tokenizer state to {}", state_name(m_state), state_name(new_state));
    m_state = new_state;
}

void HTMLTokenizer::will_emit(HTMLToken& token)
{
    if (token.is_start_tag())
        m_last_emitted_start_tag_name = token.tag_name();
    token.m_end_position = m_source_positions.last();
}

bool HTMLTokenizer::current_end_tag_token_is_appropriate() const
{
    VERIFY(m_current_token.is_end_tag());
    if (!m_last_emitted_start_tag_name.has_value())
        return false;
    return m_current_token.tag_name() == m_last_emitted_start_tag_name.value();
}

bool HTMLTokenizer::consumed_as_part_of_an_attribute() const
{
    return m_return_state == State::AttributeValueUnquoted || m_return_state == State::AttributeValueSingleQuoted || m_return_state == State::AttributeValueDoubleQuoted;
}

void HTMLTokenizer::restore_to(const Utf8CodepointIterator& new_iterator)
{
    if (new_iterator != m_prev_utf8_iterator) {
        auto diff = m_prev_utf8_iterator - new_iterator;
        if (diff > 0) {
            for (ssize_t i = 0; i < diff; ++i)
                m_source_positions.take_last();
        } else {
            // Going forwards...?
            TODO();
        }
    }
    m_utf8_iterator = new_iterator;
}

String HTMLTokenizer::consume_current_builder()
{
    auto string = m_current_builder.to_string();
    m_current_builder.clear();
    return string;
}

}
