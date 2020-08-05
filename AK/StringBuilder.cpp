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

#include <AK/Memory.h>
#include <AK/PrintfImplementation.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Utf32View.h>

namespace AK {

inline void StringBuilder::will_append(size_t size)
{
    if ((m_length + size) > m_buffer.size())
        m_buffer.grow(max(static_cast<size_t>(16), m_buffer.size() * 2 + size));
}

StringBuilder::StringBuilder(size_t initial_capacity)
{
    m_buffer.grow((int)initial_capacity);
}

void StringBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    will_append(str.length());
    memcpy(m_buffer.data() + m_length, str.characters_without_null_termination(), str.length());
    m_length += str.length();
}

void StringBuilder::append(const char* characters, size_t length)
{
    append(StringView { characters, length });
}

void StringBuilder::append(char ch)
{
    will_append(1);
    m_buffer.data()[m_length] = ch;
    m_length += 1;
}

void StringBuilder::appendvf(const char* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

void StringBuilder::appendf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    will_append(strlen(fmt));
    appendvf(fmt, ap);
    va_end(ap);
}

ByteBuffer StringBuilder::to_byte_buffer() const
{
    ByteBuffer buffer_copy = m_buffer.isolated_copy();
    buffer_copy.trim(m_length);
    return buffer_copy;
}

String StringBuilder::to_string() const
{
    if (is_empty())
        return String::empty();
    return String((const char*)m_buffer.data(), m_length);
}

String StringBuilder::build() const
{
    return to_string();
}

StringView StringBuilder::string_view() const
{
    return StringView { (const char*)m_buffer.data(), m_length };
}

void StringBuilder::clear()
{
    m_buffer.clear();
    m_length = 0;
}

void StringBuilder::append_code_point(u32 code_point)
{
    if (code_point <= 0x7f) {
        append((char)code_point);
    } else if (code_point <= 0x07ff) {
        append((char)(((code_point >> 6) & 0x1f) | 0xc0));
        append((char)(((code_point >> 0) & 0x3f) | 0x80));
    } else if (code_point <= 0xffff) {
        append((char)(((code_point >> 12) & 0x0f) | 0xe0));
        append((char)(((code_point >> 6) & 0x3f) | 0x80));
        append((char)(((code_point >> 0) & 0x3f) | 0x80));
    } else if (code_point <= 0x10ffff) {
        append((char)(((code_point >> 18) & 0x07) | 0xf0));
        append((char)(((code_point >> 12) & 0x3f) | 0x80));
        append((char)(((code_point >> 6) & 0x3f) | 0x80));
        append((char)(((code_point >> 0) & 0x3f) | 0x80));
    } else {
        append(0xef);
        append(0xbf);
        append(0xbd);
    }
}

void StringBuilder::append(const Utf32View& utf32_view)
{
    for (size_t i = 0; i < utf32_view.length(); ++i) {
        auto code_point = utf32_view.code_points()[i];
        append_code_point(code_point);
    }
}

}
