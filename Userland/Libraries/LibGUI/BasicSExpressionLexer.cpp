/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BasicSExpressionLexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Vector.h>

namespace GUI {

BasicSExpressionLexer::BasicSExpressionLexer(StringView input)
    : m_lexer(input)
{
}

Vector<BasicSExpressionToken> BasicSExpressionLexer::lex()
{
    Vector<BasicSExpressionToken> tokens;
    auto token = [&](auto type, auto token_start) {
        tokens.append({ type, token_start, m_position });
    };

    while (!m_lexer.is_eof()) {
        while (m_lexer.next_is(is_ascii_space)) {
            ++m_position.column;
            if (m_lexer.next_is('\n')) {
                ++m_position.line;
                m_position.column = 0;
            }
            m_lexer.ignore();
        }

        auto token_start = m_position;
        if (m_lexer.consume_specific('(')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::OpenParen, token_start);
        } else if (m_lexer.consume_specific(')')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::CloseParen, token_start);
        } else if (m_lexer.consume_specific('{')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::OpenBrace, token_start);
        } else if (m_lexer.consume_specific('}')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::CloseBrace, token_start);
        } else if (m_lexer.consume_specific('[')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::OpenBracket, token_start);
        } else if (m_lexer.consume_specific(']')) {
            ++m_position.column;
            token(BasicSExpressionToken::Type::CloseBracket, token_start);
        } else if (m_lexer.consume_specific(';')) {
            while (!m_lexer.is_eof() && !m_lexer.next_is('\n')) {
                m_lexer.ignore();
                ++m_position.column;
            }
            token(BasicSExpressionToken::Type::Comment, token_start);
        } else if (m_lexer.next_is(is_any_of("'\""))) {
            auto quote = m_lexer.consume();

            ++m_position.column;
            while (!m_lexer.is_eof()) {
                if (m_lexer.next_is(quote))
                    break;

                ++m_position.column;
                if (m_lexer.next_is('\n')) {
                    ++m_position.line;
                    m_position.column = 0;
                }

                m_lexer.ignore();
            }
            if (m_lexer.next_is(quote)) {
                m_lexer.ignore();
                ++m_position.column;
            }

            token(quote == '"' ? BasicSExpressionToken::Type::DoubleQuotedString : BasicSExpressionToken::Type::SingleQuotedString, token_start);
        } else if (m_lexer.next_is(is_ascii_digit)) {
            auto seen_dot = false;
            while (!m_lexer.is_eof()) {
                if (!seen_dot && m_lexer.next_is('.'))
                    seen_dot = true;
                else if (!m_lexer.next_is(is_ascii_digit))
                    break;

                ++m_position.column;
                m_lexer.ignore();
            }

            token(BasicSExpressionToken::Type::Number, token_start);
        } else {
            while (!m_lexer.is_eof()) {
                if (m_lexer.next_is(is_any_of(" \r\t\b\n\v\f(){}[]\"'")))
                    break;

                ++m_position.column;
                m_lexer.ignore();
            }

            if (!tokens.is_empty() && tokens.last().m_type == BasicSExpressionToken::Type::OpenParen)
                token(BasicSExpressionToken::Type::FormName, token_start);
            else
                token(BasicSExpressionToken::Type::Word, token_start);
        }
    }

    return tokens;
}

}
