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

//#define TOKENIZER_TRACE

#define SWITCH_TO(new_state)                    \
    will_switch_to(State::new_state);           \
    m_state = State::new_state;                 \
    current_input_character = next_codepoint(); \
    goto new_state;

#define RECONSUME_IN(new_state)          \
    will_reconsume_in(State::new_state); \
    m_state = State::new_state;          \
    goto new_state;

#define DONT_CONSUME_NEXT_INPUT_CHARACTER --m_cursor;

#define IGNORE_CHARACTER_AND_CONTINUE_IN(x) SWITCH_TO(x)

#define ON(codepoint) \
    if (current_input_character.has_value() && current_input_character.value() == codepoint)

#define ON_EOF \
    if (!current_input_character.has_value())

#define ON_ASCII_ALPHA \
    if (current_input_character.has_value() && isalpha(current_input_character.value()))

#define ON_WHITESPACE \
    if (current_input_character.has_value() && (current_input_character.value() == '\t' || current_input_character.value() == '\a' || current_input_character.value() == '\f' || current_input_character.value() == ' '))

#define ANYTHING_ELSE if (1)

#define EMIT_EOF_AND_RETURN                       \
    create_new_token(HTMLToken::Type::EndOfFile); \
    emit_current_token();                         \
    return;

#define BEGIN_STATE(state) \
    state:                 \
    case State::state:

#define END_STATE         \
    ASSERT_NOT_REACHED(); \
    break;

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

void HTMLTokenizer::run()
{
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
                    EMIT_EOF_AND_RETURN;
                }
                ANYTHING_ELSE
                {
                    create_new_token(HTMLToken::Type::Character);
                    m_current_token.m_comment_or_character.data.append(current_input_character.value());
                    emit_current_token();
                    continue;
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
            }
            END_STATE

            BEGIN_STATE(TagName)
            {
                ON('>')
                {
                    emit_current_token();
                    SWITCH_TO(Data);
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_tag.tag_name.append(current_input_character.value());
                    continue;
                }
            }

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
                if (next_few_characters_are("DOCTYPE")) {
                    consume("DOCTYPE");
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
            }
            END_STATE

            BEGIN_STATE(BeforeDOCTYPEName)
            {
                ON_WHITESPACE
                {
                    continue;
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
                ON('>')
                {
                    emit_current_token();
                    SWITCH_TO(Data);
                }
                ANYTHING_ELSE
                {
                    m_current_token.m_doctype.name.append(current_input_character.value());
                    continue;
                }
            }
            END_STATE

            BEGIN_STATE(CharacterReference)
            {
            }
            END_STATE

        default:
            ASSERT_NOT_REACHED();
        }
    }
}

void HTMLTokenizer::consume(const StringView& string)
{
    ASSERT(next_few_characters_are(string));
    m_cursor += string.length();
}

bool HTMLTokenizer::next_few_characters_are(const StringView& string) const
{
    for (size_t i = 0; i < string.length(); ++i) {
        auto codepoint = peek_codepoint(i);
        if (!codepoint.has_value())
            return false;
        // FIXME: This should be more Unicode-aware.
        if (codepoint.value() != (u32)string[i])
            return false;
    }
    return true;
}

void HTMLTokenizer::emit_current_token()
{
    StringBuilder builder;

    switch (m_current_token.type()) {
    case HTMLToken::Type::DOCTYPE:
        builder.append("DOCTYPE");
        builder.append(" { name: '");
        builder.append(m_current_token.m_doctype.name.to_string());
        builder.append("' }");
        break;
    case HTMLToken::Type::StartTag:
        builder.append("StartTag");
        break;
    case HTMLToken::Type::EndTag:
        builder.append("EndTag");
        break;
    case HTMLToken::Type::Comment:
        builder.append("Comment");
        break;
    case HTMLToken::Type::Character:
        builder.append("Character");
        break;
    case HTMLToken::Type::EndOfFile:
        builder.append("EndOfFile");
        break;
    }

    if (m_current_token.type() == HTMLToken::Type::StartTag || m_current_token.type() == HTMLToken::Type::EndTag) {
        builder.append(" { name: '");
        builder.append(m_current_token.m_tag.tag_name.to_string());
        builder.append("' }");
    }

    dbg() << "[" << String::format("%42s", state_name(m_state)) << "] " << builder.to_string();
    m_current_token = {};
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

}
