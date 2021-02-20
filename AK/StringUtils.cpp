/*
 * Copyright (c) 2018-2020, Andreas Kling <awesomekling@gmail.com>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
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

#include <AK/MemMem.h>
#include <AK/Memory.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <ctype.h>

namespace AK {

namespace StringUtils {

bool matches(const StringView& str, const StringView& mask, CaseSensitivity case_sensitivity, Vector<MaskSpan>* match_spans)
{
    auto record_span = [&match_spans](size_t start, size_t length) {
        if (match_spans)
            match_spans->append({ start, length });
    };

    if (str.is_null() || mask.is_null())
        return str.is_null() && mask.is_null();

    if (mask == "*") {
        record_span(0, str.length());
        return true;
    }

    if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
        const String str_lower = String(str).to_lowercase();
        const String mask_lower = String(mask).to_lowercase();
        return matches(str_lower, mask_lower, CaseSensitivity::CaseSensitive, match_spans);
    }

    const char* string_ptr = str.characters_without_null_termination();
    const char* string_start = str.characters_without_null_termination();
    const char* string_end = string_ptr + str.length();
    const char* mask_ptr = mask.characters_without_null_termination();
    const char* mask_end = mask_ptr + mask.length();

    auto matches_one = [](char ch, char p) {
        if (p == '?')
            return true;
        return p == ch && ch != 0;
    };
    while (string_ptr < string_end && mask_ptr < mask_end) {
        auto string_start_ptr = string_ptr;
        switch (*mask_ptr) {
        case '*':
            if (mask_ptr[1] == 0) {
                record_span(string_ptr - string_start, string_end - string_ptr);
                return true;
            }
            while (string_ptr < string_end && !matches(string_ptr, mask_ptr + 1))
                ++string_ptr;
            record_span(string_start_ptr - string_start, string_ptr - string_start_ptr);
            --string_ptr;
            break;
        case '?':
            record_span(string_ptr - string_start, 1);
            break;
        default:
            if (!matches_one(*string_ptr, *mask_ptr))
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
Optional<T> convert_to_int(const StringView& str)
{
    auto str_trimmed = str.trim_whitespace();
    if (str_trimmed.is_empty())
        return {};

    T sign = 1;
    size_t i = 0;
    const auto characters = str_trimmed.characters_without_null_termination();

    if (characters[0] == '-' || characters[0] == '+') {
        if (str_trimmed.length() == 1)
            return {};
        i++;
        if (characters[0] == '-')
            sign = -1;
    }

    T value = 0;
    for (; i < str_trimmed.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};

        if (__builtin_mul_overflow(value, 10, &value))
            return {};

        if (__builtin_add_overflow(value, sign * (characters[i] - '0'), &value))
            return {};
    }
    return value;
}

template Optional<i8> convert_to_int(const StringView& str);
template Optional<i16> convert_to_int(const StringView& str);
template Optional<i32> convert_to_int(const StringView& str);
template Optional<i64> convert_to_int(const StringView& str);

template<typename T>
Optional<T> convert_to_uint(const StringView& str)
{
    auto str_trimmed = str.trim_whitespace();
    if (str_trimmed.is_empty())
        return {};

    T value = 0;
    const auto characters = str_trimmed.characters_without_null_termination();

    for (size_t i = 0; i < str_trimmed.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};

        if (__builtin_mul_overflow(value, 10, &value))
            return {};

        if (__builtin_add_overflow(value, characters[i] - '0', &value))
            return {};
    }
    return value;
}

template Optional<u8> convert_to_uint(const StringView& str);
template Optional<u16> convert_to_uint(const StringView& str);
template Optional<u32> convert_to_uint(const StringView& str);
template Optional<u64> convert_to_uint(const StringView& str);
template Optional<long> convert_to_uint(const StringView& str);
template Optional<long long> convert_to_uint(const StringView& str);

template<typename T>
Optional<T> convert_to_uint_from_hex(const StringView& str)
{
    auto str_trimmed = str.trim_whitespace();
    if (str_trimmed.is_empty())
        return {};

    T value = 0;
    const auto count = str_trimmed.length();
    const T upper_bound = AK::NumericLimits<T>::max();

    for (size_t i = 0; i < count; i++) {
        char digit = str_trimmed[i];
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

template Optional<u8> convert_to_uint_from_hex(const StringView& str);
template Optional<u16> convert_to_uint_from_hex(const StringView& str);
template Optional<u32> convert_to_uint_from_hex(const StringView& str);
template Optional<u64> convert_to_uint_from_hex(const StringView& str);

static inline char to_lowercase(char c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

bool equals_ignoring_case(const StringView& a, const StringView& b)
{
    if (a.impl() && a.impl() == b.impl())
        return true;
    if (a.length() != b.length())
        return false;
    for (size_t i = 0; i < a.length(); ++i) {
        if (to_lowercase(a.characters_without_null_termination()[i]) != to_lowercase(b.characters_without_null_termination()[i]))
            return false;
    }
    return true;
}

bool ends_with(const StringView& str, const StringView& end, CaseSensitivity case_sensitivity)
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
        if (to_lowercase(str_chars[si]) != to_lowercase(end_chars[ei]))
            return false;
    }
    return true;
}

bool starts_with(const StringView& str, const StringView& start, CaseSensitivity case_sensitivity)
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
        if (to_lowercase(str_chars[si]) != to_lowercase(start_chars[starti]))
            return false;
    }
    return true;
}

bool contains(const StringView& str, const StringView& needle, CaseSensitivity case_sensitivity)
{
    if (str.is_null() || needle.is_null() || str.is_empty() || needle.length() > str.length())
        return false;
    if (needle.is_empty())
        return true;
    auto str_chars = str.characters_without_null_termination();
    auto needle_chars = needle.characters_without_null_termination();
    if (case_sensitivity == CaseSensitivity::CaseSensitive)
        return memmem(str_chars, str.length(), needle_chars, needle.length()) != nullptr;

    auto needle_first = to_lowercase(needle_chars[0]);
    for (size_t si = 0; si < str.length(); si++) {
        if (to_lowercase(str_chars[si]) != needle_first)
            continue;
        for (size_t ni = 0; si + ni < str.length(); ni++) {
            if (to_lowercase(str_chars[si + ni]) != to_lowercase(needle_chars[ni])) {
                si += ni;
                break;
            }
            if (ni + 1 == needle.length())
                return true;
        }
    }
    return false;
}

bool is_whitespace(const StringView& str)
{
    for (auto ch : str) {
        if (!isspace(ch))
            return false;
    }
    return true;
}

StringView trim_whitespace(const StringView& str, TrimMode mode)
{
    size_t substring_start = 0;
    size_t substring_length = str.length();

    if (mode == TrimMode::Left || mode == TrimMode::Both) {
        for (size_t i = 0; i < str.length(); ++i) {
            if (substring_length == 0)
                return "";
            if (!isspace(str[i]))
                break;
            ++substring_start;
            --substring_length;
        }
    }

    if (mode == TrimMode::Right || mode == TrimMode::Both) {
        for (size_t i = str.length() - 1; i > 0; --i) {
            if (substring_length == 0)
                return "";
            if (!isspace(str[i]))
                break;
            --substring_length;
        }
    }

    return str.substring_view(substring_start, substring_length);
}

Optional<size_t> find(const StringView& haystack, const StringView& needle)
{
    return AK::memmem_optional(
        haystack.characters_without_null_termination(), haystack.length(),
        needle.characters_without_null_termination(), needle.length());
}

String to_snakecase(const StringView& str)
{
    auto should_insert_underscore = [&](auto i, auto current_char) {
        if (i == 0)
            return false;
        auto previous_ch = str[i - 1];
        if (islower(previous_ch) && isupper(current_char))
            return true;
        if (i >= str.length() - 1)
            return false;
        auto next_ch = str[i + 1];
        if (isupper(current_char) && islower(next_ch))
            return true;
        return false;
    };

    StringBuilder builder;
    for (size_t i = 0; i < str.length(); ++i) {
        auto ch = str[i];
        if (should_insert_underscore(i, ch))
            builder.append('_');
        builder.append(tolower(ch));
    }
    return builder.to_string();
}

}

}
