/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

namespace Detail {
class StringData;
}

// String is a strongly owned sequence of Unicode code points encoded as UTF-8.
// The data may or may not be heap-allocated, and may or may not be reference counted.
// There is no guarantee that the underlying bytes are null-terminated.
class String {
public:
    // NOTE: For short strings, we avoid heap allocations by storing them in the data pointer slot.
    static constexpr size_t MAX_SHORT_STRING_BYTE_COUNT = sizeof(Detail::StringData*) - 1;

    String(String const&);
    String(String&&);

    String& operator=(String&&);
    String& operator=(String const&);

    ~String();

    // Creates an empty (zero-length) String.
    String();

    // Creates a new String from a sequence of UTF-8 encoded code points.
    static ErrorOr<String> from_utf8(StringView);

    // Creates a substring with a deep copy of the specified data window.
    ErrorOr<String> substring_from_byte_offset(size_t start, size_t byte_count) const;

    // Creates a substring that strongly references the origin superstring instead of making a deep copy of the data.
    ErrorOr<String> substring_from_byte_offset_with_shared_superstring(size_t start, size_t byte_count) const;

    // Returns an iterable view over the Unicode code points.
    [[nodiscard]] Utf8View code_points() const;

    // Returns the underlying UTF-8 encoded bytes.
    // NOTE: There is no guarantee about null-termination.
    [[nodiscard]] ReadonlyBytes bytes() const;

    // Returns true if the String is zero-length.
    [[nodiscard]] bool is_empty() const;

    // Returns a StringView covering the full length of the string. Note that iterating this will go byte-at-a-time, not code-point-at-a-time.
    [[nodiscard]] StringView bytes_as_string_view() const;

    ErrorOr<String> replace(StringView needle, StringView replacement, ReplaceMode replace_mode) const;

    [[nodiscard]] bool operator==(String const&) const;
    [[nodiscard]] bool operator!=(String const& other) const { return !(*this == other); }

    [[nodiscard]] bool operator==(StringView) const;
    [[nodiscard]] bool operator!=(StringView other) const { return !(*this == other); }

    [[nodiscard]] bool operator==(char const* cstring) const;
    [[nodiscard]] bool operator!=(char const* cstring) const { return !(*this == cstring); }

    [[nodiscard]] u32 hash() const;

    template<Arithmetic T>
    static ErrorOr<String> number(T value)
    {
        return formatted("{}", value);
    }

    static ErrorOr<String> vformatted(StringView fmtstr, TypeErasedFormatParams&);

    template<typename... Parameters>
    static ErrorOr<String> formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    // NOTE: This is primarily interesting to unit tests.
    [[nodiscard]] bool is_short_string() const;

    // FIXME: Remove these once all code has been ported to String
    [[nodiscard]] DeprecatedString to_deprecated_string() const;
    static ErrorOr<String> from_deprecated_string(DeprecatedString const&);

private:
    // NOTE: If the least significant bit of the pointer is set, this is a short string.
    static constexpr uintptr_t SHORT_STRING_FLAG = 1;

    struct ShortString {
        ReadonlyBytes bytes() const;
        size_t byte_count() const;

        // NOTE: This is the byte count shifted left 1 step and or'ed with a 1 (the SHORT_STRING_FLAG)
        u8 byte_count_and_short_string_flag { 0 };
        u8 storage[MAX_SHORT_STRING_BYTE_COUNT] = { 0 };
    };

    explicit String(NonnullRefPtr<Detail::StringData>);
    explicit String(ShortString);

    union {
        ShortString m_short_string;
        Detail::StringData* m_data { nullptr };
    };
};

template<>
struct Traits<String> : public GenericTraits<String> {
    static unsigned hash(String const&);
};

template<>
struct Formatter<String> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder&, String const&);
};

}
