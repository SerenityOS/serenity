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

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <stdarg.h>

#ifndef KERNEL
#include <inttypes.h>
#endif

#ifdef KERNEL
extern "C" char* strstr(const char* haystack, const char* needle);
#endif

static inline char to_lowercase(char c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

namespace AK {

bool String::operator==(const String& other) const
{
    if (!m_impl)
        return !other.m_impl;

    if (!other.m_impl)
        return false;

    if (length() != other.length())
        return false;

    return !memcmp(characters(), other.characters(), length());
}

bool String::operator==(const StringView& other) const
{
    if (!m_impl)
        return !other.m_characters;

    if (!other.m_characters)
        return false;

    if (length() != other.length())
        return false;

    return !memcmp(characters(), other.characters_without_null_termination(), length());
}

bool String::operator<(const String& other) const
{
    if (!m_impl)
        return other.m_impl;

    if (!other.m_impl)
        return false;

    return strcmp(characters(), other.characters()) < 0;
}

bool String::operator>(const String& other) const
{
    if (!m_impl)
        return other.m_impl;

    if (!other.m_impl)
        return false;

    return strcmp(characters(), other.characters()) > 0;
}

String String::empty()
{
    return StringImpl::the_empty_stringimpl();
}

String String::isolated_copy() const
{
    if (!m_impl)
        return {};
    if (!m_impl->length())
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(length(), buffer);
    memcpy(buffer, m_impl->characters(), m_impl->length());
    return String(move(*impl));
}

String String::substring(size_t start, size_t length) const
{
    if (!length)
        return {};
    ASSERT(m_impl);
    ASSERT(start + length <= m_impl->length());
    // FIXME: This needs some input bounds checking.
    return { characters() + start, length };
}

StringView String::substring_view(size_t start, size_t length) const
{
    ASSERT(m_impl);
    ASSERT(start + length <= m_impl->length());
    // FIXME: This needs some input bounds checking.
    return { characters() + start, length };
}

Vector<String> String::split(char separator, bool keep_empty) const
{
    return split_limit(separator, 0, keep_empty);
}

Vector<String> String::split_limit(char separator, size_t limit, bool keep_empty) const
{
    if (is_empty())
        return {};

    Vector<String> v;
    size_t substart = 0;
    for (size_t i = 0; i < length() && ((size_t)v.size() + 1) != limit; ++i) {
        char ch = characters()[i];
        if (ch == separator) {
            size_t sublen = i - substart;
            if (sublen != 0 || keep_empty)
                v.append(substring(substart, sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0 || keep_empty)
        v.append(substring(substart, taillen));
    return v;
}

Vector<StringView> String::split_view(const char separator, bool keep_empty) const
{
    if (is_empty())
        return {};

    Vector<StringView> v;
    size_t substart = 0;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters()[i];
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

ByteBuffer String::to_byte_buffer() const
{
    if (!m_impl)
        return nullptr;
    return ByteBuffer::copy(reinterpret_cast<const u8*>(characters()), length());
}

int String::to_int(bool& ok) const
{
    bool negative = false;
    int value = 0;
    size_t i = 0;

    if (is_empty()) {
        ok = false;
        return 0;
    }

    if (characters()[0] == '-') {
        i++;
        negative = true;
    }
    for (; i < length(); i++) {
        if (characters()[i] < '0' || characters()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters()[i] - '0';
    }
    ok = true;

    return negative ? -value : value;
}

unsigned String::to_uint(bool& ok) const
{
    unsigned value = 0;
    for (size_t i = 0; i < length(); ++i) {
        if (characters()[i] < '0' || characters()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters()[i] - '0';
    }
    ok = true;
    return value;
}

String String::number(unsigned long long value)
{
    int size;
    char buffer[32];
    size = sprintf(buffer, "%llu", value);
    return String(buffer, size);
}

String String::number(unsigned long value)
{
    int size;
    char buffer[32];
    size = sprintf(buffer, "%lu", value);
    return String(buffer, size);
}

String String::number(unsigned value)
{
    char buffer[32];
    int size = sprintf(buffer, "%u", value);
    return String(buffer, size);
}

String String::number(long long value)
{
    char buffer[32];
    int size = sprintf(buffer, "%lld", value);
    return String(buffer, size);
}

String String::number(long value)
{
    char buffer[32];
    int size = sprintf(buffer, "%ld", value);
    return String(buffer, size);
}

String String::number(int value)
{
    char buffer[32];
    int size = sprintf(buffer, "%d", value);
    return String(buffer, size);
}

String String::format(const char* fmt, ...)
{
    StringBuilder builder;
    va_list ap;
    va_start(ap, fmt);
    builder.appendvf(fmt, ap);
    va_end(ap);
    return builder.to_string();
}

bool String::starts_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    return !memcmp(characters(), str.characters_without_null_termination(), str.length());
}

bool String::ends_with(const StringView& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    return !memcmp(characters() + (length() - str.length()), str.characters_without_null_termination(), str.length());
}

String String::repeated(char ch, size_t count)
{
    if (!count)
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(count, buffer);
    memset(buffer, ch, count);
    return *impl;
}

bool String::matches(const StringView& mask, CaseSensitivity case_sensitivity) const
{
    if (case_sensitivity == CaseSensitivity::CaseInsensitive) {
        String this_lower = this->to_lowercase();
        String mask_lower = String(mask).to_lowercase();
        return this_lower.match_helper(mask_lower);
    }

    return match_helper(mask);
}

bool String::match_helper(const StringView& mask) const
{
    if (is_null())
        return false;

    const char* string_ptr = characters();
    const char* mask_ptr = mask.characters_without_null_termination();
    const char* mask_end = mask_ptr + mask.length();

    // Match string against mask directly unless we hit a *
    while ((*string_ptr) && (mask_ptr < mask_end) && (*mask_ptr != '*')) {
        if ((*mask_ptr != *string_ptr) && (*mask_ptr != '?'))
            return false;
        mask_ptr++;
        string_ptr++;
    }

    const char* cp = nullptr;
    const char* mp = nullptr;

    while (*string_ptr) {
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
    return (mask_ptr == mask_end) && !*string_ptr;
}

bool String::contains(const String& needle) const
{
    return strstr(characters(), needle.characters());
}

bool String::equals_ignoring_case(const StringView& other) const
{
    if (other.m_impl == impl())
        return true;
    if (length() != other.length())
        return false;
    for (size_t i = 0; i < length(); ++i) {
        if (::to_lowercase(characters()[i]) != ::to_lowercase(other.characters_without_null_termination()[i]))
            return false;
    }
    return true;
}

}

