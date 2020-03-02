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

bool StringView::starts_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    if (characters_without_null_termination() == str.characters_without_null_termination())
        return true;
    return !memcmp(characters_without_null_termination(), str.characters_without_null_termination(), str.length());
}

bool StringView::ends_with(char ch) const
{
    if (is_empty())
        return false;
    return ch == characters_without_null_termination()[length() - 1];
}

bool StringView::ends_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    return !memcmp(characters_without_null_termination() + length() - str.length(), str.characters_without_null_termination(), str.length());
}

bool StringView::matches(const StringView& mask, CaseSensitivity case_sensitivity) const
{
    return StringUtils::matches(*this, mask, case_sensitivity);
}

StringView StringView::substring_view(size_t start, size_t length) const
{
    ASSERT(start + length <= m_length);
    return { m_characters + start, length };
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

int StringView::to_int(bool& ok) const
{
    return StringUtils::convert_to_int(*this, ok);
}

unsigned StringView::to_uint(bool& ok) const
{
    return StringUtils::convert_to_uint(*this, ok);
}

unsigned StringView::hash() const
{
    if (is_empty())
        return 0;
    if (m_impl)
        return m_impl->hash();
    return string_hash(characters_without_null_termination(), length());
}

}
