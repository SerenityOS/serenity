/*
 * Copyright (c) 2018-2020, Andreas Kling <awesomekling@gmail.com>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/MemMem.h>
#include <AK/Memory.h>
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

#ifndef KERNEL
#    include <AK/String.h>
#endif

namespace AK {

namespace StringUtils {

bool matches(StringView str, StringView mask, CaseSensitivity case_sensitivity, Vector<MaskSpan>* match_spans)
{
    auto record_span = [&match_spans](size_t start, size_t length) {
        if (match_spans)
            match_spans->append({ start, length });
    };

    if (str.is_null() || mask.is_null())
        return str.is_null() && mask.is_null();

    if (mask == "*"sv) {
        record_span(0, str.length());
        return true;
    }

    char const* string_ptr = str.characters_without_null_termination();
    char const* string_start = str.characters_without_null_termination();
    char const* string_end = string_ptr + str.length();
    char const* mask_ptr = mask.characters_without_null_termination();
    char const* mask_end = mask_ptr + mask.length();

    while (string_ptr < string_end && mask_ptr < mask_end) {
        auto string_start_ptr = string_ptr;
        switch (*mask_ptr) {
        case '*':
            if (mask_ptr == mask_end - 1) {
                record_span(string_ptr - string_start, string_end - string_ptr);
                return true;
            }
            while (string_ptr < string_end && !matches({ string_ptr, static_cast<size_t>(string_end - string_ptr) }, { mask_ptr + 1, static_cast<size_t>(mask_end - mask_ptr - 1) }, case_sensitivity))
                ++string_ptr;
            record_span(string_start_ptr - string_start, string_ptr - string_start_ptr);
            --string_ptr;
            break;
        case '?':
            record_span(string_ptr - string_start, 1);
            break;
        default:
            auto p = *mask_ptr;
            auto ch = *string_ptr;
            if (case_sensitivity == CaseSensitivity::CaseSensitive ? p != ch : to_ascii_lowercase(p) != to_ascii_lowercase(ch))
                return false;
            break;
        }
        ++string_ptr;
        ++mask_ptr;
    }

    if (string_ptr == string_end) {
        // Allow ending '*' to contain nothing.
        while (mask_ptr != mask_end && *mask_ptr == '*') {
            record_span(string_ptr - string_start, 0);
            ++mask_ptr;
        }
    }

    return string_ptr == string_end && mask_ptr == mask_end;
}

template<typename T>
Optional<T> convert_to_int(StringView str, TrimWhitespace trim_whitespace)
{
    auto string = trim_whitespace == TrimWhitespace::Yes
        ? str.trim_whitespace()
        : str;
    if (string.is_empty())
        return {};

    T sign = 1;
    size_t i = 0;
    auto const characters = string.characters_without_null_termination();

    if (characters[0] == '-' || characters[0] == '+') {
        if (string.length() == 1)
            return {};
        i++;
        if (characters[0] == '-')
            sign = -1;
    }

    T value = 0;
    for (; i < string.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};

        if (__builtin_mul_overflow(value, 10, &value))
            return {};

        if (__builtin_add_overflow(value, sign * (characters[i] - '0'), &value))
            return {};
    }
    return value;
}

template Optional<i8> convert_to_int(StringView str, TrimWhitespace);
template Optional<i16> convert_to_int(StringView str, TrimWhitespace);
template Optional<i32> convert_to_int(StringView str, TrimWhitespace);
template Optional<long> convert_to_int(StringView str, TrimWhitespace);
template Optional<long long> convert_to_int(StringView str, TrimWhitespace);

template<typename T>
Optional<T> convert_to_uint(StringView str, TrimWhitespace trim_whitespace)
{
    auto string = trim_whitespace == TrimWhitespace::Yes
        ? str.trim_whitespace()
        : str;
    if (string.is_empty())
        return {};

    T value = 0;
    auto const characters = string.characters_without_null_termination();

    for (size_t i = 0; i < string.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};

        if (__builtin_mul_overflow(value, 10, &value))
            return {};

        if (__builtin_add_overflow(value, characters[i] - '0', &value))
            return {};
    }
    return value;
}

template Optional<u8> convert_to_uint(StringView str, TrimWhitespace);
template Optional<u16> convert_to_uint(StringView str, TrimWhitespace);
template Optional<u32> convert_to_uint(StringView str, TrimWhitespace);
template Optional<unsigned long> convert_to_uint(StringView str, TrimWhitespace);
template Optional<unsigned long long> convert_to_uint(StringView str, TrimWhitespace);
template Optional<long> convert_to_uint(StringView str, TrimWhitespace);
template Optional<long long> convert_to_uint(StringView str, TrimWhitespace);

template<typename T>
Optional<T> convert_to_uint_from_hex(StringView str, TrimWhitespace trim_whitespace)
{
    auto string = trim_whitespace == TrimWhitespace::Yes
        ? str.trim_whitespace()
        : str;
    if (string.is_empty())
        return {};

    T value = 0;
    auto const count = string.length();
    const T upper_bound = NumericLimits<T>::max();

    for (size_t i = 0; i < count; i++) {
        char digit = string[i];
        u8 digit_val;
        if (value > (upper_bound >> 4))
            return {};

        if (digit >= '0' && digit <= '9') {
            digit_val = digit - '0';
        } else if (digit >= 'a' && digit <= 'f') {
            digit_val = 10 + (digit - 'a');
        } else if (digit >= 'A' && digit <= 'F') {
            digit_val = 10 + (digit - 'A');
        } else {
            return {};
        }

        value = (value << 4) + digit_val;
    }
    return value;
}

template Optional<u8> convert_to_uint_from_hex(StringView str, TrimWhitespace);
template Optional<u16> convert_to_uint_from_hex(StringView str, TrimWhitespace);
template Optional<u32> convert_to_uint_from_hex(StringView str, TrimWhitespace);
template Optional<u64> convert_to_uint_from_hex(StringView str, TrimWhitespace);

template<typename T>
Optional<T> convert_to_uint_from_octal(StringView str, TrimWhitespace trim_whitespace)
{
    auto string = trim_whitespace == TrimWhitespace::Yes
        ? str.trim_whitespace()
        : str;
    if (string.is_empty())
        return {};

    T value = 0;
    auto const count = string.length();
    const T upper_bound = NumericLimits<T>::max();

    for (size_t i = 0; i < count; i++) {
        char digit = string[i];
        u8 digit_val;
        if (value > (upper_bound >> 3))
            return {};

        if (digit >= '0' && digit <= '7') {
            digit_val = digit - '0';
        } else {
            return {};
        }

        value = (value << 3) + digit_val;
    }
    return value;
}

template Optional<u8> convert_to_uint_from_octal(StringView str, TrimWhitespace);
template Optional<u16> convert_to_uint_from_octal(StringView str, TrimWhitespace);
template Optional<u32> convert_to_uint_from_octal(StringView str, TrimWhitespace);
template Optional<u64> convert_to_uint_from_octal(StringView str, TrimWhitespace);

bool equals_ignoring_case(StringView a, StringView b)
{
    if (a.length() != b.length())
        return false;
    for (size_t i = 0; i < a.length(); ++i) {
        if (to_ascii_lowercase(a.characters_without_null_termination()[i]) != to_ascii_lowercase(b.characters_without_null_termination()[i]))
            return false;
    }
    return true;
}

bool ends_with(StringView str, StringView end, CaseSensitivity case_sensitivity)
{
    if (end.is_empty())
        return true;
    if (str.is_empty())
        return false;
    if (end.length() > str.length())
        return false;

    if (case_sensitivity == CaseSensitivity::CaseSensitive)
        return !memcmp(str.characters_without_null_termination() + (str.length() - end.length()), end.characters_without_null_termination(), end.length());

    auto str_chars = str.characters_without_null_termination();
    auto end_chars = end.characters_without_null_termination();

    size_t si = str.length() - end.length();
    for (size_t ei = 0; ei < end.length(); ++si, ++ei) {
        if (to_ascii_lowercase(str_chars[si]) != to_ascii_lowercase(end_chars[ei]))
            return false;
    }
    return true;
}

bool starts_with(StringView str, StringView start, CaseSensitivity case_sensitivity)
{
    if (start.is_empty())
        return true;
    if (str.is_empty())
        return false;
    if (start.length() > str.length())
        return false;
    if (str.characters_without_null_termination() == start.characters_without_null_termination())
        return true;

    if (case_sensitivity == CaseSensitivity::CaseSensitive)
        return !memcmp(str.characters_without_null_termination(), start.characters_without_null_termination(), start.length());

    auto str_chars = str.characters_without_null_termination();
    auto start_chars = start.characters_without_null_termination();

    size_t si = 0;
    for (size_t starti = 0; starti < start.length(); ++si, ++starti) {
        if (to_ascii_lowercase(str_chars[si]) != to_ascii_lowercase(start_chars[starti]))
            return false;
    }
    return true;
}

bool contains(StringView str, StringView needle, CaseSensitivity case_sensitivity)
{
    if (str.is_null() || needle.is_null() || str.is_empty() || needle.length() > str.length())
        return false;
    if (needle.is_empty())
        return true;
    auto str_chars = str.characters_without_null_termination();
    auto needle_chars = needle.characters_without_null_termination();
    if (case_sensitivity == CaseSensitivity::CaseSensitive)
        return memmem(str_chars, str.length(), needle_chars, needle.length()) != nullptr;

    auto needle_first = to_ascii_lowercase(needle_chars[0]);
    for (size_t si = 0; si < str.length(); si++) {
        if (to_ascii_lowercase(str_chars[si]) != needle_first)
            continue;
        for (size_t ni = 0; si + ni < str.length(); ni++) {
            if (to_ascii_lowercase(str_chars[si + ni]) != to_ascii_lowercase(needle_chars[ni])) {
                if (ni > 0)
                    si += ni - 1;
                break;
            }
            if (ni + 1 == needle.length())
                return true;
        }
    }
    return false;
}

bool is_whitespace(StringView str)
{
    return all_of(str, is_ascii_space);
}

StringView trim(StringView str, StringView characters, TrimMode mode)
{
    size_t substring_start = 0;
    size_t substring_length = str.length();

    if (mode == TrimMode::Left || mode == TrimMode::Both) {
        for (size_t i = 0; i < str.length(); ++i) {
            if (substring_length == 0)
                return "";
            if (!characters.contains(str[i]))
                break;
            ++substring_start;
            --substring_length;
        }
    }

    if (mode == TrimMode::Right || mode == TrimMode::Both) {
        for (size_t i = str.length() - 1; i > 0; --i) {
            if (substring_length == 0)
                return "";
            if (!characters.contains(str[i]))
                break;
            --substring_length;
        }
    }

    return str.substring_view(substring_start, substring_length);
}

StringView trim_whitespace(StringView str, TrimMode mode)
{
    return trim(str, " \n\t\v\f\r", mode);
}

Optional<size_t> find(StringView haystack, char needle, size_t start)
{
    if (start >= haystack.length())
        return {};
    for (size_t i = start; i < haystack.length(); ++i) {
        if (haystack[i] == needle)
            return i;
    }
    return {};
}

Optional<size_t> find(StringView haystack, StringView needle, size_t start)
{
    if (start > haystack.length())
        return {};
    auto index = AK::memmem_optional(
        haystack.characters_without_null_termination() + start, haystack.length() - start,
        needle.characters_without_null_termination(), needle.length());
    return index.has_value() ? (*index + start) : index;
}

Optional<size_t> find_last(StringView haystack, char needle)
{
    for (size_t i = haystack.length(); i > 0; --i) {
        if (haystack[i - 1] == needle)
            return i - 1;
    }
    return {};
}

Vector<size_t> find_all(StringView haystack, StringView needle)
{
    Vector<size_t> positions;
    size_t current_position = 0;
    while (current_position <= haystack.length()) {
        auto maybe_position = AK::memmem_optional(
            haystack.characters_without_null_termination() + current_position, haystack.length() - current_position,
            needle.characters_without_null_termination(), needle.length());
        if (!maybe_position.has_value())
            break;
        positions.append(current_position + *maybe_position);
        current_position += *maybe_position + 1;
    }
    return positions;
}

Optional<size_t> find_any_of(StringView haystack, StringView needles, SearchDirection direction)
{
    if (haystack.is_empty() || needles.is_empty())
        return {};
    if (direction == SearchDirection::Forward) {
        for (size_t i = 0; i < haystack.length(); ++i) {
            if (needles.contains(haystack[i]))
                return i;
        }
    } else if (direction == SearchDirection::Backward) {
        for (size_t i = haystack.length(); i > 0; --i) {
            if (needles.contains(haystack[i - 1]))
                return i - 1;
        }
    }
    return {};
}

#ifndef KERNEL
String to_snakecase(StringView str)
{
    auto should_insert_underscore = [&](auto i, auto current_char) {
        if (i == 0)
            return false;
        auto previous_ch = str[i - 1];
        if (is_ascii_lower_alpha(previous_ch) && is_ascii_upper_alpha(current_char))
            return true;
        if (i >= str.length() - 1)
            return false;
        auto next_ch = str[i + 1];
        if (is_ascii_upper_alpha(current_char) && is_ascii_lower_alpha(next_ch))
            return true;
        return false;
    };

    StringBuilder builder;
    for (size_t i = 0; i < str.length(); ++i) {
        auto ch = str[i];
        if (should_insert_underscore(i, ch))
            builder.append('_');
        builder.append_as_lowercase(ch);
    }
    return builder.to_string();
}

String to_titlecase(StringView str)
{
    StringBuilder builder;
    bool next_is_upper = true;

    for (auto ch : str) {
        if (next_is_upper)
            builder.append_code_point(to_ascii_uppercase(ch));
        else
            builder.append_code_point(to_ascii_lowercase(ch));
        next_is_upper = ch == ' ';
    }

    return builder.to_string();
}

String replace(StringView str, StringView needle, StringView replacement, bool all_occurrences)
{
    if (str.is_empty())
        return str;

    Vector<size_t> positions;
    if (all_occurrences) {
        positions = str.find_all(needle);
        if (!positions.size())
            return str;
    } else {
        auto pos = str.find(needle);
        if (!pos.has_value())
            return str;
        positions.append(pos.value());
    }

    StringBuilder replaced_string;
    size_t last_position = 0;
    for (auto& position : positions) {
        replaced_string.append(str.substring_view(last_position, position - last_position));
        replaced_string.append(replacement);
        last_position = position + needle.length();
    }
    replaced_string.append(str.substring_view(last_position, str.length() - last_position));
    return replaced_string.build();
}
#endif

// TODO: Benchmark against KMP (AK/MemMem.h) and switch over if it's faster for short strings too
size_t count(StringView str, StringView needle)
{
    if (needle.is_empty())
        return str.length();

    size_t count = 0;
    for (size_t i = 0; i < str.length() - needle.length() + 1; ++i) {
        if (str.substring_view(i).starts_with(needle))
            count++;
    }
    return count;
}

}

}
