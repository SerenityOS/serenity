/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/FlyString.h>
#include <AK/Memory.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace AK {

StringView::StringView(const String& string)
    : m_impl(string.impl())
    , m_characters(string.characters())
    , m_length(string.length())
{
}

StringView::StringView(const FlyString& string)
    : m_impl(string.impl())
    , m_characters(string.characters())
    , m_length(string.length())
{
}

StringView::StringView(const ByteBuffer& buffer)
    : m_characters((const char*)buffer.data())
    , m_length(buffer.size())
{
}

Vector<StringView> StringView::split_view(const char separator, bool keep_empty) const
{
    if (is_empty())
        return {};

    Vector<StringView> v;
    size_t substart = 0;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters_without_null_termination()[i];
        if (ch == separator) {
            size_t sublen = i - substart;
            if (sublen != 0 || keep_empty)
                v.append(substring_view(substart, sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0 || keep_empty)
        v.append(substring_view(substart, taillen));
    return v;
}

Vector<StringView> StringView::split_view(const StringView& separator, bool keep_empty) const
{
    ASSERT(!separator.is_empty());

    if (is_empty())
        return {};

    StringView view { *this };

    Vector<StringView> parts;

    auto maybe_separator_index = find_first_of(separator);
    while (maybe_separator_index.has_value()) {
        auto separator_index = maybe_separator_index.value();
        auto part_with_separator = view.substring_view(0, separator_index + separator.length());
        if (keep_empty || separator_index > 0)
            parts.append(part_with_separator.substring_view(0, separator_index));
        view = view.substring_view_starting_after_substring(part_with_separator);
        maybe_separator_index = view.find_first_of(separator);
    }
    if (keep_empty || !view.is_empty())
        parts.append(view);

    return parts;
}

Vector<StringView> StringView::lines(bool consider_cr) const
{
    if (is_empty())
        return {};

    if (!consider_cr)
        return split_view('\n', true);

    Vector<StringView> v;
    size_t substart = 0;
    bool last_ch_was_cr = false;
    bool split_view = false;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters_without_null_termination()[i];
        if (ch == '\n') {
            split_view = true;
            if (last_ch_was_cr) {
                substart = i + 1;
                split_view = false;
                last_ch_was_cr = false;
            }
        }
        if (ch == '\r') {
            split_view = true;
            last_ch_was_cr = true;
        }
        if (split_view) {
            size_t sublen = i - substart;
            v.append(substring_view(substart, sublen));
            substart = i + 1;
        }
        split_view = false;
    }
    size_t taillen = length() - substart;
    if (taillen != 0)
        v.append(substring_view(substart, taillen));
    return v;
}

bool StringView::starts_with(char ch) const
{
    if (is_empty())
        return false;
    return ch == characters_without_null_termination()[0];
}

bool StringView::starts_with(const StringView& str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::starts_with(*this, str, case_sensitivity);
}

bool StringView::ends_with(char ch) const
{
    if (is_empty())
        return false;
    return ch == characters_without_null_termination()[length() - 1];
}

bool StringView::ends_with(const StringView& str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::ends_with(*this, str, case_sensitivity);
}

bool StringView::matches(const StringView& mask, CaseSensitivity case_sensitivity) const
{
    return StringUtils::matches(*this, mask, case_sensitivity);
}

bool StringView::contains(char needle) const
{
    for (char current : *this) {
        if (current == needle)
            return true;
    }
    return false;
}

bool StringView::contains(const StringView& needle) const
{
    return memmem(m_characters, m_length, needle.m_characters, needle.m_length) != nullptr;
}

bool StringView::equals_ignoring_case(const StringView& other) const
{
    return StringUtils::equals_ignoring_case(*this, other);
}

StringView StringView::substring_view(size_t start, size_t length) const
{
    ASSERT(start + length <= m_length);
    return { m_characters + start, length };
}
StringView StringView::substring_view(size_t start) const
{
    ASSERT(start <= m_length);
    return { m_characters + start, length() - start };
}

StringView StringView::substring_view_starting_from_substring(const StringView& substring) const
{
    const char* remaining_characters = substring.characters_without_null_termination();
    ASSERT(remaining_characters >= m_characters);
    ASSERT(remaining_characters <= m_characters + m_length);
    size_t remaining_length = m_length - (remaining_characters - m_characters);
    return { remaining_characters, remaining_length };
}

StringView StringView::substring_view_starting_after_substring(const StringView& substring) const
{
    const char* remaining_characters = substring.characters_without_null_termination() + substring.length();
    ASSERT(remaining_characters >= m_characters);
    ASSERT(remaining_characters <= m_characters + m_length);
    size_t remaining_length = m_length - (remaining_characters - m_characters);
    return { remaining_characters, remaining_length };
}

Optional<int> StringView::to_int() const
{
    return StringUtils::convert_to_int(*this);
}

Optional<unsigned> StringView::to_uint() const
{
    return StringUtils::convert_to_uint(*this);
}

unsigned StringView::hash() const
{
    if (is_empty())
        return 0;
    if (m_impl)
        return m_impl->hash();
    return string_hash(characters_without_null_termination(), length());
}

bool StringView::operator==(const String& string) const
{
    if (string.is_null())
        return !m_characters;
    if (!m_characters)
        return false;
    if (m_length != string.length())
        return false;
    if (m_characters == string.characters())
        return true;
    return !__builtin_memcmp(m_characters, string.characters(), m_length);
}

Optional<size_t> StringView::find_first_of(char c) const
{
    for (size_t pos = 0; pos < m_length; ++pos) {
        if (m_characters[pos] == c)
            return pos;
    }
    return {};
}

Optional<size_t> StringView::find_first_of(const StringView& view) const
{
    for (size_t pos = 0; pos < m_length; ++pos) {
        char c = m_characters[pos];
        for (char view_char : view) {
            if (c == view_char)
                return pos;
        }
    }
    return {};
}

Optional<size_t> StringView::find_last_of(char c) const
{
    for (size_t pos = m_length; --pos > 0;) {
        if (m_characters[pos] == c)
            return pos;
    }
    return {};
}

Optional<size_t> StringView::find_last_of(const StringView& view) const
{
    for (size_t pos = m_length - 1; --pos > 0;) {
        char c = m_characters[pos];
        for (char view_char : view) {
            if (c == view_char)
                return pos;
        }
    }
    return {};
}

String StringView::to_string() const { return String { *this }; }

}
