/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/SourceLocation.h>
#include <AK/Vector.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <math.h>

// U+FFFD REPLACEMENT CHARACTER (�)
#define REPLACEMENT_CHARACTER 0xFFFD
static const u32 TOKENIZER_EOF = 0xFFFFFFFF;

static inline void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln_if(CSS_TOKENIZER_DEBUG, "Parse error (css tokenization) {} ", location);
}

static inline bool is_eof(u32 code_point)
{
    return code_point == TOKENIZER_EOF;
}

static inline bool is_quotation_mark(u32 code_point)
{
    return code_point == 0x22;
}

static inline bool is_greater_than_maximum_allowed_code_point(u32 code_point)
{
    return code_point > 0x10FFFF;
}

static inline bool is_low_line(u32 code_point)
{
    return code_point == 0x5F;
}

static inline bool is_name_start_code_point(u32 code_point)
{
    // FIXME: We use !is_ascii() for "non-ASCII code point" in the spec, but it's not quite right -
    //        it treats EOF as a valid! The spec also lacks a definition of code point. For now, the
    //        !is_eof() check is a hack, but it should work.
    return !is_eof(code_point) && (is_ascii_alpha(code_point) || !is_ascii(code_point) || is_low_line(code_point));
}

static inline bool is_hyphen_minus(u32 code_point)
{
    return code_point == 0x2D;
}

static inline bool is_name_code_point(u32 code_point)
{
    return is_name_start_code_point(code_point) || is_ascii_digit(code_point) || is_hyphen_minus(code_point);
}

static inline bool is_non_printable(u32 code_point)
{
    return code_point <= 0x8 || code_point == 0xB || (code_point >= 0xE && code_point <= 0x1F) || code_point == 0x7F;
}

static inline bool is_number_sign(u32 code_point)
{
    return code_point == 0x23;
}

static inline bool is_reverse_solidus(u32 code_point)
{
    return code_point == 0x5C;
}

static inline bool is_apostrophe(u32 code_point)
{
    return code_point == 0x27;
}

static inline bool is_left_paren(u32 code_point)
{
    return code_point == 0x28;
}

static inline bool is_right_paren(u32 code_point)
{
    return code_point == 0x29;
}

static inline bool is_plus_sign(u32 code_point)
{
    return code_point == 0x2B;
}

static inline bool is_comma(u32 code_point)
{
    return code_point == 0x2C;
}

static inline bool is_full_stop(u32 code_point)
{
    return code_point == 0x2E;
}

static inline bool is_newline(u32 code_point)
{
    return code_point == 0xA;
}

static inline bool is_asterisk(u32 code_point)
{
    return code_point == 0x2A;
}

static inline bool is_solidus(u32 code_point)
{
    return code_point == 0x2F;
}

static inline bool is_colon(u32 code_point)
{
    return code_point == 0x3A;
}

static inline bool is_semicolon(u32 code_point)
{
    return code_point == 0x3B;
}

static inline bool is_less_than_sign(u32 code_point)
{
    return code_point == 0x3C;
}

static inline bool is_greater_than_sign(u32 code_point)
{
    return code_point == 0x3E;
}

static inline bool is_at(u32 code_point)
{
    return code_point == 0x40;
}

static inline bool is_open_square_bracket(u32 code_point)
{
    return code_point == 0x5B;
}

static inline bool is_closed_square_bracket(u32 code_point)
{
    return code_point == 0x5D;
}

static inline bool is_open_curly_bracket(u32 code_point)
{
    return code_point == 0x7B;
}

static inline bool is_closed_curly_bracket(u32 code_point)
{
    return code_point == 0x7D;
}

static inline bool is_whitespace(u32 code_point)
{
    return code_point == 0x9 || code_point == 0xA || code_point == 0x20;
}

static inline bool is_percent(u32 code_point)
{
    return code_point == 0x25;
}

static inline bool is_exclamation_mark(u32 code_point)
{
    return code_point == 0x21;
}

static inline bool is_e(u32 code_point)
{
    return code_point == 0x65;
}

static inline bool is_E(u32 code_point)
{
    return code_point == 0x45;
}

namespace Web::CSS {

Tokenizer::Tokenizer(StringView input, const String& encoding)
{
    auto* decoder = TextCodec::decoder_for(encoding);
    VERIFY(decoder);

    StringBuilder builder(input.length());

    // Preprocess the stream, by doing the following:
    // - Replace \r, \f and \r\n with \n
    // - replace \0 and anything between U+D800 to U+DFFF with the replacement
    //   character.
    // https://www.w3.org/TR/css-syntax-3/#input-preprocessing
    bool last_was_carriage_return = false;
    decoder->process(input, [&builder, &last_was_carriage_return](u32 code_point) {
        if (code_point == '\r') {
            if (last_was_carriage_return) {
                builder.append('\n');
            } else {
                last_was_carriage_return = true;
            }
        } else {
            if (last_was_carriage_return) {
                builder.append('\n');
            }

            if (code_point == '\n') {
                if (!last_was_carriage_return) {
                    builder.append('\n');
                }
            } else if (code_point == '\f') {
                builder.append('\n');
            } else if (code_point >= 0xD800 && code_point <= 0xDFFF) {
                builder.append_code_point(REPLACEMENT_CHARACTER);
            } else {
                builder.append_code_point(code_point);
            }

            last_was_carriage_return = false;
        }
    });

    m_decoded_input = builder.to_string();
    m_utf8_view = Utf8View(m_decoded_input);
    m_utf8_iterator = m_utf8_view.begin();
}

Vector<Token> Tokenizer::parse()
{
    Vector<Token> tokens;
    for (;;) {
        auto token_start = m_position;
        auto token = consume_a_token();
        token.m_start_position = token_start;
        token.m_end_position = m_position;
        tokens.append(token);

        if (token.is(Token::Type::EndOfFile)) {
            return tokens;
        }
    }
}

u32 Tokenizer::next_code_point()
{
    if (m_utf8_iterator == m_utf8_view.end())
        return TOKENIZER_EOF;
    m_prev_utf8_iterator = m_utf8_iterator;
    ++m_utf8_iterator;
    auto code_point = *m_prev_utf8_iterator;

    m_prev_position = m_position;
    if (is_newline(code_point)) {
        m_position.line++;
        m_position.column = 0;
    } else {
        m_position.column++;
    }

    dbgln_if(CSS_TOKENIZER_DEBUG, "(Tokenizer) Next code_point: {:d}", code_point);
    return code_point;
}

u32 Tokenizer::peek_code_point(size_t offset) const
{
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < offset && it != m_utf8_view.end(); ++i)
        ++it;
    if (it == m_utf8_view.end())
        return TOKENIZER_EOF;
    dbgln_if(CSS_TOKENIZER_DEBUG, "(Tokenizer) Peek code_point: {:d}", *m_prev_utf8_iterator);
    return *it;
}

U32Twin Tokenizer::peek_twin() const
{
    U32Twin values { TOKENIZER_EOF, TOKENIZER_EOF };
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < 2 && it != m_utf8_view.end(); ++i) {
        values.set(i, *it);
        ++it;
    }
    dbgln_if(CSS_TOKENIZER_DEBUG, "(Tokenizer) Peek twin: {:d},{:d}", values.first, values.second);
    return values;
}

U32Triplet Tokenizer::peek_triplet() const
{
    U32Triplet values { TOKENIZER_EOF, TOKENIZER_EOF, TOKENIZER_EOF };
    auto it = m_utf8_iterator;
    for (size_t i = 0; i < 3 && it != m_utf8_view.end(); ++i) {
        values.set(i, *it);
        ++it;
    }
    dbgln_if(CSS_TOKENIZER_DEBUG, "(Tokenizer) Peek triplet: {:d},{:d},{:d}", values.first, values.second, values.third);
    return values;
}

Token Tokenizer::create_new_token(Token::Type type)
{
    Token token = {};
    token.m_type = type;
    return token;
}

Token Tokenizer::create_eof_token()
{
    return create_new_token(Token::Type::EndOfFile);
}

Token Tokenizer::create_value_token(Token::Type type, String value)
{
    Token token;
    token.m_type = type;
    token.m_value = move(value);
    return token;
}

Token Tokenizer::create_value_token(Token::Type type, u32 value)
{
    Token token = {};
    token.m_type = type;
    // FIXME: Avoid temporary StringBuilder here
    StringBuilder builder;
    builder.append_code_point(value);
    token.m_value = builder.to_string();
    return token;
}

// https://www.w3.org/TR/css-syntax-3/#consume-escaped-code-point
u32 Tokenizer::consume_escaped_code_point()
{
    auto input = next_code_point();

    if (is_eof(input)) {
        log_parse_error();
        return REPLACEMENT_CHARACTER;
    }

    if (is_ascii_hex_digit(input)) {
        StringBuilder builder;
        builder.append_code_point(input);

        size_t counter = 0;
        while (is_ascii_hex_digit(peek_code_point()) && counter++ < 5) {
            builder.append_code_point(next_code_point());
        }

        if (is_whitespace(peek_code_point())) {
            (void)next_code_point();
        }

        auto unhexed = strtoul(builder.to_string().characters(), nullptr, 16);
        if (unhexed == 0 || is_unicode_surrogate(unhexed) || is_greater_than_maximum_allowed_code_point(unhexed)) {
            return REPLACEMENT_CHARACTER;
        }

        return unhexed;
    }

    if (!input) {
        log_parse_error();
        return REPLACEMENT_CHARACTER;
    }

    return input;
}

// https://www.w3.org/TR/css-syntax-3/#consume-ident-like-token
Token Tokenizer::consume_an_ident_like_token()
{
    auto string = consume_a_name();

    if (string.equals_ignoring_case("url") && is_left_paren(peek_code_point())) {
        (void)next_code_point();

        for (;;) {
            auto maybe_whitespace = peek_twin();
            if (!(is_whitespace(maybe_whitespace.first) && is_whitespace(maybe_whitespace.second))) {
                break;
            }

            (void)next_code_point();
        }

        auto next_two = peek_twin();
        // if one of these ", ', ' "', " '"
        if (is_quotation_mark(next_two.first) || is_apostrophe(next_two.first) || (is_whitespace(next_two.first) && (is_quotation_mark(next_two.second) || is_apostrophe(next_two.second)))) {
            return create_value_token(Token::Type::Function, string);
        }

        return consume_a_url_token();
    }

    if (is_left_paren(peek_code_point())) {
        (void)next_code_point();

        return create_value_token(Token::Type::Function, string);
    }

    return create_value_token(Token::Type::Ident, string);
}

// https://www.w3.org/TR/css-syntax-3/#consume-number
CSSNumber Tokenizer::consume_a_number()
{
    StringBuilder repr;
    Token::NumberType type = Token::NumberType::Integer;

    auto next_input = peek_code_point();
    if (is_plus_sign(next_input) || is_hyphen_minus(next_input)) {
        repr.append_code_point(next_code_point());
    }

    for (;;) {
        auto digits = peek_code_point();
        if (!is_ascii_digit(digits))
            break;

        repr.append_code_point(next_code_point());
    }

    auto maybe_number = peek_twin();
    if (is_full_stop(maybe_number.first) && is_ascii_digit(maybe_number.second)) {
        repr.append_code_point(next_code_point());
        repr.append_code_point(next_code_point());

        type = Token::NumberType::Number;

        for (;;) {
            auto digit = peek_code_point();
            if (!is_ascii_digit(digit))
                break;

            repr.append_code_point(next_code_point());
        }
    }

    auto maybe_exp = peek_triplet();
    if (is_E(maybe_exp.first) || is_e(maybe_exp.first)) {
        if (is_plus_sign(maybe_exp.second) || is_hyphen_minus(maybe_exp.second)) {
            if (is_ascii_digit(maybe_exp.third)) {
                repr.append_code_point(next_code_point());
                repr.append_code_point(next_code_point());
                repr.append_code_point(next_code_point());
            }
        } else if (is_ascii_digit(maybe_exp.second)) {
            repr.append_code_point(next_code_point());
            repr.append_code_point(next_code_point());
        }

        type = Token::NumberType::Number;

        for (;;) {
            auto digits = peek_code_point();
            if (!is_ascii_digit(digits))
                break;

            repr.append_code_point(next_code_point());
        }
    }

    return { repr.to_string(), convert_a_string_to_a_number(repr.string_view()), type };
}

// https://www.w3.org/TR/css-syntax-3/#convert-string-to-number
double Tokenizer::convert_a_string_to_a_number(StringView string)
{
    auto code_point_at = [&](size_t index) -> u32 {
        if (index < string.length())
            return string[index];
        return TOKENIZER_EOF;
    };

    // This algorithm does not do any verification to ensure that the string contains only a number.
    // Ensure that the string contains only a valid CSS number before calling this algorithm.

    // Divide the string into seven components, in order from left to right:
    size_t position = 0;

    // 1. A sign: a single U+002B PLUS SIGN (+) or U+002D HYPHEN-MINUS (-), or the empty string.
    //    Let s [sign] be the number -1 if the sign is U+002D HYPHEN-MINUS (-); otherwise, let s be the number 1.
    int sign = 1;
    if (is_plus_sign(code_point_at(position)) || is_hyphen_minus(code_point_at(position))) {
        sign = is_hyphen_minus(code_point_at(position)) ? -1 : 1;
        position++;
    }

    // 2. An integer part: zero or more digits.
    //    If there is at least one digit, let i [integer_part] be the number formed by interpreting the digits
    //    as a base-10 integer; otherwise, let i be the number 0.
    double integer_part = 0;
    while (is_ascii_digit(code_point_at(position))) {
        integer_part = (integer_part * 10) + (code_point_at(position) - '0');
        position++;
    }

    // 3. A decimal point: a single U+002E FULL STOP (.), or the empty string.
    if (is_full_stop(code_point_at(position)))
        position++;

    // 4. A fractional part: zero or more digits.
    //    If there is at least one digit, let f [fractional_part] be the number formed by interpreting the digits
    //    as a base-10 integer and d [fractional_digits] be the number of digits; otherwise, let f and d be the number 0.
    double fractional_part = 0;
    int fractional_digits = 0;
    while (is_ascii_digit(code_point_at(position))) {
        fractional_part = (fractional_part * 10) + (code_point_at(position) - '0');
        position++;
        fractional_digits++;
    }

    // 5. An exponent indicator: a single U+0045 LATIN CAPITAL LETTER E (E) or U+0065 LATIN SMALL LETTER E (e),
    //    or the empty string.
    if (is_e(code_point_at(position)) || is_E(code_point_at(position)))
        position++;

    // 6. An exponent sign: a single U+002B PLUS SIGN (+) or U+002D HYPHEN-MINUS (-), or the empty string.
    //    Let t [exponent_sign] be the number -1 if the sign is U+002D HYPHEN-MINUS (-); otherwise, let t be the number 1.
    int exponent_sign = 1;
    if (is_plus_sign(code_point_at(position)) || is_hyphen_minus(code_point_at(position))) {
        exponent_sign = is_hyphen_minus(code_point_at(position)) ? -1 : 1;
        position++;
    }

    // 7. An exponent: zero or more digits.
    //    If there is at least one digit, let e [exponent] be the number formed by interpreting the digits as a
    //    base-10 integer; otherwise, let e be the number 0.
    double exponent = 0;
    while (is_ascii_digit(code_point_at(position))) {
        exponent = (exponent * 10) + (code_point_at(position) - '0');
        position++;
    }

    // NOTE: We checked before calling this function that the string is a valid number,
    //       so if there is anything at the end, something has gone wrong!
    VERIFY(position == string.length());

    // Return the number s·(i + f·10^-d)·10^te.
    return sign * (integer_part + fractional_part * pow(10, -fractional_digits)) * pow(10, exponent_sign * exponent);
}

// https://www.w3.org/TR/css-syntax-3/#consume-name
String Tokenizer::consume_a_name()
{
    StringBuilder result;

    for (;;) {
        auto input = next_code_point();

        if (is_eof(input))
            break;

        if (is_name_code_point(input)) {
            result.append_code_point(input);
            continue;
        }

        auto next = peek_code_point();
        if (!is_eof(next) && is_valid_escape_sequence({ input, next })) {
            result.append_code_point(consume_escaped_code_point());
            continue;
        }

        reconsume_current_input_code_point();
        break;
    }

    return result.to_string();
}
Token Tokenizer::consume_a_url_token()
{
    auto token = create_new_token(Token::Type::Url);
    consume_as_much_whitespace_as_possible();
    StringBuilder builder;

    auto make_token = [&]() {
        token.m_value = builder.to_string();
        return token;
    };

    for (;;) {

        auto input = peek_code_point();
        if (is_eof(input)) {
            log_parse_error();
            return make_token();
        }

        if (is_right_paren(input)) {
            (void)next_code_point();
            return make_token();
        }

        if (is_whitespace(input)) {
            consume_as_much_whitespace_as_possible();
            input = peek_code_point();

            if (is_eof(input)) {
                log_parse_error();
                return make_token();
            }

            if (is_right_paren(input)) {
                return make_token();
            }

            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::Type::BadUrl);
        }

        if (is_quotation_mark(input) || is_apostrophe(input) || is_left_paren(input) || is_non_printable(input)) {
            log_parse_error();
            (void)next_code_point();
            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::Type::BadUrl);
        }

        if (is_reverse_solidus(input)) {
            if (is_valid_escape_sequence(peek_twin())) {
                builder.append_code_point(consume_escaped_code_point());
            } else {
                log_parse_error();
                (void)next_code_point();
                consume_the_remnants_of_a_bad_url();
                return create_new_token(Token::Type::BadUrl);
            }
        }

        builder.append_code_point(next_code_point());
    }
}

// https://www.w3.org/TR/css-syntax-3/#consume-remnants-of-bad-url
void Tokenizer::consume_the_remnants_of_a_bad_url()
{
    for (;;) {
        auto next = peek_code_point();

        if (is_eof(next)) {
            return;
        }

        auto input = next;

        if (is_right_paren(input)) {
            (void)next_code_point();
            return;
        }

        if (is_valid_escape_sequence(peek_twin())) {
            [[maybe_unused]] auto cp = consume_escaped_code_point();
        }

        (void)next_code_point();
    }
}

void Tokenizer::consume_as_much_whitespace_as_possible()
{
    while (is_whitespace(peek_code_point())) {
        (void)next_code_point();
    }
}

void Tokenizer::reconsume_current_input_code_point()
{
    m_utf8_iterator = m_prev_utf8_iterator;
    m_position = m_prev_position;
}

// https://www.w3.org/TR/css-syntax-3/#consume-numeric-token
Token Tokenizer::consume_a_numeric_token()
{
    auto number = consume_a_number();
    if (would_start_an_identifier()) {
        auto token = create_new_token(Token::Type::Dimension);
        token.m_value = move(number.string);
        token.m_number_type = number.type;
        token.m_number_value = number.value;

        auto unit = consume_a_name();
        VERIFY(!unit.is_empty() && !unit.is_whitespace());
        token.m_unit = move(unit);

        return token;
    }

    if (is_percent(peek_code_point())) {
        (void)next_code_point();

        auto token = create_new_token(Token::Type::Percentage);
        token.m_value = move(number.string);
        token.m_number_type = number.type;
        token.m_number_value = number.value;
        return token;
    }

    auto token = create_new_token(Token::Type::Number);
    token.m_value = move(number.string);
    token.m_number_type = number.type;
    token.m_number_value = number.value;
    return token;
}

bool Tokenizer::would_start_a_number() const
{
    return would_start_a_number(peek_triplet());
}

// https://www.w3.org/TR/css-syntax-3/#starts-with-a-number
bool Tokenizer::would_start_a_number(U32Triplet values)
{
    if (is_plus_sign(values.first) || is_hyphen_minus(values.first)) {
        if (is_ascii_digit(values.second))
            return true;

        if (is_full_stop(values.second) && is_ascii_digit(values.third))
            return true;

        return false;
    }

    if (is_full_stop(values.first))
        return is_ascii_digit(values.second);

    if (is_ascii_digit(values.first))
        return true;

    return false;
}

// https://www.w3.org/TR/css-syntax-3/#starts-with-a-valid-escape
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
    return would_start_an_identifier(peek_triplet());
}

// https://www.w3.org/TR/css-syntax-3/#would-start-an-identifier
bool Tokenizer::would_start_an_identifier(U32Triplet values)
{
    if (is_hyphen_minus(values.first)) {
        if (is_name_start_code_point(values.second) || is_hyphen_minus(values.second) || is_valid_escape_sequence(values.to_twin_23()))
            return true;
        return false;
    }

    if (is_name_start_code_point(values.first)) {
        return true;
    }

    if (is_reverse_solidus(values.first)) {
        if (is_valid_escape_sequence(values.to_twin_12()))
            return true;
        return false;
    }

    return false;
}

// https://www.w3.org/TR/css-syntax-3/#consume-string-token
Token Tokenizer::consume_string_token(u32 ending_code_point)
{
    auto token = create_new_token(Token::Type::String);
    StringBuilder builder;

    auto make_token = [&]() {
        token.m_value = builder.to_string();
        return token;
    };

    for (;;) {
        auto input = next_code_point();

        if (is_eof(input)) {
            log_parse_error();
            return make_token();
        }

        if (input == ending_code_point)
            return make_token();

        if (is_newline(input)) {
            reconsume_current_input_code_point();
            return create_new_token(Token::Type::BadString);
        }

        if (is_reverse_solidus(input)) {
            auto next_input = peek_code_point();
            if (is_eof(next_input))
                continue;

            if (is_newline(next_input)) {
                (void)next_code_point();
                continue;
            }

            auto escaped = consume_escaped_code_point();
            builder.append_code_point(escaped);
        }

        builder.append_code_point(input);
    }
}

// https://www.w3.org/TR/css-syntax-3/#consume-comment
void Tokenizer::consume_comments()
{
start:
    auto twin = peek_twin();
    if (!(is_solidus(twin.first) && is_asterisk(twin.second)))
        return;

    (void)next_code_point();
    (void)next_code_point();

    for (;;) {
        auto twin_inner = peek_twin();
        if (is_eof(twin_inner.first) || is_eof(twin_inner.second)) {
            log_parse_error();
            return;
        }

        if (is_asterisk(twin_inner.first) && is_solidus(twin_inner.second)) {
            (void)next_code_point();
            (void)next_code_point();
            goto start;
        }

        (void)next_code_point();
    }
}

// https://www.w3.org/TR/css-syntax-3/#consume-token
Token Tokenizer::consume_a_token()
{
    consume_comments();

    auto input = next_code_point();

    if (is_eof(input)) {
        return create_new_token(Token::Type::EndOfFile);
    }

    if (is_whitespace(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is whitespace");
        consume_as_much_whitespace_as_possible();
        return create_new_token(Token::Type::Whitespace);
    }

    if (is_quotation_mark(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is quotation mark");
        return consume_string_token(input);
    }

    if (is_number_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is number sign");

        auto next_input = peek_code_point();
        auto maybe_escape = peek_twin();

        if (is_name_code_point(next_input) || is_valid_escape_sequence(maybe_escape)) {
            auto token = create_new_token(Token::Type::Hash);

            if (would_start_an_identifier())
                token.m_hash_type = Token::HashType::Id;

            auto name = consume_a_name();
            token.m_value = move(name);

            return token;
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_apostrophe(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is apostrophe");
        return consume_string_token(input);
    }

    if (is_left_paren(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is left paren");
        return create_new_token(Token::Type::OpenParen);
    }

    if (is_right_paren(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is right paren");
        return create_new_token(Token::Type::CloseParen);
    }

    if (is_plus_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is plus sign");
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_comma(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is comma");
        return create_new_token(Token::Type::Comma);
    }

    if (is_hyphen_minus(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is hyphen minus");
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        auto next_twin = peek_twin();
        if (is_hyphen_minus(next_twin.first) && is_greater_than_sign(next_twin.second)) {
            (void)next_code_point();
            (void)next_code_point();

            return create_new_token(Token::Type::CDC);
        }

        if (would_start_an_identifier()) {
            reconsume_current_input_code_point();
            return consume_an_ident_like_token();
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_full_stop(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is full stop");
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_colon(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is colon");
        return create_new_token(Token::Type::Colon);
    }

    if (is_semicolon(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is semicolon");
        return create_new_token(Token::Type::Semicolon);
    }

    if (is_less_than_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is less than");
        auto maybe_cdo = peek_triplet();

        if (is_exclamation_mark(maybe_cdo.first) && is_hyphen_minus(maybe_cdo.second) && is_hyphen_minus(maybe_cdo.third)) {
            (void)next_code_point();
            (void)next_code_point();
            (void)next_code_point();

            return create_new_token(Token::Type::CDO);
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_at(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is at");
        if (would_start_an_identifier()) {
            auto name = consume_a_name();

            return create_value_token(Token::Type::AtKeyword, name);
        }

        return create_value_token(Token::Type::Delim, input);
    }

    if (is_open_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is open square");
        return create_new_token(Token::Type::OpenSquare);
    }

    if (is_reverse_solidus(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is reverse solidus");
        if (is_valid_escape_sequence({ input, peek_code_point() })) {
            reconsume_current_input_code_point();
            return consume_an_ident_like_token();
        }

        log_parse_error();
        return create_value_token(Token::Type::Delim, input);
    }

    if (is_closed_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is closed square");
        return create_new_token(Token::Type::CloseSquare);
    }

    if (is_open_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is open curly");
        return create_new_token(Token::Type::OpenCurly);
    }

    if (is_closed_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is closed curly");
        return create_new_token(Token::Type::CloseCurly);
    }

    if (is_ascii_digit(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is digit");
        reconsume_current_input_code_point();
        return consume_a_numeric_token();
    }

    if (is_name_start_code_point(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is name start");
        reconsume_current_input_code_point();
        return consume_an_ident_like_token();
    }

    dbgln_if(CSS_TOKENIZER_DEBUG, "is delimiter");
    return create_value_token(Token::Type::Delim, input);
}

}
