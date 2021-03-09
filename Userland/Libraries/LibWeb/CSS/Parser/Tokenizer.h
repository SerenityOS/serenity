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

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/Parser/Token.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class U32Twin {
public:
    void set(size_t index, u32 value)
    {
        if (index == 0)
            first = value;
        if (index == 1)
            second = value;
    }

    u32 first {};
    u32 second {};
};

class U32Triplet {
public:
    void set(size_t index, u32 value)
    {
        if (index == 0)
            first = value;
        if (index == 1)
            second = value;
        if (index == 2)
            third = value;
    }

    U32Twin to_twin_12()
    {
        return { first, second };
    }

    U32Twin to_twin_23()
    {
        return { second, third };
    }

    u32 first {};
    u32 second {};
    u32 third {};
};

class CSSNumber {
public:
    String value;
    Token::NumberType type {};
};

class Tokenizer {

public:
    explicit Tokenizer(const StringView& input, const String& encoding);

    [[nodiscard]] Vector<Token> parse();

private:
    [[nodiscard]] Optional<u32> next_codepoint();
    [[nodiscard]] Optional<u32> peek_codepoint(size_t offset = 0) const;
    [[nodiscard]] Optional<U32Twin> peek_twin() const;
    [[nodiscard]] Optional<U32Triplet> peek_triplet() const;

    [[nodiscard]] static Token create_new_token(Token::TokenType);
    [[nodiscard]] static Token create_value_token(Token::TokenType, String value);
    [[nodiscard]] static Token create_value_token(Token::TokenType, u32 value);
    [[nodiscard]] Token consume_a_token();
    [[nodiscard]] Token consume_string_token(u32 ending_codepoint);
    [[nodiscard]] Token consume_a_numeric_token();
    [[nodiscard]] Token consume_an_ident_like_token();
    [[nodiscard]] CSSNumber consume_a_number();
    [[nodiscard]] String consume_a_name();
    [[nodiscard]] u32 consume_escaped_codepoint();
    [[nodiscard]] Token consume_a_url_token();
    void consume_the_remnants_of_a_bad_url();
    void consume_comments();
    void reconsume_current_input_codepoint();
    [[nodiscard]] bool is_valid_escape_sequence();
    [[nodiscard]] static bool is_valid_escape_sequence(U32Twin);
    [[nodiscard]] bool would_start_an_identifier();
    [[nodiscard]] bool would_start_an_identifier(U32Triplet);
    [[nodiscard]] bool starts_with_a_number() const;
    [[nodiscard]] static bool starts_with_a_number(U32Triplet);

    String m_decoded_input;
    Utf8View m_utf8_view;
    AK::Utf8CodepointIterator m_utf8_iterator;
    AK::Utf8CodepointIterator m_prev_utf8_iterator;
};
}
