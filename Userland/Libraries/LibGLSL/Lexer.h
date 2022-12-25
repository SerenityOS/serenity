/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Token.h"

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/StringView.h>

namespace GLSL {

class Lexer {
public:
    explicit Lexer(StringView source, size_t line_number = 1, size_t line_column = 0);

    Token next();

    DeprecatedString const& source() const { return m_source; };

private:
    void consume();
    bool consume_exponent();
    bool consume_octal_number();
    bool consume_hexadecimal_number();
    bool consume_binary_number();
    bool consume_decimal_number();

    u32 current_code_point() const;

    bool is_eof() const;
    bool is_line_terminator() const;
    bool is_whitespace() const;
    Optional<u32> is_identifier_start(size_t& identifier_length) const;
    Optional<u32> is_identifier_middle(size_t& identifier_length) const;
    bool is_line_comment_start() const;
    bool is_block_comment_start() const;
    bool is_block_comment_end() const;
    bool is_numeric_literal_start() const;
    bool match(char, char) const;
    bool match(char, char, char) const;
    bool match(char, char, char, char) const;
    template<typename Callback>
    bool match_numeric_literal_separator_followed_by(Callback) const;
    bool slash_means_division() const;

    DeprecatedString m_source;
    size_t m_position { 0 };
    Token m_current_token;
    char m_current_char { 0 };
    bool m_eof { false };

    size_t m_line_number { 1 };
    size_t m_line_column { 0 };

    static HashMap<FlyString, TokenType> s_keywords;
    static HashMap<DeprecatedString, TokenType> s_three_char_tokens;
    static HashMap<DeprecatedString, TokenType> s_two_char_tokens;
    static HashMap<char, TokenType> s_single_char_tokens;

    struct ParsedIdentifiers : public RefCounted<ParsedIdentifiers> {
        // Resolved identifiers must be kept alive for the duration of the parsing stage, otherwise
        // the only references to these strings are deleted by the Token destructor.
        HashTable<FlyString> identifiers;
    };

    RefPtr<ParsedIdentifiers> m_parsed_identifiers;
};

}
