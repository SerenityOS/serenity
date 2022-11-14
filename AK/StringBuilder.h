/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
    using OutputType = DeprecatedString;

    explicit StringBuilder(size_t initial_capacity = inline_capacity);
    ~StringBuilder() = default;

    ErrorOr<void> try_append(StringView);
#ifndef KERNEL
    ErrorOr<void> try_append(Utf16View const&);
#endif
    ErrorOr<void> try_append(Utf32View const&);
    ErrorOr<void> try_append_code_point(u32);
    ErrorOr<void> try_append(char);
    template<typename... Parameters>
    ErrorOr<void> try_appendff(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams variadic_format_params { parameters... };
        return vformat(*this, fmtstr.view(), variadic_format_params);
    }
    ErrorOr<void> try_append(char const*, size_t);
    ErrorOr<void> try_append_repeated(char, size_t);
    ErrorOr<void> try_append_escaped_for_json(StringView);

    void append(StringView);
#ifndef KERNEL
    void append(Utf16View const&);
#endif
    void append(Utf32View const&);
    void append(char);
    void append_code_point(u32);
    void append(char const*, size_t);
    void appendvf(char const*, va_list);
    void append_repeated(char, size_t);

    void append_as_lowercase(char);
    void append_escaped_for_json(StringView);

    template<typename... Parameters>
    void appendff(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams variadic_format_params { parameters... };
        MUST(vformat(*this, fmtstr.view(), variadic_format_params));
    }

#ifndef KERNEL
    [[nodiscard]] DeprecatedString build() const;
    [[nodiscard]] DeprecatedString to_deprecated_string() const;
    ErrorOr<String> to_string() const;
#endif

    [[nodiscard]] ByteBuffer to_byte_buffer() const;

    [[nodiscard]] StringView string_view() const;
    void clear();

    [[nodiscard]] size_t length() const { return m_buffer.size(); }
    [[nodiscard]] bool is_empty() const { return m_buffer.is_empty(); }
    void trim(size_t count) { m_buffer.resize(m_buffer.size() - count); }

    template<class SeparatorType, class CollectionType>
    void join(SeparatorType const& separator, CollectionType const& collection, StringView fmtstr = "{}"sv)
    {
        bool first = true;
        for (auto& item : collection) {
            if (first)
                first = false;
            else
                append(separator);
            appendff(fmtstr, item);
        }
    }

private:
    ErrorOr<void> will_append(size_t);
    u8* data() { return m_buffer.data(); }
    u8 const* data() const { return m_buffer.data(); }

    static constexpr size_t inline_capacity = 256;
    Detail::ByteBuffer<inline_capacity> m_buffer;
};

}

#if USING_AK_GLOBALLY
using AK::StringBuilder;
#endif
