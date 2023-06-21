/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringUtils.h>

namespace AK {

class DeprecatedFlyString {
public:
    DeprecatedFlyString() = default;
    DeprecatedFlyString(DeprecatedFlyString const& other)
        : m_impl(other.impl())
    {
    }
    DeprecatedFlyString(DeprecatedFlyString&& other)
        : m_impl(move(other.m_impl))
    {
    }
    DeprecatedFlyString(DeprecatedString const&);
    DeprecatedFlyString(StringView);
    DeprecatedFlyString(char const* string)
        : DeprecatedFlyString(static_cast<DeprecatedString>(string))
    {
    }

    static DeprecatedFlyString from_fly_impl(NonnullRefPtr<StringImpl const> impl)
    {
        VERIFY(impl->is_fly());
        DeprecatedFlyString string;
        string.m_impl = move(impl);
        return string;
    }

    DeprecatedFlyString& operator=(DeprecatedFlyString const& other)
    {
        m_impl = other.m_impl;
        return *this;
    }

    DeprecatedFlyString& operator=(DeprecatedFlyString&& other)
    {
        m_impl = move(other.m_impl);
        return *this;
    }

    bool is_empty() const { return !m_impl || !m_impl->length(); }
    bool is_null() const { return !m_impl; }

    bool operator==(DeprecatedFlyString const& other) const { return m_impl == other.m_impl; }

    bool operator==(DeprecatedString const&) const;

    bool operator==(StringView) const;

    bool operator==(char const*) const;

    StringImpl const* impl() const { return m_impl; }
    char const* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    size_t length() const { return m_impl ? m_impl->length() : 0; }

    ALWAYS_INLINE u32 hash() const { return m_impl ? m_impl->existing_hash() : 0; }
    ALWAYS_INLINE StringView view() const { return m_impl ? m_impl->view() : StringView {}; }

    DeprecatedFlyString to_lowercase() const;

    template<typename T = int>
    Optional<T> to_int(TrimWhitespace = TrimWhitespace::Yes) const;
    template<typename T = unsigned>
    Optional<T> to_uint(TrimWhitespace = TrimWhitespace::Yes) const;
#ifndef KERNEL
    Optional<double> to_double(TrimWhitespace = TrimWhitespace::Yes) const;
    Optional<float> to_float(TrimWhitespace = TrimWhitespace::Yes) const;
#endif

    bool equals_ignoring_ascii_case(StringView) const;
    bool starts_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    bool ends_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    static void did_destroy_impl(Badge<StringImpl>, StringImpl&);

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

private:
    RefPtr<StringImpl const> m_impl;
};

template<>
struct Traits<DeprecatedFlyString> : public GenericTraits<DeprecatedFlyString> {
    static unsigned hash(DeprecatedFlyString const& s) { return s.hash(); }
};

}

#if USING_AK_GLOBALLY
using AK::DeprecatedFlyString;
#endif
