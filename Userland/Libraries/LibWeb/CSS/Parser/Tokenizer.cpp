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

#include <AK/Vector.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <ctype.h>

#define CSS_TOKENIZER_TRACE 0

#define PARSE_ERROR()                                                                                           \
    do {                                                                                                        \
        dbgln_if(CSS_TOKENIZER_TRACE, "Parse error (css tokenization) {} @ {}", __PRETTY_FUNCTION__, __LINE__); \
    } while (0)

//U+FFFD REPLACEMENT CHARACTER (�)
#define REPLACEMENT_CHARACTER 0xFFFD

static inline bool is_surrogate(u32 codepoint)
{
    return (codepoint & 0xfffff800) == 0xd800;
}

static inline bool is_quotation_mark(u32 codepoint)
{
    return codepoint == 0x22;
}

static inline bool is_greater_than_maximum_allowed_codepoint(u32 codepoint)
{
    return codepoint > 0x10FFFF;
}

static inline bool is_hex_digit(u32 codepoint)
{
    return isxdigit(codepoint);
}

static inline bool is_low_line(u32 codepoint)
{
    return codepoint == 0x5F;
}

static inline bool is_non_ascii(u32 codepoint)
{
    return codepoint >= 0x80;
}

static inline bool is_name_start_codepoint(u32 codepoint)
{
    return isalpha(codepoint) || is_non_ascii(codepoint) || is_low_line(codepoint);
}

static inline bool is_hyphen_minus(u32 codepoint)
{
    return codepoint == 0x2D;
}

static inline bool is_name_codepoint(u32 codepoint)
{
    return is_name_start_codepoint(codepoint) || isdigit(codepoint) || is_hyphen_minus(codepoint);
}

static inline bool is_non_printable(u32 codepoint)
{
    return codepoint <= 0x8 || codepoint == 0xB || (codepoint >= 0xE && codepoint <= 0x1F) || codepoint == 0x7F;
}

static inline bool is_number_sign(u32 codepoint)
{
    return codepoint == 0x23;
}

static inline bool is_reverse_solidus(u32 codepoint)
{
    return codepoint == 0x5C;
}

static inline bool is_apostrophe(u32 codepoint)
{
    return codepoint == 0x27;
}

static inline bool is_left_paren(u32 codepoint)
{
    return codepoint == 0x28;
}

static inline bool is_right_paren(u32 codepoint)
{
    return codepoint == 0x29;
}

static inline bool is_plus_sign(u32 codepoint)
{
    return codepoint == 0x2B;
}

static inline bool is_comma(u32 codepoint)
{
    return codepoint == 0x2C;
}

static inline bool is_full_stop(u32 codepoint)
{
    return codepoint == 0x2E;
}

static inline bool is_newline(u32 codepoint)
{
    return codepoint == 0xA;
}

static inline bool is_asterisk(u32 codepoint)
{
    return codepoint == 0x2A;
}

static inline bool is_solidus(u32 codepoint)
{
    return codepoint == 0x2F;
}

static inline bool is_colon(u32 codepoint)
{
    return codepoint == 0x3A;
}

static inline bool is_semicolon(u32 codepoint)
{
    return codepoint == 0x3B;
}

static inline bool is_less_than_sign(u32 codepoint)
{
    return codepoint == 0x3C;
}

static inline bool is_greater_than_sign(u32 codepoint)
{
    return codepoint == 0x3E;
}

static inline bool is_at(u32 codepoint)
{
    return codepoint == 0x40;
}

static inline bool is_open_square_bracket(u32 codepoint)
{
    return codepoint == 0x5B;
}

static inline bool is_closed_square_bracket(u32 codepoint)
{
    return codepoint == 0x5D;
}

static inline bool is_open_curly_bracket(u32 codepoint)
{
    return codepoint == 0x7B;
}

static inline bool is_closed_curly_bracket(u32 codepoint)
{
    return codepoint == 0x7D;
}

static inline bool is_whitespace(u32 codepoint)
{
    return codepoint == 0x9 || codepoint == 0xA || codepoint == 0x20;
}

static inline bool is_percent(u32 codepoint)
{
    return codepoint == 0x25;
}

static inline bool is_exclamation_mark(u32 codepoint)
{
    return codepoint == 0x21;
}

static inline bool is_e(u32 codepoint)
{
    return codepoint == 0x65;
}

static inline bool is_E(u32 codepoint)
{
    return codepoint == 0x45;
}

namespace Web::CSS {

Tokenizer::Tokenizer(const StringView& input, const String& encoding)
{
    auto* decoder = TextCodec::decoder_for(encoding);
    VERIFY(decoder);

    // FIXME: preprocess the stream
    // https://www.w3.org/TR/css-syntax-3/#input-preprocessing
    m_decoded_input = decoder->to_utf8(input);
    m_utf8_view = Utf8View(m_decoded_input);
    m_utf8_iterator = m_utf8_view.begin();
}

Vector<Token> Tokenizer::parse()
{
    Vector<Token> tokens;
    for (;;) {
        auto token = consume_a_token();
        tokens.append(token);

        if (token.is_eof()) {
            return tokens;
        }
    }
}

Optional<u32> Tokenizer::next_codepoint()
{
    if (m_utf8_iterator == m_utf8_view.end())
        return {};
    m_prev_utf8_iterator = m_utf8_iterator;
    ++m_utf8_iterator;
    dbgln_if(CSS_TOKENIZER_TRACE, "(Tokenizer) Next codepoint: {:c}", (char)*m_prev_utf8_iterator);
    return *m_prev_utf8_iterator;
}

Optional<u32> Tokenizer::peek_codepoint(size_t offset) const
{
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < offset && it != m_utf8_view.end(); ++i)
        ++it;
    if (it == m_utf8_view.end())
        return {};
    return *it;
}

Optional<U32Twin> Tokenizer::peek_twin() const
{
    U32Twin values;
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < 2 && it != m_utf8_view.end(); ++i) {
        if (it == m_utf8_view.end())
            return {};
        values.set(i, *it);
        ++it;
    }

    return values;
}

Optional<U32Triplet> Tokenizer::peek_triplet() const
{
    U32Triplet values;
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < 3 && it != m_utf8_view.end(); ++i) {
        if (it == m_utf8_view.end())
            return {};
        values.set(i, *it);
        ++it;
    }
    return values;
}

Token Tokenizer::create_new_token(Token::TokenType type)
{
    Token token = {};
    token.m_type = type;
    return token;
}

Token Tokenizer::create_value_token(Token::TokenType type, String value)
{
    Token token;
    token.m_type = type;
    token.m_value.append(move(value));
    return token;
}

Token Tokenizer::create_value_token(Token::TokenType type, u32 value)
{
    Token token = {};
    token.m_type = type;
    token.m_value.append_code_point(value);
    return token;
}

u32 Tokenizer::consume_escaped_codepoint()
{
    auto codepoint = next_codepoint();

    if (!codepoint.has_value()) {
        PARSE_ERROR();
        return REPLACEMENT_CHARACTER;
    }

    auto input = codepoint.value();

    if (is_hex_digit(input)) {
        StringBuilder builder;
        builder.append_code_point(input);

        size_t counter = 0;
        while (is_hex_digit(peek_codepoint().value()) && counter++ < 5) {
            builder.append_code_point(next_codepoint().value());
        }

        if (is_whitespace(peek_codepoint().value())) {
            (void)next_codepoint();
        }

        auto unhexed = strtoul(builder.to_string().characters(), nullptr, 16);
        if (unhexed == 0 || is_surrogate(unhexed) || is_greater_than_maximum_allowed_codepoint(unhexed)) {
            return REPLACEMENT_CHARACTER;
        }

        return unhexed;
    }

    if (!input) {
        PARSE_ERROR();
        return REPLACEMENT_CHARACTER;
    }

    return input;
}

Token Tokenizer::consume_an_ident_like_token()
{
    auto string = consume_a_name();

    if (string.equals_ignoring_case("url") && is_left_paren(peek_codepoint().value())) {
        (void)next_codepoint();

        for (;;) {
            auto maybe_whitespace = peek_twin().value();
            if (!(is_whitespace(maybe_whitespace.first) && is_whitespace(maybe_whitespace.second))) {
                break;
            }

            (void)next_codepoint();
        }

        auto next_two = peek_twin().value();
        // if one of these ", ', ' "', " '"
        if (is_quotation_mark(next_two.first) || is_apostrophe(next_two.first) || (is_whitespace(next_two.first) && (is_quotation_mark(next_two.second) || is_apostrophe(next_two.second)))) {
            return create_value_token(Token::TokenType::Function, string);
        }

        return consume_a_url_token();
    }

    if (is_left_paren(peek_codepoint().value())) {
        (void)next_codepoint();

        return create_value_token(Token::TokenType::Function, string);
    }

    return create_value_token(Token::TokenType::Ident, string);
}

CSSNumber Tokenizer::consume_a_number()
{
    StringBuilder repr;
    Token::NumberType type = Token::NumberType::Integer;

    auto next_input = peek_codepoint().value();
    if (is_plus_sign(next_input) || is_hyphen_minus(next_input)) {
        repr.append_code_point(next_codepoint().value());
    }

    for (;;) {
        auto digits = peek_codepoint().value();
        if (!isdigit(digits))
            break;

        repr.append_code_point(next_codepoint().value());
    }

    auto maybe_number = peek_twin().value();
    if (is_full_stop(maybe_number.first) && isdigit(maybe_number.second)) {
        repr.append_code_point(next_codepoint().value());
        repr.append_code_point(next_codepoint().value());

        type = Token::NumberType::Number;

        for (;;) {
            auto digits = peek_codepoint();
            if (digits.has_value() && !isdigit(digits.value()))
                break;

            repr.append_code_point(next_codepoint().value());
        }
    }

    auto maybe_exp = peek_triplet().value();
    if (is_E(maybe_exp.first) || is_e(maybe_exp.first)) {
        if (is_plus_sign(maybe_exp.second) || is_hyphen_minus(maybe_exp.second)) {
            if (isdigit(maybe_exp.third)) {
                repr.append_code_point(next_codepoint().value());
                repr.append_code_point(next_codepoint().value());
                repr.append_code_point(next_codepoint().value());
            }
        } else if (isdigit(maybe_exp.second)) {
            repr.append_code_point(next_codepoint().value());
            repr.append_code_point(next_codepoint().value());
        }

        type = Token::NumberType::Number;

        for (;;) {
            auto digits = peek_codepoint().value();
            if (!isdigit(digits))
                break;

            repr.append_code_point(next_codepoint().value());
        }
    }

    return { repr.to_string(), type };
}

String Tokenizer::consume_a_name()
{
    StringBuilder result;

    for (;;) {
        auto input = next_codepoint().value();

        if (is_name_codepoint(input)) {
            result.append_code_point(input);
            continue;
        }

        auto next = peek_codepoint();
        if (next.has_value() && is_valid_escape_sequence({ input, next.value() })) {
            result.append_code_point(consume_escaped_codepoint());
            continue;
        }

        break;
    }

    reconsume_current_input_codepoint();
    return result.to_string();
}
Token Tokenizer::consume_a_url_token()
{
    auto token = create_new_token(Token::TokenType::Url);
    for (;;) {
        if (!is_whitespace(peek_codepoint().value())) {
            break;
        }

        (void)next_codepoint();
    }

    for (;;) {

        auto codepoint = peek_codepoint();
        if (!codepoint.has_value()) {
            PARSE_ERROR();
            return token;
        }

        auto input = codepoint.value();

        if (is_right_paren(input)) {
            (void)next_codepoint();
            return token;
        }

        if (is_whitespace(input)) {
            for (;;) {
                if (!is_whitespace(peek_codepoint().value())) {
                    break;
                }

                codepoint = next_codepoint();
            }

            if (!codepoint.has_value()) {
                PARSE_ERROR();
                return token;
            }

            input = codepoint.value();

            if (is_right_paren(input)) {
                return token;
            }

            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::TokenType::BadUrl);
        }

        if (is_quotation_mark(input) || is_apostrophe(input) || is_left_paren(input) || is_non_printable(input)) {
            PARSE_ERROR();
            (void)next_codepoint();
            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::TokenType::BadUrl);
        }

        if (is_reverse_solidus(input)) {
            if (is_valid_escape_sequence()) {
                token.m_value.append_code_point(consume_escaped_codepoint());
            } else {
                PARSE_ERROR();
                (void)next_codepoint();
                consume_the_remnants_of_a_bad_url();
                return create_new_token(Token::TokenType::BadUrl);
            }
        }

        token.m_value.append_code_point(next_codepoint().value());
    }
}

void Tokenizer::consume_the_remnants_of_a_bad_url()
{
    for (;;) {
        auto next = peek_codepoint();

        if (!next.has_value()) {
            return;
        }

        auto input = next.value();

        if (is_right_paren(input)) {
            (void)next_codepoint();
            return;
        }

        if (is_valid_escape_sequence()) {
            [[maybe_unused]] auto cp = consume_escaped_codepoint();
        }

        (void)next_codepoint();
    }
}

void Tokenizer::reconsume_current_input_codepoint()
{
    m_utf8_iterator = m_prev_utf8_iterator;
}

Token Tokenizer::consume_a_numeric_token()
{
    auto number = consume_a_number();
    if (would_start_an_identifier()) {
        auto token = create_new_token(Token::TokenType::Dimension);
        token.m_value.append(number.value);
        token.m_number_type = number.type;

        auto unit = consume_a_name();
        token.m_unit.append(unit);

        return token;
    }

    if (is_percent(peek_codepoint().value())) {
        (void)next_codepoint();

        auto token = create_new_token(Token::TokenType::Percentage);
        token.m_value.append(number.value);
        return token;
    }

    auto token = create_new_token(Token::TokenType::Number);
    token.m_value.append(number.value);
    token.m_number_type = number.type;
    return token;
}

bool Tokenizer::starts_with_a_number() const
{
    return starts_with_a_number(peek_triplet().value());
}

bool Tokenizer::starts_with_a_number(U32Triplet values)
{
    if (is_plus_sign(values.first) || is_hyphen_minus(values.first)) {
        if (isdigit(values.second))
            return true;

        if (is_full_stop(values.second) && isdigit(values.third))
            return true;

        return false;
    }

    if (is_full_stop(values.first))
        return isdigit(values.second);

    if (isdigit(values.first))
        return true;

    return false;
}

bool Tokenizer::is_valid_escape_sequence()
{
    return is_valid_escape_sequence(peek_twin().value());
}

bool Tokenizer::is_valid_escape_sequence(U32Twin values)
{
    if (!is_reverse_solidus(values.first)) {
        return false;
    }

    if (is_newline(values.second)) {
        return false;
    }

    return true;
}

bool Tokenizer::would_start_an_identifier()
{
    return would_start_an_identifier(peek_triplet().value());
}

bool Tokenizer::would_start_an_identifier(U32Triplet values)
{
    if (is_hyphen_minus(values.first)) {
        if (is_name_start_codepoint(values.second) || is_hyphen_minus(values.second) || is_valid_escape_sequence(values.to_twin_23()))
            return true;
        return false;
    }

    if (is_name_start_codepoint(values.first)) {
        return true;
    }

    if (is_reverse_solidus(values.first)) {
        if (is_valid_escape_sequence(values.to_twin_12()))
            return true;
        return false;
    }

    return false;
}

Token Tokenizer::consume_string_token(u32 ending_codepoint)
{
    auto token = create_new_token(Token::TokenType::String);

    for (;;) {
        auto codepoint = next_codepoint();

        if (!codepoint.has_value()) {
            PARSE_ERROR();
            return token;
        }

        auto input = codepoint.value();

        if (input == ending_codepoint)
            return token;

        if (is_newline(input)) {
            reconsume_current_input_codepoint();
            return create_new_token(Token::TokenType::BadString);
        }

        if (is_reverse_solidus(input)) {
            auto next_input = peek_codepoint();
            if (!next_input.has_value())
                continue;

            if (is_newline(next_input.value())) {
                (void)next_codepoint();
                continue;
            }

            auto escaped = consume_escaped_codepoint();
            token.m_value.append_code_point(escaped);
        }

        token.m_value.append_code_point(input);
    }
}

void Tokenizer::consume_comments()
{
start:
    auto peek = peek_twin();
    if (!peek.has_value()) {
        PARSE_ERROR();
        return;
    }

    auto twin = peek.value();
    if (!(is_solidus(twin.first) && is_asterisk(twin.second)))
        return;

    (void)next_codepoint();
    (void)next_codepoint();

    for (;;) {
        auto peek_inner = peek_twin();
        if (!peek_inner.has_value()) {
            PARSE_ERROR();
            return;
        }

        auto twin_inner = peek_inner.value();

        if (is_asterisk(twin_inner.first) && is_solidus(twin_inner.second)) {
            (void)next_codepoint();
            (void)next_codepoint();
            goto start;
        }

        (void)next_codepoint();
    }
}

Token Tokenizer::consume_a_token()
{
    consume_comments();

    auto codepoint = next_codepoint();

    if (!codepoint.has_value()) {
        return create_new_token(Token::TokenType::EndOfFile);
    }

    auto input = codepoint.value();

    if (is_whitespace(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is whitespace");

        while (is_whitespace(peek_codepoint().value()))
            (void)next_codepoint();

        return create_new_token(Token::TokenType::Whitespace);
    }

    if (is_quotation_mark(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is quotation mark");
        return consume_string_token(input);
    }

    if (is_number_sign(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is number sign");

        auto next_input = peek_codepoint().value();
        auto maybe_escape = peek_twin().value();

        if (is_name_codepoint(next_input) || is_valid_escape_sequence(maybe_escape)) {
            auto token = create_new_token(Token::TokenType::Hash);

            if (would_start_an_identifier())
                token.m_hash_type = Token::HashType::Id;

            auto name = consume_a_name();
            token.m_value.append(name);

            return token;
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_apostrophe(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is apostrophe");
        return consume_string_token(input);
    }

    if (is_left_paren(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is left paren");
        return create_new_token(Token::TokenType::OpenParen);
    }

    if (is_right_paren(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is right paren");
        return create_new_token(Token::TokenType::CloseParen);
    }

    if (is_plus_sign(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is plus sign");
        if (starts_with_a_number()) {
            reconsume_current_input_codepoint();
            return consume_a_numeric_token();
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_comma(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is comma");
        return create_new_token(Token::TokenType::Comma);
    }

    if (is_hyphen_minus(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is hyphen minus");
        if (starts_with_a_number()) {
            reconsume_current_input_codepoint();
            return consume_a_numeric_token();
        }

        auto next_twin = peek_twin().value();
        if (is_hyphen_minus(next_twin.first) && is_greater_than_sign(next_twin.second)) {
            (void)next_codepoint();
            (void)next_codepoint();

            return create_new_token(Token::TokenType::CDC);
        }

        if (would_start_an_identifier()) {
            reconsume_current_input_codepoint();
            return consume_an_ident_like_token();
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_full_stop(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is full stop");
        if (starts_with_a_number()) {
            reconsume_current_input_codepoint();
            return consume_a_numeric_token();
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_colon(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is colon");
        return create_new_token(Token::TokenType::Colon);
    }

    if (is_semicolon(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is semicolon");
        return create_new_token(Token::TokenType::Semicolon);
    }

    if (is_less_than_sign(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is less than");
        auto maybe_cdo = peek_triplet().value();

        if (is_exclamation_mark(maybe_cdo.first) && is_hyphen_minus(maybe_cdo.second) && is_hyphen_minus(maybe_cdo.third)) {
            (void)next_codepoint();
            (void)next_codepoint();
            (void)next_codepoint();

            return create_new_token(Token::TokenType::CDO);
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_at(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is at");
        if (would_start_an_identifier()) {
            auto name = consume_a_name();

            return create_value_token(Token::TokenType::AtKeyword, input);
        }

        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_open_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is open square");
        return create_new_token(Token::TokenType::OpenSquare);
    }

    if (is_reverse_solidus(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is reverse solidus");
        if (is_valid_escape_sequence()) {
            reconsume_current_input_codepoint();
            return consume_an_ident_like_token();
        }

        PARSE_ERROR();
        return create_value_token(Token::TokenType::Delim, input);
    }

    if (is_closed_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is closed square");
        return create_new_token(Token::TokenType::CloseSquare);
    }

    if (is_open_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is open curly");
        return create_new_token(Token::TokenType::OpenCurly);
    }

    if (is_closed_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is closed curly");
        return create_new_token(Token::TokenType::CloseCurly);
    }

    if (isdigit(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is digit");
        reconsume_current_input_codepoint();
        return consume_a_numeric_token();
    }

    if (is_name_start_codepoint(input)) {
        dbgln_if(CSS_TOKENIZER_TRACE, "is name start");
        reconsume_current_input_codepoint();
        return consume_an_ident_like_token();
    }

    dbgln_if(CSS_TOKENIZER_TRACE, "is delimiter");
    return create_value_token(Token::TokenType::Delim, input);
}

}
