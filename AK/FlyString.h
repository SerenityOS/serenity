/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/String.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

class FlyString {
public:
    FlyString();
    ~FlyString();

    static ErrorOr<FlyString> from_utf8(StringView);
    template<typename T>
    requires(IsOneOf<RemoveCVReference<T>, DeprecatedString, DeprecatedFlyString>)
    static ErrorOr<String> from_utf8(T&&) = delete;

    FlyString(String const&);
    FlyString& operator=(String const&);

    FlyString(FlyString const&);
    FlyString& operator=(FlyString const&);

    FlyString(FlyString&&);
    FlyString& operator=(FlyString&&);

    [[nodiscard]] bool is_empty() const;
    [[nodiscard]] unsigned hash() const;

    explicit operator String() const;
    String to_string() const;

    [[nodiscard]] Utf8View code_points() const;
    [[nodiscard]] ReadonlyBytes bytes() const;
    [[nodiscard]] StringView bytes_as_string_view() const;

    [[nodiscard]] bool operator==(FlyString const& other) const;
    [[nodiscard]] bool operator==(String const&) const;
    [[nodiscard]] bool operator==(StringView) const;
    [[nodiscard]] bool operator==(char const*) const;

    static void did_destroy_fly_string_data(Badge<Detail::StringData>, StringView);
    [[nodiscard]] uintptr_t data(Badge<String>) const;

    // This is primarily interesting to unit tests.
    [[nodiscard]] static size_t number_of_fly_strings();

    // FIXME: Remove these once all code has been ported to FlyString
    [[nodiscard]] DeprecatedFlyString to_deprecated_fly_string() const;
    static ErrorOr<FlyString> from_deprecated_fly_string(DeprecatedFlyString const&);
    template<typename T>
    requires(IsSame<RemoveCVReference<T>, StringView>)
    static ErrorOr<String> from_deprecated_fly_string(T&&) = delete;

    // Compare this FlyString against another string with ASCII caseless matching.
    [[nodiscard]] bool equals_ignoring_ascii_case(FlyString const&) const;

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

private:
    // This will hold either the pointer to the Detail::StringData it represents or the raw bytes of
    // an inlined short string.
    uintptr_t m_data { 0 };
};

template<>
struct Traits<FlyString> : public GenericTraits<FlyString> {
    static unsigned hash(FlyString const&);
};

template<>
struct Formatter<FlyString> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder&, FlyString const&);
};

}

[[nodiscard]] ALWAYS_INLINE AK::FlyString operator""_fly_string(char const* cstring, size_t length)
{
    return AK::FlyString::from_utf8(AK::StringView(cstring, length)).release_value();
}

#if USING_AK_GLOBALLY
using AK::FlyString;
#endif
