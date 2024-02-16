/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>

#ifndef KERNEL
#    include <AK/ByteString.h>
#    include <AK/Utf16View.h>
#endif

namespace AK {
// Consume a number of characters
StringView GenericLexer::consume(size_t count)
{
    if (count == 0)
        return {};

    size_t start = m_index;
    size_t length = min(count, m_input.length() - m_index);
    m_index += length;

    return m_input.substring_view(start, length);
}

// Consume the rest of the input
StringView GenericLexer::consume_all()
{
    if (is_eof())
        return {};

    auto rest = m_input.substring_view(m_index, m_input.length() - m_index);
    m_index = m_input.length();
    return rest;
}

// Consume until a new line is found
StringView GenericLexer::consume_line()
{
    size_t start = m_index;
    while (!is_eof() && peek() != '\r' && peek() != '\n')
        m_index++;
    size_t length = m_index - start;

    consume_specific('\r');
    consume_specific('\n');

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until `stop` is peek'd
StringView GenericLexer::consume_until(char stop)
{
    size_t start = m_index;
    while (!is_eof() && peek() != stop)
        m_index++;
    size_t length = m_index - start;

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until the string `stop` is found
StringView GenericLexer::consume_until(char const* stop)
{
    size_t start = m_index;
    while (!is_eof() && !next_is(stop))
        m_index++;
    size_t length = m_index - start;

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until the string `stop` is found
StringView GenericLexer::consume_until(StringView stop)
{
    size_t start = m_index;
    while (!is_eof() && !next_is(stop))
        m_index++;
    size_t length = m_index - start;

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

/*
 * Consume a string surrounded by single or double quotes. The returned
 * StringView does not include the quotes. An escape character can be provided
 * to capture the enclosing quotes. Please note that the escape character will
 * still be in the resulting StringView
 */
StringView GenericLexer::consume_quoted_string(char escape_char)
{
    if (!next_is(is_quote))
        return {};

    char quote_char = consume();
    size_t start = m_index;
    while (!is_eof()) {
        if (next_is(escape_char))
            m_index++;
        else if (next_is(quote_char))
            break;
        m_index++;
    }
    size_t length = m_index - start;

    if (peek() != quote_char) {
        // Restore the index in case the string is unterminated
        m_index = start - 1;
        return {};
    }

    // Ignore closing quote
    ignore();

    return m_input.substring_view(start, length);
}

template<Integral T>
ErrorOr<T> GenericLexer::consume_decimal_integer()
{
    using UnsignedT = MakeUnsigned<T>;

    ArmedScopeGuard rollback { [&, rollback_position = m_index] {
        m_index = rollback_position;
    } };

    bool has_minus_sign = false;

    if (next_is('+') || next_is('-'))
        if (consume() == '-')
            has_minus_sign = true;

    StringView number_view = consume_while(is_ascii_digit);
    if (number_view.is_empty())
        return Error::from_errno(EINVAL);

    auto maybe_number = StringUtils::convert_to_uint<UnsignedT>(number_view, TrimWhitespace::No);
    if (!maybe_number.has_value())
        return Error::from_errno(ERANGE);
    auto number = maybe_number.value();

    if (!has_minus_sign) {
        if (NumericLimits<T>::max() < number) // This is only possible in a signed case.
            return Error::from_errno(ERANGE);

        rollback.disarm();
        return number;
    } else {
        if constexpr (IsUnsigned<T>) {
            if (number == 0) {
                rollback.disarm();
                return 0;
            }
            return Error::from_errno(ERANGE);
        } else {
            static constexpr UnsignedT max_value = static_cast<UnsignedT>(NumericLimits<T>::max()) + 1;
            if (number > max_value)
                return Error::from_errno(ERANGE);
            rollback.disarm();
            return -number;
        }
    }
}

LineTrackingLexer::Position LineTrackingLexer::position_for(size_t index) const
{
    auto& [cached_index, cached_line, cached_column] = m_cached_position;

    if (cached_index <= index) {
        for (size_t i = cached_index; i < index; ++i) {
            if (m_input[i] == '\n')
                ++cached_line, cached_column = 0;
            else
                ++cached_column;
        }
    } else {
        auto lines_backtracked = m_input.substring_view(index, cached_index - index).count('\n');
        cached_line -= lines_backtracked;
        if (lines_backtracked == 0) {
            cached_column -= cached_index - index;
        } else {
            auto current_line_start = m_input.substring_view(0, index).find_last('\n').value_or(0);
            cached_column = index - current_line_start;
        }
    }
    cached_index = index;
    return m_cached_position;
}

template ErrorOr<u8> GenericLexer::consume_decimal_integer<u8>();
template ErrorOr<i8> GenericLexer::consume_decimal_integer<i8>();
template ErrorOr<u16> GenericLexer::consume_decimal_integer<u16>();
template ErrorOr<i16> GenericLexer::consume_decimal_integer<i16>();
template ErrorOr<u32> GenericLexer::consume_decimal_integer<u32>();
template ErrorOr<i32> GenericLexer::consume_decimal_integer<i32>();
template ErrorOr<u64> GenericLexer::consume_decimal_integer<u64>();
template ErrorOr<i64> GenericLexer::consume_decimal_integer<i64>();

#ifndef KERNEL
Optional<ByteString> GenericLexer::consume_and_unescape_string(char escape_char)
{
    auto view = consume_quoted_string(escape_char);
    if (view.is_null())
        return {};

    StringBuilder builder;
    for (size_t i = 0; i < view.length(); ++i)
        builder.append(consume_escaped_character(escape_char));
    return builder.to_byte_string();
}

auto GenericLexer::consume_escaped_code_point(bool combine_surrogate_pairs) -> Result<u32, UnicodeEscapeError>
{
    if (!consume_specific("\\u"sv))
        return UnicodeEscapeError::MalformedUnicodeEscape;

    if (next_is('{'))
        return decode_code_point();
    return decode_single_or_paired_surrogate(combine_surrogate_pairs);
}

auto GenericLexer::decode_code_point() -> Result<u32, UnicodeEscapeError>
{
    bool starts_with_open_bracket = consume_specific('{');
    VERIFY(starts_with_open_bracket);

    u32 code_point = 0;

    while (true) {
        if (!next_is(is_ascii_hex_digit))
            return UnicodeEscapeError::MalformedUnicodeEscape;

        auto new_code_point = (code_point << 4u) | parse_ascii_hex_digit(consume());
        if (new_code_point < code_point)
            return UnicodeEscapeError::UnicodeEscapeOverflow;

        code_point = new_code_point;
        if (consume_specific('}'))
            break;
    }

    if (is_unicode(code_point))
        return code_point;
    return UnicodeEscapeError::UnicodeEscapeOverflow;
}

auto GenericLexer::decode_single_or_paired_surrogate(bool combine_surrogate_pairs) -> Result<u32, UnicodeEscapeError>
{
    constexpr size_t surrogate_length = 4;

    auto decode_one_surrogate = [&]() -> Optional<u16> {
        u16 surrogate = 0;

        for (size_t i = 0; i < surrogate_length; ++i) {
            if (!next_is(is_ascii_hex_digit))
                return {};

            surrogate = (surrogate << 4u) | parse_ascii_hex_digit(consume());
        }

        return surrogate;
    };

    auto high_surrogate = decode_one_surrogate();
    if (!high_surrogate.has_value())
        return UnicodeEscapeError::MalformedUnicodeEscape;
    if (!Utf16View::is_high_surrogate(*high_surrogate))
        return *high_surrogate;
    if (!combine_surrogate_pairs || !consume_specific("\\u"sv))
        return *high_surrogate;

    auto low_surrogate = decode_one_surrogate();
    if (!low_surrogate.has_value())
        return UnicodeEscapeError::MalformedUnicodeEscape;
    if (Utf16View::is_low_surrogate(*low_surrogate))
        return Utf16View::decode_surrogate_pair(*high_surrogate, *low_surrogate);

    retreat(6);
    return *high_surrogate;
}
#endif

}
