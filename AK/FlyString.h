/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>

namespace AK {

class FlyString {
public:
    FlyString() { }
    FlyString(const FlyString& other)
        : m_impl(other.impl())
    {
    }
    FlyString(FlyString&& other)
        : m_impl(move(other.m_impl))
    {
    }
    FlyString(const String&);
    FlyString(const StringView&);
    FlyString(const char*);

    FlyString& operator=(const FlyString& other)
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

    bool operator==(const FlyString& other) const { return m_impl == other.m_impl; }
    bool operator!=(const FlyString& other) const { return m_impl != other.m_impl; }

    bool operator==(const String&) const;
    bool operator!=(const String& string) const { return !(*this == string); }

    bool operator==(const StringView&) const;
    bool operator!=(const StringView& string) const { return !(*this == string); }

    bool operator==(const char*) const;
    bool operator!=(const char* string) const { return !(*this == string); }

    const StringImpl* impl() const { return m_impl; }
    const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    size_t length() const { return m_impl ? m_impl->length() : 0; }

    ALWAYS_INLINE u32 hash() const { return m_impl ? m_impl->existing_hash() : 0; }

    StringView view() const;

    FlyString to_lowercase() const;

    Optional<int> to_int() const;

    bool equals_ignoring_case(const StringView&) const;
    bool starts_with(const StringView&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    bool ends_with(const StringView&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    static void did_destroy_impl(Badge<StringImpl>, StringImpl&);

    template<typename T, typename... Rest>
    bool is_one_of(const T& string, Rest... rest) const
    {
        if (*this == string)
            return true;
        return is_one_of(rest...);
    }

private:
    bool is_one_of() const { return false; }

    RefPtr<StringImpl> m_impl;
};

template<>
struct Traits<FlyString> : public GenericTraits<FlyString> {
    static unsigned hash(const FlyString& s) { return s.hash(); }
};

}

using AK::FlyString;
