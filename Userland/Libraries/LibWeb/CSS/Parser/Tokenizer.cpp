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
    // This section describes how to consume an escaped code point.
    // It assumes that the U+005C REVERSE SOLIDUS (\) has already been consumed and that the next
    // input code point has already been verified to be part of a valid escape.
    // It will return a code point.

    // Consume the next input code point.
    auto input = next_code_point();

    // hex digit
    if (is_ascii_hex_digit(input)) {
        // Consume as many hex digits as possible, but no more than 5.
        // Note that this means 1-6 hex digits have been consumed in total.
        StringBuilder builder;
        builder.append_code_point(input);

        size_t counter = 0;
        while (is_ascii_hex_digit(peek_code_point()) && counter++ < 5) {
            builder.append_code_point(next_code_point());
        }

        // If the next input code point is whitespace, consume it as well.
        if (is_whitespace(peek_code_point())) {
            (void)next_code_point();
        }

        // Interpret the hex digits as a hexadecimal number.
        auto unhexed = strtoul(builder.to_string().characters(), nullptr, 16);
        // If this number is zero, or is for a surrogate, or is greater than the maximum allowed
        // code point, return U+FFFD REPLACEMENT CHARACTER (�).
        if (unhexed == 0 || is_unicode_surrogate(unhexed) || is_greater_than_maximum_allowed_code_point(unhexed)) {
            return REPLACEMENT_CHARACTER;
        }

        // Otherwise, return the code point with that value.
        return unhexed;
    }

    // EOF
    if (is_eof(input)) {
        // This is a parse error. Return U+FFFD REPLACEMENT CHARACTER (�).
        log_parse_error();
        return REPLACEMENT_CHARACTER;
    }

    // anything else
    // Return the current input code point.
    return input;
}

// https://www.w3.org/TR/css-syntax-3/#consume-ident-like-token
Token Tokenizer::consume_an_ident_like_token()
{
    // This section describes how to consume an ident-like token from a stream of code points.
    // It returns an <ident-token>, <function-token>, <url-token>, or <bad-url-token>.

    // Consume a name, and let string be the result.
    auto string = consume_a_name();

    // If string’s value is an ASCII case-insensitive match for "url", and the next input code
    // point is U+0028 LEFT PARENTHESIS ((), consume it.
    if (string.equals_ignoring_case("url") && is_left_paren(peek_code_point())) {
        (void)next_code_point();

        // While the next two input code points are whitespace, consume the next input code point.
        for (;;) {
            auto maybe_whitespace = peek_twin();
            if (!(is_whitespace(maybe_whitespace.first) && is_whitespace(maybe_whitespace.second))) {
                break;
            }

            (void)next_code_point();
        }

        // If the next one or two input code points are U+0022 QUOTATION MARK ("), U+0027 APOSTROPHE ('),
        // or whitespace followed by U+0022 QUOTATION MARK (") or U+0027 APOSTROPHE ('), then create a
        // <function-token> with its value set to string and return it.
        auto next_two = peek_twin();
        if (is_quotation_mark(next_two.first) || is_apostrophe(next_two.first) || (is_whitespace(next_two.first) && (is_quotation_mark(next_two.second) || is_apostrophe(next_two.second)))) {
            return create_value_token(Token::Type::Function, string);
        }

        // Otherwise, consume a url token, and return it.
        return consume_a_url_token();
    }

    // Otherwise, if the next input code point is U+0028 LEFT PARENTHESIS ((), consume it.
    if (is_left_paren(peek_code_point())) {
        (void)next_code_point();

        // Create a <function-token> with its value set to string and return it.
        return create_value_token(Token::Type::Function, string);
    }

    // Otherwise, create an <ident-token> with its value set to string and return it.
    return create_value_token(Token::Type::Ident, string);
}

// https://www.w3.org/TR/css-syntax-3/#consume-number
CSSNumber Tokenizer::consume_a_number()
{
    // This section describes how to consume a number from a stream of code points.
    // It returns a numeric value, and a type which is either "integer" or "number".
    //
    // Note: This algorithm does not do the verification of the first few code points
    // that are necessary to ensure a number can be obtained from the stream. Ensure
    // that the stream starts with a number before calling this algorithm.

    // Execute the following steps in order:

    // 1. Initially set type to "integer". Let repr be the empty string.
    StringBuilder repr;
    Token::NumberType type = Token::NumberType::Integer;

    // 2. If the next input code point is U+002B PLUS SIGN (+) or U+002D HYPHEN-MINUS (-),
    // consume it and append it to repr.
    auto next_input = peek_code_point();
    if (is_plus_sign(next_input) || is_hyphen_minus(next_input)) {
        repr.append_code_point(next_code_point());
    }

    // 3. While the next input code point is a digit, consume it and append it to repr.
    for (;;) {
        auto digits = peek_code_point();
        if (!is_ascii_digit(digits))
            break;

        repr.append_code_point(next_code_point());
    }

    // 4. If the next 2 input code points are U+002E FULL STOP (.) followed by a digit, then:
    auto maybe_number = peek_twin();
    if (is_full_stop(maybe_number.first) && is_ascii_digit(maybe_number.second)) {
        // 1. Consume them.
        // 2. Append them to repr.
        repr.append_code_point(next_code_point());
        repr.append_code_point(next_code_point());

        // 3. Set type to "number".
        type = Token::NumberType::Number;

        // 4. While the next input code point is a digit, consume it and append it to repr.
        for (;;) {
            auto digit = peek_code_point();
            if (!is_ascii_digit(digit))
                break;

            repr.append_code_point(next_code_point());
        }
    }

    // 5. If the next 2 or 3 input code points are U+0045 LATIN CAPITAL LETTER E (E) or
    // U+0065 LATIN SMALL LETTER E (e), optionally followed by U+002D HYPHEN-MINUS (-)
    // or U+002B PLUS SIGN (+), followed by a digit, then:
    auto maybe_exp = peek_triplet();
    if (is_E(maybe_exp.first) || is_e(maybe_exp.first)) {
        // 1. Consume them.
        // 2. Append them to repr.
        // FIXME: These conditions should be part of step 5 above.
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

        // 3. Set type to "number".
        type = Token::NumberType::Number;

        // 4. While the next input code point is a digit, consume it and append it to repr.
        for (;;) {
            auto digits = peek_code_point();
            if (!is_ascii_digit(digits))
                break;

            repr.append_code_point(next_code_point());
        }
    }

    // 6. Convert repr to a number, and set the value to the returned value.
    auto value = convert_a_string_to_a_number(repr.string_view());

    // 7. Return value and type.
    return { repr.to_string(), value, type };
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
    // This section describes how to consume a name from a stream of code points.
    // It returns a string containing the largest name that can be formed from adjacent
    // code points in the stream, starting from the first.
    //
    // Note: This algorithm does not do the verification of the first few code points that
    // are necessary to ensure the returned code points would constitute an <ident-token>.
    // If that is the intended use, ensure that the stream starts with an identifier before
    // calling this algorithm.

    // Let result initially be an empty string.
    StringBuilder result;

    // Repeatedly consume the next input code point from the stream:
    for (;;) {
        auto input = next_code_point();

        if (is_eof(input))
            break;

        // name code point
        if (is_name_code_point(input)) {
            // Append the code point to result.
            result.append_code_point(input);
            continue;
        }

        // the stream starts with a valid escape
        auto next = peek_code_point();
        if (!is_eof(next) && is_valid_escape_sequence({ input, next })) {
            // Consume an escaped code point. Append the returned code point to result.
            result.append_code_point(consume_escaped_code_point());
            continue;
        }

        // anything else
        // Reconsume the current input code point. Return result.
        reconsume_current_input_code_point();
        break;
    }

    return result.to_string();
}

// https://www.w3.org/TR/css-syntax-3/#consume-url-token
Token Tokenizer::consume_a_url_token()
{
    // This section describes how to consume a url token from a stream of code points.
    // It returns either a <url-token> or a <bad-url-token>.
    //
    // Note: This algorithm assumes that the initial "url(" has already been consumed.
    // This algorithm also assumes that it’s being called to consume an "unquoted" value,
    // like url(foo). A quoted value, like url("foo"), is parsed as a <function-token>.
    // Consume an ident-like token automatically handles this distinction; this algorithm
    // shouldn’t be called directly otherwise.

    // 1. Initially create a <url-token> with its value set to the empty string.
    auto token = create_new_token(Token::Type::Url);
    StringBuilder builder;

    // 2. Consume as much whitespace as possible.
    consume_as_much_whitespace_as_possible();

    auto make_token = [&]() {
        token.m_value = builder.to_string();
        return token;
    };

    // 3. Repeatedly consume the next input code point from the stream:
    for (;;) {
        // NOTE: We peek here instead of consuming, so that we can peek a twin later
        // to determine if it's a valid escape sequence.
        auto input = peek_code_point();

        // U+0029 RIGHT PARENTHESIS ())
        if (is_right_paren(input)) {
            // Return the <url-token>.
            (void)next_code_point(); // Not to spec, see NOTE above.
            return make_token();
        }

        // EOF
        if (is_eof(input)) {
            // This is a parse error. Return the <url-token>.
            log_parse_error();
            return make_token();
        }

        // whitespace
        if (is_whitespace(input)) {
            // Consume as much whitespace as possible.
            consume_as_much_whitespace_as_possible();

            // If the next input code point is U+0029 RIGHT PARENTHESIS ()) or EOF, consume it
            // and return the <url-token> (if EOF was encountered, this is a parse error);
            input = peek_code_point();

            if (is_right_paren(input)) {
                (void)next_code_point();
                return make_token();
            }

            if (is_eof(input)) {
                (void)next_code_point();
                log_parse_error();
                return make_token();
            }

            // otherwise, consume the remnants of a bad url, create a <bad-url-token>, and return it.
            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::Type::BadUrl);
        }

        // U+0022 QUOTATION MARK (")
        // U+0027 APOSTROPHE (')
        // U+0028 LEFT PARENTHESIS (()
        // non-printable code point
        if (is_quotation_mark(input) || is_apostrophe(input) || is_left_paren(input) || is_non_printable(input)) {
            // This is a parse error. Consume the remnants of a bad url, create a <bad-url-token>, and return it.
            log_parse_error();
            (void)next_code_point(); // Not to spec, see NOTE above.
            consume_the_remnants_of_a_bad_url();
            return create_new_token(Token::Type::BadUrl);
        }

        // U+005C REVERSE SOLIDUS (\)
        if (is_reverse_solidus(input)) {
            // If the stream starts with a valid escape,
            if (is_valid_escape_sequence(peek_twin())) {
                // consume an escaped code point and append the returned code point to the <url-token>’s value.
                builder.append_code_point(consume_escaped_code_point());
            } else {
                // Otherwise, this is a parse error.
                log_parse_error();
                (void)next_code_point(); // Not to spec, see NOTE above.
                // Consume the remnants of a bad url, create a <bad-url-token>, and return it.
                consume_the_remnants_of_a_bad_url();
                return create_new_token(Token::Type::BadUrl);
            }
        }

        // anything else
        // Append the current input code point to the <url-token>’s value.
        builder.append_code_point(input);
        (void)next_code_point(); // Not to spec, see NOTE above.
    }
}

// https://www.w3.org/TR/css-syntax-3/#consume-remnants-of-bad-url
void Tokenizer::consume_the_remnants_of_a_bad_url()
{
    // This section describes how to consume the remnants of a bad url from a stream of code points,
    // "cleaning up" after the tokenizer realizes that it’s in the middle of a <bad-url-token> rather
    // than a <url-token>. It returns nothing; its sole use is to consume enough of the input stream
    // to reach a recovery point where normal tokenizing can resume.

    // Repeatedly consume the next input code point from the stream:
    for (;;) {
        // NOTE: We peek instead of consuming so is_valid_escape_sequence() can peek a twin.
        //       So, we have to consume the code point later.
        auto input = peek_code_point();

        // U+0029 RIGHT PARENTHESIS ())
        // EOF
        if (is_eof(input) || is_right_paren(input)) {
            (void)next_code_point(); // Not to spec, see NOTE above.
            // Return.
            return;
        }

        // the input stream starts with a valid escape
        if (is_valid_escape_sequence(peek_twin())) {
            // Consume an escaped code point.
            // This allows an escaped right parenthesis ("\)") to be encountered without ending
            // the <bad-url-token>. This is otherwise identical to the "anything else" clause.
            (void)next_code_point(); // Not to spec, see NOTE above.
            (void)consume_escaped_code_point();
        }

        // anything else
        // Do nothing.

        (void)next_code_point(); // Not to spec, see NOTE above.
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
    // This section describes how to consume a numeric token from a stream of code points.
    // It returns either a <number-token>, <percentage-token>, or <dimension-token>.

    // Consume a number and let number be the result.
    auto number = consume_a_number();

    // If the next 3 input code points would start an identifier, then:
    if (would_start_an_identifier()) {
        // 1. Create a <dimension-token> with the same value and type flag as number,
        //    and a unit set initially to the empty string.
        auto token = create_new_token(Token::Type::Dimension);
        token.m_value = move(number.string);
        token.m_number_type = number.type;
        token.m_number_value = number.value;

        // 2. Consume a name. Set the <dimension-token>’s unit to the returned value.
        auto unit = consume_a_name();
        VERIFY(!unit.is_empty() && !unit.is_whitespace());
        token.m_unit = move(unit);

        // 3. Return the <dimension-token>.
        return token;
    }

    // Otherwise, if the next input code point is U+0025 PERCENTAGE SIGN (%), consume it.
    if (is_percent(peek_code_point())) {
        (void)next_code_point();

        // Create a <percentage-token> with the same value as number, and return it.
        auto token = create_new_token(Token::Type::Percentage);
        token.m_value = move(number.string);
        token.m_number_type = number.type;
        token.m_number_value = number.value;
        return token;
    }

    // Otherwise, create a <number-token> with the same value and type flag as number, and return it.
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
    // This section describes how to check if three code points would start a number.
    // The algorithm described here can be called explicitly with three code points,
    // or can be called with the input stream itself. In the latter case, the three
    // code points in question are the current input code point and the next two input
    // code points, in that order.
    //
    // Note: This algorithm will not consume any additional code points.

    // Look at the first code point:

    // U+002B PLUS SIGN (+)
    // U+002D HYPHEN-MINUS (-)
    if (is_plus_sign(values.first) || is_hyphen_minus(values.first)) {
        // If the second code point is a digit, return true.
        if (is_ascii_digit(values.second))
            return true;

        // Otherwise, if the second code point is a U+002E FULL STOP (.) and the third
        // code point is a digit, return true.
        if (is_full_stop(values.second) && is_ascii_digit(values.third))
            return true;

        // Otherwise, return false.
        return false;
    }

    // U+002E FULL STOP (.)
    if (is_full_stop(values.first))
        // If the second code point is a digit, return true. Otherwise, return false.
        return is_ascii_digit(values.second);

    // digit
    if (is_ascii_digit(values.first))
        // Return true.
        return true;

    // anything else
    // Return false.
    return false;
}

// https://www.w3.org/TR/css-syntax-3/#starts-with-a-valid-escape
bool Tokenizer::is_valid_escape_sequence(U32Twin values)
{
    // This section describes how to check if two code points are a valid escape.
    // The algorithm described here can be called explicitly with two code points,
    // or can be called with the input stream itself. In the latter case, the two
    // code points in question are the current input code point and the next input
    // code point, in that order.
    //
    // Note: This algorithm will not consume any additional code point.

    // If the first code point is not U+005C REVERSE SOLIDUS (\), return false.
    if (!is_reverse_solidus(values.first))
        return false;

    // Otherwise, if the second code point is a newline, return false.
    if (is_newline(values.second))
        return false;

    // Otherwise, return true.
    return true;
}

bool Tokenizer::would_start_an_identifier()
{
    return would_start_an_identifier(peek_triplet());
}

// https://www.w3.org/TR/css-syntax-3/#would-start-an-identifier
bool Tokenizer::would_start_an_identifier(U32Triplet values)
{
    // This section describes how to check if three code points would start an identifier.
    // The algorithm described here can be called explicitly with three code points, or
    // can be called with the input stream itself. In the latter case, the three code
    // points in question are the current input code point and the next two input code
    // points, in that order.
    //
    // Note: This algorithm will not consume any additional code points.

    // Look at the first code point:

    // U+002D HYPHEN-MINUS
    if (is_hyphen_minus(values.first)) {
        // If the second code point is a name-start code point or a U+002D HYPHEN-MINUS,
        // or the second and third code points are a valid escape, return true.
        if (is_name_start_code_point(values.second) || is_hyphen_minus(values.second) || is_valid_escape_sequence(values.to_twin_23()))
            return true;
        // Otherwise, return false.
        return false;
    }

    // name-start code point
    if (is_name_start_code_point(values.first)) {
        // Return true.
        return true;
    }

    // U+005C REVERSE SOLIDUS (\)
    if (is_reverse_solidus(values.first)) {
        // If the first and second code points are a valid escape, return true.
        if (is_valid_escape_sequence(values.to_twin_12()))
            return true;
        // Otherwise, return false.
        return false;
    }

    // anything else
    // Return false.
    return false;
}

// https://www.w3.org/TR/css-syntax-3/#consume-string-token
Token Tokenizer::consume_string_token(u32 ending_code_point)
{
    // This section describes how to consume a string token from a stream of code points.
    // It returns either a <string-token> or <bad-string-token>.
    //
    // This algorithm may be called with an ending code point, which denotes the code point
    // that ends the string. If an ending code point is not specified, the current input
    // code point is used.

    // Initially create a <string-token> with its value set to the empty string.
    auto token = create_new_token(Token::Type::String);
    StringBuilder builder;

    auto make_token = [&]() {
        token.m_value = builder.to_string();
        return token;
    };

    // Repeatedly consume the next input code point from the stream:
    for (;;) {
        auto input = next_code_point();

        // ending code point
        if (input == ending_code_point)
            return make_token();

        // EOF
        if (is_eof(input)) {
            // This is a parse error. Return the <string-token>.
            log_parse_error();
            return make_token();
        }

        // newline
        if (is_newline(input)) {
            // This is a parse error. Reconsume the current input code point, create a
            // <bad-string-token>, and return it.
            reconsume_current_input_code_point();
            return create_new_token(Token::Type::BadString);
        }

        // U+005C REVERSE SOLIDUS (\)
        if (is_reverse_solidus(input)) {
            // If the next input code point is EOF, do nothing.
            auto next_input = peek_code_point();
            if (is_eof(next_input))
                continue;

            // Otherwise, if the next input code point is a newline, consume it.
            if (is_newline(next_input)) {
                (void)next_code_point();
                continue;
            }

            // Otherwise, (the stream starts with a valid escape) consume an escaped code
            // point and append the returned code point to the <string-token>’s value.
            auto escaped = consume_escaped_code_point();
            builder.append_code_point(escaped);
        }

        // anything else
        // Append the current input code point to the <string-token>’s value.
        builder.append_code_point(input);
    }
}

// https://www.w3.org/TR/css-syntax-3/#consume-comment
void Tokenizer::consume_comments()
{
    // This section describes how to consume comments from a stream of code points.
    // It returns nothing.

start:
    // If the next two input code point are U+002F SOLIDUS (/) followed by a U+002A ASTERISK (*),
    // consume them and all following code points up to and including the first U+002A ASTERISK (*)
    // followed by a U+002F SOLIDUS (/), or up to an EOF code point. Return to the start of this step.
    //
    // If the preceding paragraph ended by consuming an EOF code point, this is a parse error.
    //
    // Return nothing.
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
    // This section describes how to consume a token from a stream of code points.
    // It will return a single token of any type.

    // Consume comments.
    consume_comments();

    // Consume the next input code point.
    auto input = next_code_point();

    // whitespace
    if (is_whitespace(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is whitespace");
        // Consume as much whitespace as possible. Return a <whitespace-token>.
        consume_as_much_whitespace_as_possible();
        return create_new_token(Token::Type::Whitespace);
    }

    // U+0022 QUOTATION MARK (")
    if (is_quotation_mark(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is quotation mark");
        // Consume a string token and return it.
        return consume_string_token(input);
    }

    // U+0023 NUMBER SIGN (#)
    if (is_number_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is number sign");

        // If the next input code point is a name code point or the next two input code points
        // are a valid escape, then:
        auto next_input = peek_code_point();
        auto maybe_escape = peek_twin();

        if (is_name_code_point(next_input) || is_valid_escape_sequence(maybe_escape)) {
            // 1. Create a <hash-token>.
            auto token = create_new_token(Token::Type::Hash);

            // 2. If the next 3 input code points would start an identifier, set the <hash-token>’s
            //    type flag to "id".
            if (would_start_an_identifier())
                token.m_hash_type = Token::HashType::Id;

            // 3. Consume a name, and set the <hash-token>’s value to the returned string.
            auto name = consume_a_name();
            token.m_value = move(name);

            // 4. Return the <hash-token>.
            return token;
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+0027 APOSTROPHE (')
    if (is_apostrophe(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is apostrophe");
        // Consume a string token and return it.
        return consume_string_token(input);
    }

    // U+0028 LEFT PARENTHESIS (()
    if (is_left_paren(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is left paren");
        // Return a <(-token>.
        return create_new_token(Token::Type::OpenParen);
    }

    // U+0029 RIGHT PARENTHESIS ())
    if (is_right_paren(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is right paren");
        // Return a <)-token>.
        return create_new_token(Token::Type::CloseParen);
    }

    // U+002B PLUS SIGN (+)
    if (is_plus_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is plus sign");
        // If the input stream starts with a number, reconsume the current input code point,
        // consume a numeric token and return it.
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+002C COMMA (,)
    if (is_comma(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is comma");
        // Return a <comma-token>.
        return create_new_token(Token::Type::Comma);
    }

    // U+002D HYPHEN-MINUS (-)
    if (is_hyphen_minus(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is hyphen minus");
        // If the input stream starts with a number, reconsume the current input code point,
        // consume a numeric token, and return it.
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        // Otherwise, if the next 2 input code points are U+002D HYPHEN-MINUS U+003E
        // GREATER-THAN SIGN (->), consume them and return a <CDC-token>.
        auto next_twin = peek_twin();
        if (is_hyphen_minus(next_twin.first) && is_greater_than_sign(next_twin.second)) {
            (void)next_code_point();
            (void)next_code_point();

            return create_new_token(Token::Type::CDC);
        }

        // Otherwise, if the input stream starts with an identifier, reconsume the current
        // input code point, consume an ident-like token, and return it.
        if (would_start_an_identifier()) {
            reconsume_current_input_code_point();
            return consume_an_ident_like_token();
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+002E FULL STOP (.)
    if (is_full_stop(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is full stop");
        // If the input stream starts with a number, reconsume the current input code point,
        // consume a numeric token, and return it.
        if (would_start_a_number()) {
            reconsume_current_input_code_point();
            return consume_a_numeric_token();
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+003A COLON (:)
    if (is_colon(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is colon");
        // Return a <colon-token>.
        return create_new_token(Token::Type::Colon);
    }

    // U+003B SEMICOLON (;)
    if (is_semicolon(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is semicolon");
        // Return a <semicolon-token>.
        return create_new_token(Token::Type::Semicolon);
    }

    // U+003C LESS-THAN SIGN (<)
    if (is_less_than_sign(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is less than");
        // If the next 3 input code points are U+0021 EXCLAMATION MARK U+002D HYPHEN-MINUS
        // U+002D HYPHEN-MINUS (!--), consume them and return a <CDO-token>.
        auto maybe_cdo = peek_triplet();
        if (is_exclamation_mark(maybe_cdo.first) && is_hyphen_minus(maybe_cdo.second) && is_hyphen_minus(maybe_cdo.third)) {
            (void)next_code_point();
            (void)next_code_point();
            (void)next_code_point();

            return create_new_token(Token::Type::CDO);
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+0040 COMMERCIAL AT (@)
    if (is_at(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is at");
        // If the next 3 input code points would start an identifier, consume a name, create
        // an <at-keyword-token> with its value set to the returned value, and return it.
        if (would_start_an_identifier()) {
            auto name = consume_a_name();
            return create_value_token(Token::Type::AtKeyword, name);
        }

        // Otherwise, return a <delim-token> with its value set to the current input code point.
        return create_value_token(Token::Type::Delim, input);
    }

    // U+005B LEFT SQUARE BRACKET ([)
    if (is_open_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is open square");
        // Return a <[-token>.
        return create_new_token(Token::Type::OpenSquare);
    }

    // U+005C REVERSE SOLIDUS (\)
    if (is_reverse_solidus(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is reverse solidus");
        // If the input stream starts with a valid escape, reconsume the current input code point,
        // consume an ident-like token, and return it.
        if (is_valid_escape_sequence({ input, peek_code_point() })) {
            reconsume_current_input_code_point();
            return consume_an_ident_like_token();
        }

        // Otherwise, this is a parse error. Return a <delim-token> with its value set to the
        // current input code point.
        log_parse_error();
        return create_value_token(Token::Type::Delim, input);
    }

    // U+005D RIGHT SQUARE BRACKET (])
    if (is_closed_square_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is closed square");
        // Return a <]-token>.
        return create_new_token(Token::Type::CloseSquare);
    }

    // U+007B LEFT CURLY BRACKET ({)
    if (is_open_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is open curly");
        // Return a <{-token>.
        return create_new_token(Token::Type::OpenCurly);
    }

    // U+007D RIGHT CURLY BRACKET (})
    if (is_closed_curly_bracket(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is closed curly");
        // Return a <}-token>.
        return create_new_token(Token::Type::CloseCurly);
    }

    // digit
    if (is_ascii_digit(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is digit");
        // Reconsume the current input code point, consume a numeric token, and return it.
        reconsume_current_input_code_point();
        return consume_a_numeric_token();
    }

    // name-start code point
    if (is_name_start_code_point(input)) {
        dbgln_if(CSS_TOKENIZER_DEBUG, "is name start");
        // Reconsume the current input code point, consume an ident-like token, and return it.
        reconsume_current_input_code_point();
        return consume_an_ident_like_token();
    }

    // EOF
    if (is_eof(input)) {
        // Return an <EOF-token>.
        return create_new_token(Token::Type::EndOfFile);
    }

    // anything else
    dbgln_if(CSS_TOKENIZER_DEBUG, "is delimiter");
    // Return a <delim-token> with its value set to the current input code point.
    return create_value_token(Token::Type::Delim, input);
}

}
