/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <AK/UnicodeUtils.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

namespace AK {

namespace Detail {
class StringData;
}

// FIXME: Remove this when OpenBSD Clang fully supports consteval.
//        And once oss-fuzz updates to clang >15.
//        And once Android ships an NDK with clang >14
#if defined(AK_OS_OPENBSD) || defined(OSS_FUZZ) || defined(AK_OS_ANDROID)
#    define AK_SHORT_STRING_CONSTEVAL constexpr
#else
#    define AK_SHORT_STRING_CONSTEVAL consteval
#endif

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

    constexpr ~String()
    {
        if (!is_constant_evaluated())
            destroy_string();
    }

    // Creates an empty (zero-length) String.
    constexpr String()
        : String(ShortString { SHORT_STRING_FLAG, {} })
    {
    }

    // Creates a new String from a sequence of UTF-8 encoded code points.
    static ErrorOr<String> from_utf8(StringView);
    template<typename T>
    requires(IsOneOf<RemoveCVReference<T>, DeprecatedString, DeprecatedFlyString>)
    static ErrorOr<String> from_utf8(T&&) = delete;

    // Creates a new String by reading byte_count bytes from a UTF-8 encoded Stream.
    static ErrorOr<String> from_stream(Stream&, size_t byte_count);

    // Creates a new String from a short sequence of UTF-8 encoded code points. If the provided string
    // does not fit in the short string storage, a compilation error will be emitted.
    static AK_SHORT_STRING_CONSTEVAL String from_utf8_short_string(StringView string)
    {
        VERIFY(string.length() <= MAX_SHORT_STRING_BYTE_COUNT);
        VERIFY(Utf8View { string }.validate());

        ShortString short_string;
        for (size_t i = 0; i < string.length(); ++i)
            short_string.storage[i] = string.characters_without_null_termination()[i];
        short_string.byte_count_and_short_string_flag = (string.length() << 1) | SHORT_STRING_FLAG;

        return String { short_string };
    }

    // Creates a new String from a single code point.
    static constexpr String from_code_point(u32 code_point)
    {
        VERIFY(is_unicode(code_point));

        ShortString short_string;
        size_t i = 0;

        auto length = UnicodeUtils::code_point_to_utf8(code_point, [&](auto byte) {
            short_string.storage[i++] = static_cast<u8>(byte);
        });
        short_string.byte_count_and_short_string_flag = (length << 1) | SHORT_STRING_FLAG;

        return String { short_string };
    }

    // Creates a new String with a single code point repeated N times.
    static ErrorOr<String> repeated(u32 code_point, size_t count);

    // Creates a new String by case-transforming this String. Using these methods require linking LibUnicode into your application.
    ErrorOr<String> to_lowercase(Optional<StringView> const& locale = {}) const;
    ErrorOr<String> to_uppercase(Optional<StringView> const& locale = {}) const;
    ErrorOr<String> to_titlecase(Optional<StringView> const& locale = {}) const;
    ErrorOr<String> to_casefold() const;

    // Compare this String against another string with caseless matching. Using this method requires linking LibUnicode into your application.
    [[nodiscard]] bool equals_ignoring_case(String const&) const;

    [[nodiscard]] bool starts_with(u32 code_point) const;
    [[nodiscard]] bool starts_with_bytes(StringView) const;

    [[nodiscard]] bool ends_with(u32 code_point) const;
    [[nodiscard]] bool ends_with_bytes(StringView) const;

    // Creates a substring with a deep copy of the specified data window.
    ErrorOr<String> substring_from_byte_offset(size_t start, size_t byte_count) const;
    ErrorOr<String> substring_from_byte_offset(size_t start) const;

    // Creates a substring that strongly references the origin superstring instead of making a deep copy of the data.
    ErrorOr<String> substring_from_byte_offset_with_shared_superstring(size_t start, size_t byte_count) const;
    ErrorOr<String> substring_from_byte_offset_with_shared_superstring(size_t start) const;

    // Returns an iterable view over the Unicode code points.
    [[nodiscard]] Utf8View code_points() const;

    // Returns the underlying UTF-8 encoded bytes.
    // NOTE: There is no guarantee about null-termination.
    [[nodiscard]] ReadonlyBytes bytes() const;

    // Returns true if the String is zero-length.
    [[nodiscard]] bool is_empty() const;

    // Returns a StringView covering the full length of the string. Note that iterating this will go byte-at-a-time, not code-point-at-a-time.
    [[nodiscard]] StringView bytes_as_string_view() const;

    [[nodiscard]] size_t count(StringView needle) const { return StringUtils::count(bytes_as_string_view(), needle); }

    ErrorOr<String> replace(StringView needle, StringView replacement, ReplaceMode replace_mode) const;
    ErrorOr<String> reverse() const;

    ErrorOr<String> trim(Utf8View const& code_points_to_trim, TrimMode mode = TrimMode::Both) const;
    ErrorOr<String> trim(StringView code_points_to_trim, TrimMode mode = TrimMode::Both) const;

    ErrorOr<Vector<String>> split_limit(u32 separator, size_t limit, SplitBehavior = SplitBehavior::Nothing) const;
    ErrorOr<Vector<String>> split(u32 separator, SplitBehavior = SplitBehavior::Nothing) const;

    Optional<size_t> find_byte_offset(u32 code_point, size_t from_byte_offset = 0) const;
    Optional<size_t> find_byte_offset(StringView substring, size_t from_byte_offset = 0) const;

    [[nodiscard]] bool operator==(String const&) const;
    [[nodiscard]] bool operator!=(String const& other) const { return !(*this == other); }

    [[nodiscard]] bool operator==(FlyString const&) const;
    [[nodiscard]] bool operator!=(FlyString const& other) const { return !(*this == other); }

    [[nodiscard]] bool operator==(StringView) const;
    [[nodiscard]] bool operator!=(StringView other) const { return !(*this == other); }

    [[nodiscard]] bool operator==(char const* cstring) const;
    [[nodiscard]] bool operator!=(char const* cstring) const { return !(*this == cstring); }

    // NOTE: UTF-8 is defined in a way that lexicographic ordering of code points is equivalent to lexicographic ordering of bytes.
    [[nodiscard]] int operator<=>(String const& other) const { return this->bytes_as_string_view().compare(other.bytes_as_string_view()); }

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts&&... strings) const
    {
        return (this->operator==(forward<Ts>(strings)) || ...);
    }

    [[nodiscard]] bool contains(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool contains(u32, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    [[nodiscard]] u32 hash() const;

    template<Arithmetic T>
    static ErrorOr<String> number(T value)
    {
        return formatted("{}", value);
    }

    template<Arithmetic T>
    Optional<T> to_number(TrimWhitespace trim_whitespace = TrimWhitespace::Yes) const
    {
        if constexpr (IsSigned<T>)
            return StringUtils::convert_to_int<T>(bytes_as_string_view(), trim_whitespace);
        else
            return StringUtils::convert_to_uint<T>(bytes_as_string_view(), trim_whitespace);
    }

    static ErrorOr<String> vformatted(StringView fmtstr, TypeErasedFormatParams&);

    template<typename... Parameters>
    static ErrorOr<String> formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams<AllowDebugOnlyFormatters::No, Parameters...> variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    template<class SeparatorType, class CollectionType>
    static ErrorOr<String> join(SeparatorType const& separator, CollectionType const& collection, StringView fmtstr = "{}"sv)
    {
        StringBuilder builder;
        TRY(builder.try_join(separator, collection, fmtstr));
        return builder.to_string();
    }

    // NOTE: This is primarily interesting to unit tests.
    [[nodiscard]] bool is_short_string() const;

    [[nodiscard]] static String fly_string_data_to_string(Badge<FlyString>, uintptr_t const&);
    [[nodiscard]] static StringView fly_string_data_to_string_view(Badge<FlyString>, uintptr_t const&);
    [[nodiscard]] static u32 fly_string_data_to_hash(Badge<FlyString>, uintptr_t const&);
    [[nodiscard]] uintptr_t to_fly_string_data(Badge<FlyString>) const;

    static void ref_fly_string_data(Badge<FlyString>, uintptr_t);
    static void unref_fly_string_data(Badge<FlyString>, uintptr_t);
    void did_create_fly_string(Badge<FlyString>) const;

    // FIXME: Remove these once all code has been ported to String
    [[nodiscard]] DeprecatedString to_deprecated_string() const;
    static ErrorOr<String> from_deprecated_string(DeprecatedString const&);
    template<typename T>
    requires(IsSame<RemoveCVReference<T>, StringView>)
    static ErrorOr<String> from_deprecated_string(T&&) = delete;

private:
    // NOTE: If the least significant bit of the pointer is set, this is a short string.
    static constexpr uintptr_t SHORT_STRING_FLAG = 1;

    static constexpr bool has_short_string_bit(uintptr_t data)
    {
        return (data & SHORT_STRING_FLAG) != 0;
    }

    struct ShortString {
        ReadonlyBytes bytes() const;
        size_t byte_count() const;

        // NOTE: This is the byte count shifted left 1 step and or'ed with a 1 (the SHORT_STRING_FLAG)
        u8 byte_count_and_short_string_flag { 0 };
        u8 storage[MAX_SHORT_STRING_BYTE_COUNT] = { 0 };
    };

    explicit String(NonnullRefPtr<Detail::StringData const>);

    explicit constexpr String(ShortString short_string)
        : m_short_string(short_string)
    {
    }

    void destroy_string();

    union {
        ShortString m_short_string;
        Detail::StringData const* m_data { nullptr };
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

[[nodiscard]] ALWAYS_INLINE AK::ErrorOr<AK::String> operator""_string(char const* cstring, size_t length)
{
    return AK::String::from_utf8(AK::StringView(cstring, length));
}

[[nodiscard]] ALWAYS_INLINE AK_SHORT_STRING_CONSTEVAL AK::String operator""_short_string(char const* cstring, size_t length)
{
    return AK::String::from_utf8_short_string(AK::StringView(cstring, length));
}
