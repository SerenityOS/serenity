/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibWeb/CSS/Parser/Token.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS::Parser {

DeprecatedString Token::to_deprecated_string() const
{
    StringBuilder builder;

    switch (m_type) {
    case Type::EndOfFile:
        return "";
    case Type::Ident:
        return serialize_an_identifier(ident());
    case Type::Function:
        return DeprecatedString::formatted("{}(", serialize_an_identifier(function()));
    case Type::AtKeyword:
        return DeprecatedString::formatted("@{}", serialize_an_identifier(at_keyword()));
    case Type::Hash: {
        switch (m_hash_type) {
        case HashType::Id:
            return DeprecatedString::formatted("#{}", serialize_an_identifier(hash_value()));
        case HashType::Unrestricted:
            return DeprecatedString::formatted("#{}", hash_value());
        }
        VERIFY_NOT_REACHED();
    }
    case Type::String:
        return serialize_a_string(string());
    case Type::BadString:
        return "";
    case Type::Url:
        return serialize_a_url(url());
    case Type::BadUrl:
        return "url()";
    case Type::Delim:
        return DeprecatedString(m_value.bytes_as_string_view());
    case Type::Number:
        return DeprecatedString::number(m_number_value.value());
    case Type::Percentage:
        return DeprecatedString::formatted("{}%", m_number_value.value());
    case Type::Dimension:
        return DeprecatedString::formatted("{}{}", m_number_value.value(), dimension_unit());
    case Type::Whitespace:
        return " ";
    case Type::CDO:
        return "<!--";
    case Type::CDC:
        return "-->";
    case Type::Colon:
        return ":";
    case Type::Semicolon:
        return ";";
    case Type::Comma:
        return ",";
    case Type::OpenSquare:
        return "[";
    case Type::CloseSquare:
        return "]";
    case Type::OpenParen:
        return "(";
    case Type::CloseParen:
        return ")";
    case Type::OpenCurly:
        return "{";
    case Type::CloseCurly:
        return "}";
    case Type::Invalid:
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<String> Token::to_debug_string() const
{
    switch (m_type) {
    case Type::Invalid:
        VERIFY_NOT_REACHED();

    case Type::EndOfFile:
        return String::from_utf8("__EOF__"sv);
    case Type::Ident:
        return String::formatted("Ident: {}", ident());
    case Type::Function:
        return String::formatted("Function: {}", function());
    case Type::AtKeyword:
        return String::formatted("AtKeyword: {}", at_keyword());
    case Type::Hash:
        return String::formatted("Hash: {} (hash_type: {})", hash_value(), m_hash_type == HashType::Unrestricted ? "Unrestricted" : "Id");
    case Type::String:
        return String::formatted("String: {}", string());
    case Type::BadString:
        return String::from_utf8("BadString"sv);
    case Type::Url:
        return String::formatted("Url: {}", url());
    case Type::BadUrl:
        return String::from_utf8("BadUrl"sv);
    case Type::Delim:
        return String::formatted("Delim: {}", m_value);
    case Type::Number:
        return String::formatted("Number: {}{} (number_type: {})", m_number_value.value() > 0 && m_number_value.is_integer_with_explicit_sign() ? "+" : "", m_number_value.value(), m_number_value.is_integer() ? "Integer" : "Number");
    case Type::Percentage:
        return String::formatted("Percentage: {}% (number_type: {})", percentage(), m_number_value.is_integer() ? "Integer" : "Number");
    case Type::Dimension:
        return String::formatted("Dimension: {}{} (number_type: {})", dimension_value(), dimension_unit(), m_number_value.is_integer() ? "Integer" : "Number");
    case Type::Whitespace:
        return String::from_utf8("Whitespace"sv);
    case Type::CDO:
        return String::from_utf8("CDO"sv);
    case Type::CDC:
        return String::from_utf8("CDC"sv);
    case Type::Colon:
        return String::from_utf8("Colon"sv);
    case Type::Semicolon:
        return String::from_utf8("Semicolon"sv);
    case Type::Comma:
        return String::from_utf8("Comma"sv);
    case Type::OpenSquare:
        return String::from_utf8("OpenSquare"sv);
    case Type::CloseSquare:
        return String::from_utf8("CloseSquare"sv);
    case Type::OpenParen:
        return String::from_utf8("OpenParen"sv);
    case Type::CloseParen:
        return String::from_utf8("CloseParen"sv);
    case Type::OpenCurly:
        return String::from_utf8("OpenCurly"sv);
    case Type::CloseCurly:
        return String::from_utf8("CloseCurly"sv);
    }
    VERIFY_NOT_REACHED();
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

StringView Token::bracket_string() const
{
    if (is(Token::Type::OpenCurly)) {
        return "{"sv;
    }

    if (is(Token::Type::CloseCurly)) {
        return "}"sv;
    }

    if (is(Token::Type::OpenSquare)) {
        return "["sv;
    }

    if (is(Token::Type::CloseSquare)) {
        return "]"sv;
    }

    if (is(Token::Type::OpenParen)) {
        return "("sv;
    }

    if (is(Token::Type::CloseParen)) {
        return ")"sv;
    }

    return ""sv;
}

StringView Token::bracket_mirror_string() const
{
    if (is(Token::Type::OpenCurly)) {
        return "}"sv;
    }

    if (is(Token::Type::CloseCurly)) {
        return "{"sv;
    }

    if (is(Token::Type::OpenSquare)) {
        return "]"sv;
    }

    if (is(Token::Type::CloseSquare)) {
        return "["sv;
    }

    if (is(Token::Type::OpenParen)) {
        return ")"sv;
    }

    if (is(Token::Type::CloseParen)) {
        return "("sv;
    }

    return ""sv;
}

}
