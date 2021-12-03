/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

String Token::to_debug_string() const
{
    StringBuilder builder;

    switch (m_type) {
    case Type::Invalid:
        VERIFY_NOT_REACHED();

    case Type::EndOfFile:
        builder.append("__EOF__");
        break;
    case Type::Ident:
        builder.append("Identifier: ");
        builder.append(m_value);
        return builder.to_string();
    case Type::Function:
        builder.append("Function");
        break;
    case Type::AtKeyword:
        builder.append("@");
        break;
    case Type::Hash:
        builder.append("Hash: ");
        builder.append(m_value);
        return builder.to_string();
    case Type::String:
        builder.append("String: ");
        builder.append(m_value);
        return builder.to_string();
    case Type::BadString:
        builder.append("Invalid String");
        break;
    case Type::Url:
        builder.append("Url");
        break;
    case Type::BadUrl:
        builder.append("Invalid Url");
        break;
    case Type::Delim:
        builder.append("Delimiter: ");
        builder.append(m_value);
        return builder.to_string();
    case Type::Number:
        builder.append("Number: ");
        builder.append(m_value);
        builder.append(m_number_type == NumberType::Integer ? " (int)" : " (float)");
        return builder.to_string();
    case Type::Percentage:
        builder.append("Percentage: ");
        builder.append(m_value);
        builder.append('%');
        return builder.to_string();
    case Type::Dimension:
        builder.append("Dimension: ");
        builder.append(m_value);
        builder.append(m_unit);
        return builder.to_string();
    case Type::Whitespace:
        builder.append("Whitespace");
        break;
    case Type::CDO:
        builder.append("CDO");
        break;
    case Type::CDC:
        builder.append("CDC");
        break;
    case Type::Colon:
        builder.append(":");
        break;
    case Type::Semicolon:
        builder.append(";");
        break;
    case Type::Comma:
        builder.append(",");
        break;
    case Type::OpenSquare:
        builder.append("[");
        break;
    case Type::CloseSquare:
        builder.append("]");
        break;
    case Type::OpenParen:
        builder.append("(");
        break;
    case Type::CloseParen:
        builder.append(")");
        break;
    case Type::OpenCurly:
        builder.append("{");
        break;
    case Type::CloseCurly:
        builder.append("}");
        break;
    }

    if (m_value.is_empty()) {
        return builder.to_string();
    }

    builder.append(" ");

    builder.append(" { value: '");
    builder.append(m_value);

    if (m_type == Token::Type::Hash) {
        builder.append("', hash_type: '");
        if (m_hash_type == Token::HashType::Unrestricted) {
            builder.append("Unrestricted");
        } else {
            builder.append("Id");
        }
    }

    if (m_type == Token::Type::Number) {
        builder.append("', number_type: '");
        if (m_number_type == Token::NumberType::Integer) {
            builder.append("Integer");
        } else {
            builder.append("Number");
        }
    }

    if (m_type == Token::Type::Dimension) {
        builder.append("', number_type: '");
        if (m_number_type == Token::NumberType::Integer) {
            builder.append("Integer");
        } else {
            builder.append("Number");
        }

        builder.append("', unit: '");
        builder.append(m_unit);
    }

    builder.append("' }");
    return builder.to_string();
}

Token::Type Token::mirror_variant() const
{
    if (is(Token::Type::OpenCurly)) {
        return Type::CloseCurly;
    }

    if (is(Token::Type::OpenSquare)) {
        return Type::CloseSquare;
    }

    if (is(Token::Type::OpenParen)) {
        return Type::CloseParen;
    }

    return Type::Invalid;
}

String Token::bracket_string() const
{
    if (is(Token::Type::OpenCurly)) {
        return "{";
    }

    if (is(Token::Type::CloseCurly)) {
        return "}";
    }

    if (is(Token::Type::OpenSquare)) {
        return "[";
    }

    if (is(Token::Type::CloseSquare)) {
        return "]";
    }

    if (is(Token::Type::OpenParen)) {
        return "(";
    }

    if (is(Token::Type::CloseParen)) {
        return ")";
    }

    return "";
}

String Token::bracket_mirror_string() const
{
    if (is(Token::Type::OpenCurly)) {
        return "}";
    }

    if (is(Token::Type::CloseCurly)) {
        return "{";
    }

    if (is(Token::Type::OpenSquare)) {
        return "]";
    }

    if (is(Token::Type::CloseSquare)) {
        return "[";
    }

    if (is(Token::Type::OpenParen)) {
        return ")";
    }

    if (is(Token::Type::CloseParen)) {
        return "(";
    }

    return "";
}

}
