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

#include <AK/Memory.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>

namespace AK {

namespace StringUtils {

bool matches(const StringView& str, const StringView& mask, CaseSensitivity case_sensitivity)
{
    if (str.is_null() || mask.is_null())
        return str.is_null() && mask.is_null();

    if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
        const String str_lower = String(str).to_lowercase();
        const String mask_lower = String(mask).to_lowercase();
        return matches(str_lower, mask_lower, CaseSensitivity::CaseSensitive);
    }

    const char* string_ptr = str.characters_without_null_termination();
    const char* string_end = string_ptr + str.length();
    const char* mask_ptr = mask.characters_without_null_termination();
    const char* mask_end = mask_ptr + mask.length();

    // Match string against mask directly unless we hit a *
    while ((string_ptr < string_end) && (mask_ptr < mask_end) && (*mask_ptr != '*')) {
        if ((*mask_ptr != *string_ptr) && (*mask_ptr != '?'))
            return false;
        mask_ptr++;
        string_ptr++;
    }

    const char* cp = nullptr;
    const char* mp = nullptr;

    while (string_ptr < string_end) {
        if ((mask_ptr < mask_end) && (*mask_ptr == '*')) {
            // If we have only a * left, there is no way to not match.
            if (++mask_ptr == mask_end)
                return true;
            mp = mask_ptr;
            cp = string_ptr + 1;
        } else if ((mask_ptr < mask_end) && ((*mask_ptr == *string_ptr) || (*mask_ptr == '?'))) {
            mask_ptr++;
            string_ptr++;
        } else if ((cp != nullptr) && (mp != nullptr)) {
            mask_ptr = mp;
            string_ptr = cp++;
        } else {
            break;
        }
    }

    // Handle any trailing mask
    while ((mask_ptr < mask_end) && (*mask_ptr == '*'))
        mask_ptr++;

    // If we 'ate' all of the mask and the string then we match.
    return (mask_ptr == mask_end) && string_ptr == string_end;
}

Optional<int> convert_to_int(const StringView& str)
{
    if (str.is_empty())
        return {};

    bool negative = false;
    size_t i = 0;
    const auto characters = str.characters_without_null_termination();

    if (characters[0] == '-' || characters[0] == '+') {
        if (str.length() == 1)
            return {};
        i++;
        negative = (characters[0] == '-');
    }

    int value = 0;
    for (; i < str.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};
        value = value * 10;
        value += characters[i] - '0';
    }
    return negative ? -value : value;
}

Optional<unsigned> convert_to_uint(const StringView& str)
{
    if (str.is_empty())
        return {};

    unsigned value = 0;
    const auto characters = str.characters_without_null_termination();

    for (size_t i = 0; i < str.length(); i++) {
        if (characters[i] < '0' || characters[i] > '9')
            return {};

        value = value * 10;
        value += characters[i] - '0';
    }
    return value;
}

Optional<unsigned> convert_to_uint_from_hex(const StringView& str)
{
    if (str.is_empty())
        return {};

    unsigned value = 0;
    const auto count = str.length();

    for (size_t i = 0; i < count; i++) {
        char digit = str[i];
        u8 digit_val;

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

StringView trim_whitespace(const StringView& str, TrimMode mode)
{
    auto is_whitespace_character = [](char ch) -> bool {
        return ch == '\t'
            || ch == '\n'
            || ch == '\v'
            || ch == '\f'
            || ch == '\r'
            || ch == ' ';
    };

    size_t substring_start = 0;
    size_t substring_length = str.length();

    if (mode == TrimMode::Left || mode == TrimMode::Both) {
        for (size_t i = 0; i < str.length(); ++i) {
            if (substring_length == 0)
                return "";
            if (!is_whitespace_character(str[i]))
                break;
            ++substring_start;
            --substring_length;
        }
    }

    if (mode == TrimMode::Right || mode == TrimMode::Both) {
        for (size_t i = str.length() - 1; i > 0; --i) {
            if (substring_length == 0)
                return "";
            if (!is_whitespace_character(str[i]))
                break;
            --substring_length;
        }
    }

    return str.substring_view(substring_start, substring_length);
}
}

}
