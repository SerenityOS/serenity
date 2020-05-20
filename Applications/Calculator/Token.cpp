//
// Created by zac on 17/5/20.
//

#include "Token.h"
#include <AK/StringBuilder.h>

Token::Token(Token::Kind kind)
    : m_kind(kind)
    , m_value(0)
{
}

Token::Token(Token::Kind kind, double value)
    : m_kind(kind)
    , m_value(value)
{
}

void Token::print_token(const DebugLogStream& ls)
{
    String enum_string;
    switch (m_kind) {
    case Token::Kind::INVALID:
        enum_string = "INVALID";
        break;
    case Token::Kind::NUMBER:
        enum_string = "NUMBER";
        break;
    case Token::Kind::PLUS:
        enum_string = "PLUS";
        break;
    case Token::Kind::MINUS:
        enum_string = "MINUS";
        break;
    case Token::Kind::MULTIPLY:
        enum_string = "MULTIPLY";
        break;
    case Token::Kind::DIVIDE:
        enum_string = "DIVIDE";
        break;
    case Token::Kind::OPEN_PARENTHESIS:
        enum_string = "OPEN_PARENTHESIS";
        break;
    case Token::Kind::CLOSE_PARENTHESIS:
        enum_string = "CLOSE_PARENTHESIS";
        break;
    default:
        enum_string = "DEFAULT";
        break;
    }
    ls << "Token{kind:" << enum_string << ", value:" << m_value << "}";
}
