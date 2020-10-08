/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <AK/Forward.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <AK/StringUtils.h>

namespace AK {

class StringView {
public:
    ALWAYS_INLINE constexpr StringView() { }
    ALWAYS_INLINE constexpr StringView(const char* characters, size_t length)
        : m_characters(characters)
        , m_length(length)
    {
        ASSERT(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
    }
    ALWAYS_INLINE StringView(const unsigned char* characters, size_t length)
        : m_characters((const char*)characters)
        , m_length(length)
    {
        ASSERT(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
    }
    ALWAYS_INLINE constexpr StringView(const char* cstring)
        : m_characters(cstring)
        , m_length(cstring ? __builtin_strlen(cstring) : 0)
    {
    }
    ALWAYS_INLINE StringView(ReadonlyBytes bytes)
        : m_characters(reinterpret_cast<const char*>(bytes.data()))
        , m_length(bytes.size())
    {
    }

    StringView(const ByteBuffer&);
    StringView(const String&);
    StringView(const FlyString&);

    bool is_null() const { return !m_characters; }
    bool is_empty() const { return m_length == 0; }

    const char* characters_without_null_termination() const { return m_characters; }
    size_t length() const { return m_length; }

    ReadonlyBytes bytes() const { return { m_characters, m_length }; }

    const char& operator[](size_t index) const { return m_characters[index]; }

    using ConstIterator = SimpleIterator<const StringView, const char>;

    constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    constexpr ConstIterator end() const { return ConstIterator::end(*this); }

    unsigned hash() const;

    bool starts_with(const StringView&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    bool ends_with(const StringView&, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    bool starts_with(char) const;
    bool ends_with(char) const;
    bool matches(const StringView& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;
    bool contains(char) const;
    bool contains(const StringView&) const;
    bool equals_ignoring_case(const StringView& other) const;

    StringView trim_whitespace(TrimMode mode = TrimMode::Both) const { return StringUtils::trim_whitespace(*this, mode); }

    Optional<size_t> find_first_of(char) const;
    Optional<size_t> find_first_of(const StringView&) const;

    Optional<size_t> find_last_of(char) const;
    Optional<size_t> find_last_of(const StringView&) const;

    StringView substring_view(size_t start, size_t length) const;
    StringView substring_view(size_t start) const;
    Vector<StringView> split_view(char, bool keep_empty = false) const;
    Vector<StringView> split_view(const StringView&, bool keep_empty = false) const;

    // Create a Vector of StringViews split by line endings. As of CommonMark
    // 0.29, the spec defines a line ending as "a newline (U+000A), a carriage
    // return (U+000D) not followed by a newline, or a carriage return and a
    // following newline.".
    Vector<StringView> lines(bool consider_cr = true) const;

    Optional<int> to_int() const;
    Optional<unsigned> to_uint() const;

    // Create a new substring view of this string view, starting either at the beginning of
    // the given substring view, or after its end, and continuing until the end of this string
    // view (that is, for the remaining part of its length). For example,
    //
    //    StringView str { "foobar" };
    //    StringView substr = str.substring_view(1, 2);  // "oo"
    //    StringView substr_from = str.substring_view_starting_from_substring(subst);  // "oobar"
    //    StringView substr_after = str.substring_view_starting_after_substring(subst);  // "bar"
    //
    // Note that this only works if the string view passed as an argument is indeed a substring
    // view of this string view, such as one created by substring_view() and split_view(). It
    // does not work for arbitrary strings; for example declaring substr in the example above as
    //
    //     StringView substr { "oo" };
    //
    // would not work.
    StringView substring_view_starting_from_substring(const StringView& substring) const;
    StringView substring_view_starting_after_substring(const StringView& substring) const;

    bool operator==(const char* cstring) const
    {
        if (is_null())
            return !cstring;
        if (!cstring)
            return false;
        size_t other_length = __builtin_strlen(cstring);
        if (m_length != other_length)
            return false;
        return !__builtin_memcmp(m_characters, cstring, m_length);
    }
    bool operator!=(const char* cstring) const
    {
        return !(*this == cstring);
    }

    bool operator==(const String&) const;

    bool operator==(const StringView& other) const
    {
        if (is_null())
            return other.is_null();
        if (other.is_null())
            return false;
        if (length() != other.length())
            return false;
        return !__builtin_memcmp(m_characters, other.m_characters, m_length);
    }

    bool operator!=(const StringView& other) const
    {
        return !(*this == other);
    }

    bool operator<(const StringView& other) const
    {
        if (int c = __builtin_memcmp(m_characters, other.m_characters, min(m_length, other.m_length)))
            return c < 0;
        return m_length < other.m_length;
    }

    const StringImpl* impl() const { return m_impl; }

    String to_string() const;

    const char* begin() { return m_characters; }
    const char* end() { return m_characters + m_length; }

private:
    friend class String;
    const StringImpl* m_impl { nullptr };
    const char* m_characters { nullptr };
    size_t m_length { 0 };
};

template<>
struct Traits<StringView> : public GenericTraits<String> {
    static unsigned hash(const StringView& s) { return s.hash(); }
};

}

using AK::StringView;
