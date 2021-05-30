/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Checked.h>
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
    Checked<size_t> needed_capacity = m_buffer.size();
    needed_capacity += size;
    VERIFY(!needed_capacity.has_overflow());
    // Prefer to completely use the existing capacity first
    if (needed_capacity <= m_buffer.capacity())
        return;
    Checked<size_t> expanded_capacity = needed_capacity;
    expanded_capacity *= 2;
    VERIFY(!expanded_capacity.has_overflow());
    m_buffer.ensure_capacity(expanded_capacity.value());
}

StringBuilder::StringBuilder(size_t initial_capacity)
{
    m_buffer.ensure_capacity(initial_capacity);
}

void StringBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    will_append(str.length());
    m_buffer.append(str.characters_without_null_termination(), str.length());
}

void StringBuilder::append(const char* characters, size_t length)
{
    append(StringView { characters, length });
}

void StringBuilder::append(char ch)
{
    will_append(1);
    m_buffer.append(&ch, 1);
}

void StringBuilder::appendvf(const char* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

ByteBuffer StringBuilder::to_byte_buffer() const
{
    return ByteBuffer::copy(data(), length());
}

String StringBuilder::to_string() const
{
    if (is_empty())
        return String::empty();
    return String((const char*)data(), length());
}

String StringBuilder::build() const
{
    return to_string();
}

StringView StringBuilder::string_view() const
{
    return StringView { data(), m_buffer.size() };
}

void StringBuilder::clear()
{
    m_buffer.clear();
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

void StringBuilder::append_as_lowercase(char ch)
{
    if (ch >= 'A' && ch <= 'Z')
        append(ch + 0x20);
    else
        append(ch);
}

void StringBuilder::append_escaped_for_json(const StringView& string)
{
    for (auto ch : string) {
        switch (ch) {
        case '\e':
            append("\\u001B");
            break;
        case '\b':
            append("\\b");
            break;
        case '\n':
            append("\\n");
            break;
        case '\t':
            append("\\t");
            break;
        case '\"':
            append("\\\"");
            break;
        case '\\':
            append("\\\\");
            break;
        default:
            append(ch);
        }
    }
}

}
