//
// Created by zac on 17/5/20.
//

#include <AK/Assertions.h>

#include "TokenStream.h"
#include <AK/StringBuilder.h>

// Read the next token from the token stream, which in this case, is the full string of characters
// entered/found in the calculator.
Token TokenStream::get()
{
    if (m_full) {
        m_full = false;
        return m_buffer;
    }

    if (!tokens.is_empty())
        return tokens.take_first();
    else
        return Token(Token::Kind::INVALID);
}

void TokenStream::putback(Token token)
{
    ASSERT(!m_full);

    m_buffer = token;
    m_full = true;
}

// Initializes the token stream for this calculation. Parses and tokenizes the calculator input to be used by
// the Calculator class which will evaluate the calculation.
TokenStream::TokenStream(const String& input)
    : m_input(input)
    , m_full(false)
    , m_buffer(Token::Kind::INVALID)
{
    StringBuilder string_builder = StringBuilder();
    for (auto& c : input) {
        if (is_parenthesis(c)) {
            // Handles implied multiplication operator. e.g. 12(1+2) is 12 * ( 1 + 2 )
            // If the string builder is not empty, then there is an un-tokenized number
            // that needs to be handled before the parenthesis.
            if (!string_builder.is_empty() && c == '(') {
                parse_number(string_builder);
                tokens.append(Token(Token::Kind::MULTIPLY));
            } else if (!string_builder.is_empty() && c == ')') {
                parse_number(string_builder);
            }

            parse_operator(c);
            string_builder.clear();
        } else if (is_operator(c)) {
            parse_number(string_builder);
            parse_operator(c);
            string_builder.clear();
        } else {
            string_builder.append(c);
        }
    }

    if (!string_builder.is_empty())
        parse_number(string_builder);
}

void TokenStream::parse_number(const StringBuilder& string_builder)
{
    if (string_builder.is_empty())
        return;
    Token number = Token(Token::Kind::NUMBER, atof(string_builder.to_string().characters()));
    tokens.append(number);
}

void TokenStream::parse_operator(char c)
{
    Token operator_token = Token(get_kind_from_operator(c));
    tokens.append(operator_token);
}

bool TokenStream::is_parenthesis(char c)
{
    return c == '(' || c == ')';
}

bool TokenStream::is_operator(char c)
{
    return c == '+'
        || c == '-'
        || c == '/'
        || c == '*';
}

bool TokenStream::is_number(char c)
{
    switch (c) {
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
    case '0':
    case '.':
        return true;
    default:
        return false;
    }
}

Token::Kind TokenStream::get_kind_from_operator(char c)
{
    if (c == '+')
        return Token::Kind::PLUS;
    if (c == '-')
        return Token::Kind::MINUS;
    if (c == '/')
        return Token::Kind::DIVIDE;
    if (c == '*')
        return Token::Kind::MULTIPLY;
    if (c == '(')
        return Token::Kind::OPEN_PARENTHESIS;
    if (c == ')')
        return Token::Kind::CLOSE_PARENTHESIS;
    return Token::Kind::INVALID;
}

void TokenStream::print_tokens()
{
    for (auto& token : tokens) {
        token.print_token(dbg());
    }
}
