/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Checked.h>
#include <AK/PrintfImplementation.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/UnicodeUtils.h>
#include <AK/Utf32View.h>

#ifndef KERNEL
#    include <AK/DeprecatedString.h>
#    include <AK/FlyString.h>
#    include <AK/Utf16View.h>
#endif

namespace AK {

inline ErrorOr<void> StringBuilder::will_append(size_t size)
{
    if (m_use_inline_capacity_only == UseInlineCapacityOnly::Yes) {
        VERIFY(m_buffer.capacity() == StringBuilder::inline_capacity);
        Checked<size_t> current_pointer = m_buffer.size();
        current_pointer += size;
        VERIFY(!current_pointer.has_overflow());
        if (current_pointer <= StringBuilder::inline_capacity) {
            return {};
        }
        return Error::from_errno(ENOMEM);
    }

    Checked<size_t> needed_capacity = m_buffer.size();
    needed_capacity += size;
    VERIFY(!needed_capacity.has_overflow());
    // Prefer to completely use the existing capacity first
    if (needed_capacity <= m_buffer.capacity())
        return {};
    Checked<size_t> expanded_capacity = needed_capacity;
    expanded_capacity *= 2;
    VERIFY(!expanded_capacity.has_overflow());
    TRY(m_buffer.try_ensure_capacity(expanded_capacity.value()));
    return {};
}

ErrorOr<StringBuilder> StringBuilder::create(size_t initial_capacity)
{
    StringBuilder builder;
    TRY(builder.m_buffer.try_ensure_capacity(initial_capacity));
    return builder;
}

StringBuilder::StringBuilder(size_t initial_capacity)
{
    m_buffer.ensure_capacity(initial_capacity);
}

StringBuilder::StringBuilder(UseInlineCapacityOnly use_inline_capacity_only)
    : m_use_inline_capacity_only(use_inline_capacity_only)
{
}

size_t StringBuilder::length() const
{
    return m_buffer.size();
}

bool StringBuilder::is_empty() const
{
    return m_buffer.is_empty();
}

void StringBuilder::trim(size_t count)
{
    auto decrease_count = min(m_buffer.size(), count);
    m_buffer.resize(m_buffer.size() - decrease_count);
}

ErrorOr<void> StringBuilder::try_append(StringView string)
{
    if (string.is_empty())
        return {};
    TRY(will_append(string.length()));
    TRY(m_buffer.try_append(string.characters_without_null_termination(), string.length()));
    return {};
}

ErrorOr<void> StringBuilder::try_append(char ch)
{
    TRY(will_append(1));
    TRY(m_buffer.try_append(ch));
    return {};
}

ErrorOr<void> StringBuilder::try_append_repeated(char ch, size_t n)
{
    TRY(will_append(n));
    for (size_t i = 0; i < n; ++i)
        TRY(try_append(ch));
    return {};
}

void StringBuilder::append(StringView string)
{
    MUST(try_append(string));
}

ErrorOr<void> StringBuilder::try_append(char const* characters, size_t length)
{
    return try_append(StringView { characters, length });
}

void StringBuilder::append(char const* characters, size_t length)
{
    MUST(try_append(characters, length));
}

void StringBuilder::append(char ch)
{
    MUST(try_append(ch));
}

void StringBuilder::appendvf(char const* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

void StringBuilder::append_repeated(char ch, size_t n)
{
    MUST(try_append_repeated(ch, n));
}

ErrorOr<ByteBuffer> StringBuilder::to_byte_buffer() const
{
    return ByteBuffer::copy(data(), length());
}

#ifndef KERNEL
DeprecatedString StringBuilder::to_deprecated_string() const
{
    if (is_empty())
        return DeprecatedString::empty();
    return DeprecatedString((char const*)data(), length());
}

ErrorOr<String> StringBuilder::to_string() const
{
    return String::from_utf8(string_view());
}

ErrorOr<FlyString> StringBuilder::to_fly_string() const
{
    return FlyString::from_utf8(string_view());
}
#endif

u8* StringBuilder::data()
{
    return m_buffer.data();
}

u8 const* StringBuilder::data() const
{
    return m_buffer.data();
}

StringView StringBuilder::string_view() const
{
    return StringView { data(), m_buffer.size() };
}

void StringBuilder::clear()
{
    m_buffer.clear();
}

ErrorOr<void> StringBuilder::try_append_code_point(u32 code_point)
{
    auto nwritten = TRY(AK::UnicodeUtils::try_code_point_to_utf8(code_point, [this](char c) { return try_append(c); }));
    if (nwritten < 0) {
        TRY(try_append(0xef));
        TRY(try_append(0xbf));
        TRY(try_append(0xbd));
    }
    return {};
}

void StringBuilder::append_code_point(u32 code_point)
{
    MUST(try_append_code_point(code_point));
}

#ifndef KERNEL
ErrorOr<void> StringBuilder::try_append(Utf16View const& utf16_view)
{
    for (size_t i = 0; i < utf16_view.length_in_code_units();) {
        auto code_point = utf16_view.code_point_at(i);
        TRY(try_append_code_point(code_point));

        i += (code_point > 0xffff ? 2 : 1);
    }
    return {};
}

void StringBuilder::append(Utf16View const& utf16_view)
{
    MUST(try_append(utf16_view));
}
#endif

ErrorOr<void> StringBuilder::try_append(Utf32View const& utf32_view)
{
    for (size_t i = 0; i < utf32_view.length(); ++i) {
        auto code_point = utf32_view.code_points()[i];
        TRY(try_append_code_point(code_point));
    }
    return {};
}

void StringBuilder::append(Utf32View const& utf32_view)
{
    MUST(try_append(utf32_view));
}

void StringBuilder::append_as_lowercase(char ch)
{
    if (ch >= 'A' && ch <= 'Z')
        append(ch + 0x20);
    else
        append(ch);
}

void StringBuilder::append_escaped_for_json(StringView string)
{
    MUST(try_append_escaped_for_json(string));
}

ErrorOr<void> StringBuilder::try_append_escaped_for_json(StringView string)
{
    for (auto ch : string) {
        switch (ch) {
        case '\b':
            TRY(try_append("\\b"sv));
            break;
        case '\n':
            TRY(try_append("\\n"sv));
            break;
        case '\t':
            TRY(try_append("\\t"sv));
            break;
        case '\"':
            TRY(try_append("\\\""sv));
            break;
        case '\\':
            TRY(try_append("\\\\"sv));
            break;
        default:
            if (ch >= 0 && ch <= 0x1f)
                TRY(try_appendff("\\u{:04x}", ch));
            else
                TRY(try_append(ch));
        }
    }
    return {};
}

}
