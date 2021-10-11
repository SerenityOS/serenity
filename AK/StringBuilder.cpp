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
#include <AK/UnicodeUtils.h>
#include <AK/Utf16View.h>
#include <AK/Utf32View.h>

namespace AK {

inline bool StringBuilder::will_append(size_t size)
{
    Checked<size_t> needed_capacity = m_buffer.size();
    needed_capacity += size;
    VERIFY(!needed_capacity.has_overflow());
    // Prefer to completely use the existing capacity first
    if (needed_capacity <= m_buffer.capacity())
        return true;
    Checked<size_t> expanded_capacity = needed_capacity;
    expanded_capacity *= 2;
    VERIFY(!expanded_capacity.has_overflow());
    return m_buffer.try_ensure_capacity(expanded_capacity.value());
}

StringBuilder::StringBuilder(size_t initial_capacity)
{
    m_buffer.ensure_capacity(initial_capacity);
}

void StringBuilder::append(StringView const& str)
{
    if (str.is_empty())
        return;
    auto ok = will_append(str.length());
    VERIFY(ok);
    ok = m_buffer.try_append(str.characters_without_null_termination(), str.length());
    VERIFY(ok);
}

void StringBuilder::append(char const* characters, size_t length)
{
    append(StringView { characters, length });
}

void StringBuilder::append(char ch)
{
    auto ok = will_append(1);
    VERIFY(ok);
    ok = m_buffer.try_append(&ch, 1);
    VERIFY(ok);
}

void StringBuilder::appendvf(char const* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

ByteBuffer StringBuilder::to_byte_buffer() const
{
    // FIXME: Handle OOM failure.
    return ByteBuffer::copy(data(), length()).release_value();
}

String StringBuilder::to_string() const
{
    if (is_empty())
        return String::empty();
    return String((char const*)data(), length());
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
    auto nwritten = AK::UnicodeUtils::code_point_to_utf8(code_point, [this](char c) { append(c); });
    if (nwritten < 0) {
        append(0xef);
        append(0xbf);
        append(0xbd);
    }
}

void StringBuilder::append(Utf16View const& utf16_view)
{
    for (size_t i = 0; i < utf16_view.length_in_code_units();) {
        auto code_point = utf16_view.code_point_at(i);
        append_code_point(code_point);

        i += (code_point > 0xffff ? 2 : 1);
    }
}

void StringBuilder::append(Utf32View const& utf32_view)
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

void StringBuilder::append_escaped_for_json(StringView const& string)
{
    for (auto ch : string) {
        switch (ch) {
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
            if (ch >= 0 && ch <= 0x1f)
                append(String::formatted("\\u{:04x}", ch));
            else
                append(ch);
        }
    }
}

}
