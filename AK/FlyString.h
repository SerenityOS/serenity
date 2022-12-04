/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/StringUtils.h"
#include <AK/DeprecatedString.h>

namespace AK {

class FlyString {
public:
    FlyString() = default;
    FlyString(FlyString const& other)
        : m_impl(other.impl())
    {
    }
    FlyString(FlyString&& other)
        : m_impl(move(other.m_impl))
    {
    }
    FlyString(DeprecatedString const&);
    FlyString(StringView);
    FlyString(char const* string)
        : FlyString(static_cast<DeprecatedString>(string))
    {
    }

    static FlyString from_fly_impl(NonnullRefPtr<StringImpl> impl)
    {
        VERIFY(impl->is_fly());
        FlyString string;
        string.m_impl = move(impl);
        return string;
    }

    FlyString& operator=(FlyString const& other)
    {
        m_impl = other.m_impl;
        return *this;
    }

    FlyString& operator=(FlyString&& other)
    {
        m_impl = move(other.m_impl);
        return *this;
    }

    bool is_empty() const { return !m_impl || !m_impl->length(); }
    bool is_null() const { return !m_impl; }

    bool operator==(FlyString const& other) const { return m_impl == other.m_impl; }

    bool operator==(DeprecatedString const&) const;

    bool operator==(StringView) const;

    bool operator==(char const*) const;

    StringImpl const* impl() const { return m_impl; }
    char const* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    size_t length() const { return m_impl ? m_impl->length() : 0; }

    ALWAYS_INLINE u32 hash() const { return m_impl ? m_impl->existing_hash() : 0; }
    ALWAYS_INLINE StringView view() const { return m_impl ? m_impl->view() : StringView {}; }

    FlyString to_lowercase() const;

    template<typename T = int>
    Optional<T> to_int(TrimWhitespace = TrimWhitespace::Yes) const;
    template<typename T = unsigned>
    Optional<T> to_uint(TrimWhitespace = TrimWhitespace::Yes) const;
#ifndef KERNEL
    Optional<double> to_double(TrimWhitespace = TrimWhitespace::Yes) const;
    Optional<float> to_float(TrimWhitespace = TrimWhitespace::Yes) const;
#endif

    bool equals_ignoring_case(StringView) const;
    bool starts_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    bool ends_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    static void did_destroy_impl(Badge<StringImpl>, StringImpl&);

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

private:
    RefPtr<StringImpl> m_impl;
};

template<>
struct Traits<FlyString> : public GenericTraits<FlyString> {
    static unsigned hash(FlyString const& s) { return s.hash(); }
};

}

#if USING_AK_GLOBALLY
using AK::FlyString;
#endif
