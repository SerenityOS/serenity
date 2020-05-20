//
// Created by zac on 17/5/20.
//

#pragma once

#include "Token.h"
#include <AK/String.h>
#include <AK/Vector.h>

class TokenStream final {
public:
    TokenStream(const String& input);
    Token get();
    void putback(Token);

    static bool is_operator(char);
    static bool is_parenthesis(char);
    static bool is_number(char);

private:
    void print_tokens();
    static Token::Kind get_kind_from_operator(char);

    void parse_operator(char);
    void parse_number(const StringBuilder& string_builder);

    Vector<Token> tokens;
    // The input retrieved from the calculator.
    String m_input;
    bool m_full;
    Token m_buffer;
};
