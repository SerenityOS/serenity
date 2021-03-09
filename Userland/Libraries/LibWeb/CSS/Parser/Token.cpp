/*
 * Copyright (c) 2020-2021, SerenityOS developers
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

#include <AK/String.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

String Token::to_string() const
{
    StringBuilder builder;

    switch (m_type) {
    case TokenType::Invalid:
        VERIFY_NOT_REACHED();

    case TokenType::EndOfFile:
        builder.append("__EOF__");
        break;
    case TokenType::Ident:
        //builder.append("Identifier");
        builder.append(m_value.to_string());
        return builder.to_string();
    case TokenType::Function:
        builder.append("Function");
        break;
    case TokenType::AtKeyword:
        builder.append("@");
        break;
    case TokenType::Hash:
        builder.append("#");
        builder.append(m_value.to_string());
        return builder.to_string();
    case TokenType::String:
        //builder.append("String");
        builder.append(m_value.to_string());
        return builder.to_string();
    case TokenType::BadString:
        builder.append("Invalid String");
        break;
    case TokenType::Url:
        builder.append("Url");
        break;
    case TokenType::BadUrl:
        builder.append("Invalid Url");
        break;
    case TokenType::Delim:
        //builder.append("Delimiter");
        builder.append(m_value.to_string());
        return builder.to_string();
    case TokenType::Number:
        //builder.append("Number");
        builder.append(m_value.to_string());
        builder.append(m_unit.to_string());
        return builder.to_string();
    case TokenType::Percentage:
        //builder.append("Percentage");
        builder.append(m_value.to_string());
        builder.append(m_unit.to_string());
        return builder.to_string();
    case TokenType::Dimension:
        //builder.append("Dimension");
        builder.append(m_value.to_string());
        builder.append(m_unit.to_string());
        return builder.to_string();
    case TokenType::Whitespace:
        builder.append("Whitespace");
        break;
    case TokenType::CDO:
        builder.append("CDO");
        break;
    case TokenType::CDC:
        builder.append("CDC");
        break;
    case TokenType::Colon:
        builder.append(":");
        break;
    case TokenType::Semicolon:
        builder.append(";");
        break;
    case TokenType::Comma:
        builder.append(",");
        break;
    case TokenType::OpenSquare:
        builder.append("[");
        break;
    case TokenType::CloseSquare:
        builder.append("]");
        break;
    case TokenType::OpenParen:
        builder.append("(");
        break;
    case TokenType::CloseParen:
        builder.append(")");
        break;
    case TokenType::OpenCurly:
        builder.append("{");
        break;
    case TokenType::CloseCurly:
        builder.append("}");
        break;
    }

    if (m_value.is_empty()) {
        return builder.to_string();
    }

    builder.append(" ");

    builder.append(" { value: '");
    builder.append(m_value.to_string());

    if (m_type == Token::TokenType::Hash) {
        builder.append("', hash_type: '");
        if (m_hash_type == Token::HashType::Unrestricted) {
            builder.append("Unrestricted");
        } else {
            builder.append("Id");
        }
    }

    if (m_type == Token::TokenType::Number) {
        builder.append("', number_type: '");
        if (m_number_type == Token::NumberType::Integer) {
            builder.append("Integer");
        } else {
            builder.append("Number");
        }
    }

    if (m_type == Token::TokenType::Dimension) {
        builder.append("', number_type: '");
        if (m_number_type == Token::NumberType::Integer) {
            builder.append("Integer");
        } else {
            builder.append("Number");
        }

        builder.append("', unit: '");
        builder.append(m_unit.to_string());
    }

    builder.append("' }");
    return builder.to_string();
}

Token::TokenType Token::mirror_variant() const
{
    if (is_open_curly()) {
        return TokenType::CloseCurly;
    }

    if (is_open_square()) {
        return TokenType::CloseSquare;
    }

    if (is_open_paren()) {
        return TokenType::CloseParen;
    }

    return TokenType::Invalid;
}

String Token::bracket_string() const
{
    if (is_open_curly()) {
        return "{";
    }

    if (is_close_curly()) {
        return "}";
    }

    if (is_open_square()) {
        return "[";
    }

    if (is_close_square()) {
        return "]";
    }

    if (is_open_paren()) {
        return "(";
    }

    if (is_close_paren()) {
        return ")";
    }

    return "";
}

String Token::bracket_mirror_string() const
{
    if (is_open_curly()) {
        return "}";
    }

    if (is_close_curly()) {
        return "{";
    }

    if (is_open_square()) {
        return "]";
    }

    if (is_close_square()) {
        return "[";
    }

    if (is_open_paren()) {
        return ")";
    }

    if (is_close_paren()) {
        return "(";
    }

    return "";
}

}
