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

#include "Calculator.h"
#include <AK/Assertions.h>

Calculator::Calculator(const String& input)
{
    m_token_stream = new TokenStream(input);
}

double Calculator::evaluate()
{
    return expression();
}

double Calculator::expression()
{
    double left = term();

    if (m_has_error)
        return -1;

    Token t = m_token_stream->get();

    while (true) {
        switch (t.kind()) {
        case Token::Kind::PLUS:
            left += term();
            t = m_token_stream->get();
            break;
        case Token::Kind::MINUS:
            left -= term();
            t = m_token_stream->get();
            break;
        default:
            m_token_stream->putback(t);
            return left;
        }
    }
}

double Calculator::term()
{
    double left = primary();

    if (m_has_error)
        return -1;

    Token t = m_token_stream->get();

    while (true) {
        switch (t.kind()) {
        case Token::Kind::MULTIPLY:
            left *= primary();
            t = m_token_stream->get();
            break;
        case Token::Kind::DIVIDE:
            left /= primary();
            t = m_token_stream->get();
            break;
        default:
            m_token_stream->putback(t);
            return left;
        }
    }
}

double Calculator::primary()
{
    Token t = m_token_stream->get();

    switch (t.kind()) {
    case Token::Kind::OPEN_PARENTHESIS: {
        double d = expression();
        t = m_token_stream->get();
        ASSERT(t.kind() == Token::Kind::CLOSE_PARENTHESIS);
        return d;
    }
    case Token::Kind::NUMBER:
        return t.value();
    default:
        m_has_error = true;
        return -1;
    }
}

bool Calculator::has_error() const
{
    return m_has_error;
}
