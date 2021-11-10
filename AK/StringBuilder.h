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

    void append(StringView const&);
    void append(Utf16View const&);
    void append(Utf32View const&);
    void append(char);
    void append_code_point(u32);
    void append(char const*, size_t);
    void appendvf(char const*, va_list);

    void append_as_lowercase(char);
    void append_escaped_for_json(StringView const&);

    template<typename... Parameters>
    void appendff(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams variadic_format_params { parameters... };
        vformat(*this, fmtstr.view(), variadic_format_params);
    }

    [[nodiscard]] String build() const;
    [[nodiscard]] String to_string() const;
    [[nodiscard]] ByteBuffer to_byte_buffer() const;

    [[nodiscard]] StringView string_view() const;
    void clear();

    [[nodiscard]] size_t length() const { return m_buffer.size(); }
    [[nodiscard]] bool is_empty() const { return m_buffer.is_empty(); }
    void trim(size_t count) { m_buffer.resize(m_buffer.size() - count); }

    template<class SeparatorType, class CollectionType>
    void join(SeparatorType const& separator, CollectionType const& collection)
    {
        bool first = true;
        for (auto& item : collection) {
            if (first)
                first = false;
            else
                append(separator);
            appendff("{}", item);
        }
    }

private:
    ErrorOr<void> will_append(size_t);
    u8* data() { return m_buffer.data(); }
    u8 const* data() const { return m_buffer.data(); }

    static constexpr size_t inline_capacity = 128;
    AK::Detail::ByteBuffer<inline_capacity> m_buffer;
};

}

using AK::StringBuilder;
