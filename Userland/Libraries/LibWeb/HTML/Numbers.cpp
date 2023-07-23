/*
 * Copyright (c) 2023, Jonatan Klemets <jonatan.r.klemets@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibWeb/HTML/Numbers.h>
#include <LibWeb/Infra/CharacterTypes.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#rules-for-parsing-integers
Optional<i32> parse_integer(StringView string)
{
    // 1. Let input be the string being parsed.
    // 2. Let position be a pointer into input, initially pointing at the start of the string.
    GenericLexer lexer { string };

    // 3. Let sign have the value "positive".
    // NOTE: Skipped, see comment on step 6.

    // 4. Skip ASCII whitespace within input given position.
    lexer.ignore_while(Web::Infra::is_ascii_whitespace);

    // 5. If position is past the end of input, return an error.
    if (lexer.is_eof()) {
        return {};
    }

    // 6. If the character indicated by position (the first character) is a U+002D HYPHEN-MINUS character (-):
    //
    // If we parse a signed integer, then we include the sign character (if present) in the collect step
    // (step 8) and lean on `AK::StringUtils::convert_to_int` to handle it for us.
    size_t start_index = lexer.tell();
    if (lexer.peek() == '-' || lexer.peek() == '+') {
        lexer.consume();
    }

    // 7. If the character indicated by position is not an ASCII digit, then return an error.
    if (!lexer.next_is(is_ascii_digit)) {
        return {};
    }

    // 8. Collect a sequence of code points that are ASCII digits from input given position, and interpret the resulting sequence as a base-ten integer. Let value be that integer.
    lexer.consume_while(is_ascii_digit);
    size_t end_index = lexer.tell();
    auto digits = lexer.input().substring_view(start_index, end_index - start_index);
    auto optional_value = AK::StringUtils::convert_to_int<i32>(digits);

    // 9. If sign is "positive", return value, otherwise return the result of subtracting value from zero.
    // NOTE: Skipped, see comment on step 6.

    return optional_value;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#rules-for-parsing-non-negative-integers
Optional<u32> parse_non_negative_integer(StringView string)
{
    // 1. Let input be the string being parsed.
    // 2. Let value be the result of parsing input using the rules for parsing integers.
    //
    // NOTE: Because we call `parse_integer`, we parse all integers as signed. If we need the extra
    //       size that an unsigned integer offers, then this would need to be improved. That said,
    //       I don't think we need to support such large integers at the moment.
    auto optional_value = parse_integer(string);

    // 3. If value is an error, return an error.
    if (!optional_value.has_value()) {
        return {};
    }

    // 4. If value is less than zero, return an error.
    if (optional_value.value() < 0) {
        return {};
    }

    // 5. Return value.
    return static_cast<u32>(optional_value.value());
}

}
