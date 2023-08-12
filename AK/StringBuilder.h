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
    static constexpr size_t inline_capacity = 256;

    using OutputType = DeprecatedString;

    static ErrorOr<StringBuilder> create(size_t initial_capacity = inline_capacity);

    explicit StringBuilder(size_t initial_capacity = inline_capacity);

    enum class UseInlineCapacityOnly {
        Yes,
        No,
    };
    explicit StringBuilder(UseInlineCapacityOnly use_inline_capacity_only);
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
        VariadicFormatParams<AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
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
        VariadicFormatParams<AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
        MUST(vformat(*this, fmtstr.view(), variadic_format_params));
    }

#ifndef KERNEL
    [[nodiscard]] DeprecatedString to_deprecated_string() const;
#endif

    ErrorOr<String> to_string() const;
    ErrorOr<FlyString> to_fly_string() const;

    [[nodiscard]] ErrorOr<ByteBuffer> to_byte_buffer() const;

    [[nodiscard]] StringView string_view() const;
    void clear();

    [[nodiscard]] size_t length() const;
    [[nodiscard]] bool is_empty() const;
    void trim(size_t count);

    template<class SeparatorType, class CollectionType>
    void join(SeparatorType const& separator, CollectionType const& collection, StringView fmtstr = "{}"sv)
    {
        MUST(try_join(separator, collection, fmtstr));
    }

    template<class SeparatorType, class CollectionType>
    ErrorOr<void> try_join(SeparatorType const& separator, CollectionType const& collection, StringView fmtstr = "{}"sv)
    {
        bool first = true;
        for (auto& item : collection) {
            if (!first)
                TRY(try_append(separator));
            TRY(try_appendff(fmtstr, item));
            first = false;
        }
        return {};
    }

private:
    ErrorOr<void> will_append(size_t);
    u8* data();
    u8 const* data() const;

    UseInlineCapacityOnly m_use_inline_capacity_only { UseInlineCapacityOnly::No };
    Detail::ByteBuffer<inline_capacity> m_buffer;
};

}

#if USING_AK_GLOBALLY
using AK::StringBuilder;
#endif
