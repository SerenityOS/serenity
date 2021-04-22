/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/StringView.h>
#include <stdarg.h>

namespace AK {

class StringBuilder {
public:
    using OutputType = String;

    explicit StringBuilder(size_t initial_capacity = inline_capacity);
    ~StringBuilder() = default;

    void append(const StringView&);
    void append(const Utf32View&);
    void append(char);
    void append_code_point(u32);
    void append(const char*, size_t);
    void appendf(const char*, ...) __attribute__((format(printf, 2, 3)));
    void appendvf(const char*, va_list);

    void append_escaped_for_json(const StringView&);

    template<typename... Parameters>
    void appendff(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
    {
        vformat(*this, fmtstr.view(), VariadicFormatParams { parameters... });
    }

    [[nodiscard]] String build() const;
    [[nodiscard]] String to_string() const;
    [[nodiscard]] ByteBuffer to_byte_buffer() const;

    [[nodiscard]] StringView string_view() const;
    void clear();

    [[nodiscard]] size_t length() const { return m_length; }
    [[nodiscard]] bool is_empty() const { return m_length == 0; }
    void trim(size_t count) { m_length -= count; }

    template<class SeparatorType, class CollectionType>
    void join(const SeparatorType& separator, const CollectionType& collection)
    {
        bool first = true;
        for (auto& item : collection) {
            if (first)
                first = false;
            else
                append(separator);
            append(item);
        }
    }

private:
    void will_append(size_t);
    u8* data() { return m_buffer.is_null() ? m_inline_buffer : m_buffer.data(); }
    const u8* data() const { return m_buffer.is_null() ? m_inline_buffer : m_buffer.data(); }
    bool using_inline_buffer() const { return m_buffer.is_null(); }

    static constexpr size_t inline_capacity = 128;
    u8 m_inline_buffer[inline_capacity];
    ByteBuffer m_buffer;
    size_t m_length { 0 };
};

}

using AK::StringBuilder;
