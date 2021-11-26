/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <AK/StringHash.h>
#include <AK/StringUtils.h>

namespace AK {

class StringView {
public:
    ALWAYS_INLINE constexpr StringView() = default;
    ALWAYS_INLINE constexpr StringView(const char* characters, size_t length)
        : m_characters(characters)
        , m_length(length)
    {
        if (!is_constant_evaluated())
            VERIFY(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
    }
    ALWAYS_INLINE StringView(const unsigned char* characters, size_t length)
        : m_characters((const char*)characters)
        , m_length(length)
    {
        VERIFY(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
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

    explicit StringView(ByteBuffer&&) = delete;
    explicit StringView(String&&) = delete;
    explicit StringView(FlyString&&) = delete;

    [[nodiscard]] constexpr bool is_null() const { return m_characters == nullptr; }
    [[nodiscard]] constexpr bool is_empty() const { return m_length == 0; }

    [[nodiscard]] constexpr char const* characters_without_null_termination() const { return m_characters; }
    [[nodiscard]] constexpr size_t length() const { return m_length; }

    [[nodiscard]] ReadonlyBytes bytes() const { return { m_characters, m_length }; }

    constexpr const char& operator[](size_t index) const { return m_characters[index]; }

    using ConstIterator = SimpleIterator<const StringView, const char>;

    [[nodiscard]] constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    [[nodiscard]] constexpr ConstIterator end() const { return ConstIterator::end(*this); }

    [[nodiscard]] constexpr unsigned hash() const
    {
        if (is_empty())
            return 0;
        return string_hash(characters_without_null_termination(), length());
    }

    [[nodiscard]] bool starts_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool ends_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool starts_with(char) const;
    [[nodiscard]] bool ends_with(char) const;
    [[nodiscard]] bool matches(StringView mask, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;
    [[nodiscard]] bool matches(StringView mask, Vector<MaskSpan>&, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;
    [[nodiscard]] bool contains(char) const;
    [[nodiscard]] bool contains(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool equals_ignoring_case(StringView other) const;

    [[nodiscard]] StringView trim(StringView characters, TrimMode mode = TrimMode::Both) const { return StringUtils::trim(*this, characters, mode); }
    [[nodiscard]] StringView trim_whitespace(TrimMode mode = TrimMode::Both) const { return StringUtils::trim_whitespace(*this, mode); }

    [[nodiscard]] String to_lowercase_string() const;
    [[nodiscard]] String to_uppercase_string() const;
    [[nodiscard]] String to_titlecase_string() const;

    [[nodiscard]] Optional<size_t> find(char needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find(StringView needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find_last(char needle) const { return StringUtils::find_last(*this, needle); }
    // FIXME: Implement find_last(StringView) for API symmetry.

    [[nodiscard]] Vector<size_t> find_all(StringView needle) const;

    using SearchDirection = StringUtils::SearchDirection;
    [[nodiscard]] Optional<size_t> find_any_of(StringView needles, SearchDirection direction = SearchDirection::Forward) const { return StringUtils::find_any_of(*this, needles, direction); }

    [[nodiscard]] constexpr StringView substring_view(size_t start, size_t length) const
    {
        if (!is_constant_evaluated())
            VERIFY(start + length <= m_length);
        return { m_characters + start, length };
    }

    [[nodiscard]] constexpr StringView substring_view(size_t start) const
    {
        if (!is_constant_evaluated())
            VERIFY(start <= length());
        return substring_view(start, length() - start);
    }

    [[nodiscard]] Vector<StringView> split_view(char, bool keep_empty = false) const;
    [[nodiscard]] Vector<StringView> split_view(StringView, bool keep_empty = false) const;

    [[nodiscard]] Vector<StringView> split_view_if(Function<bool(char)> const& predicate, bool keep_empty = false) const;

    // Create a Vector of StringViews split by line endings. As of CommonMark
    // 0.29, the spec defines a line ending as "a newline (U+000A), a carriage
    // return (U+000D) not followed by a newline, or a carriage return and a
    // following newline.".
    [[nodiscard]] Vector<StringView> lines(bool consider_cr = true) const;

    template<typename T = int>
    Optional<T> to_int() const;
    template<typename T = unsigned>
    Optional<T> to_uint() const;

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
    [[nodiscard]] StringView substring_view_starting_from_substring(StringView substring) const;
    [[nodiscard]] StringView substring_view_starting_after_substring(StringView substring) const;

    constexpr bool operator==(const char* cstring) const
    {
        if (is_null())
            return cstring == nullptr;
        if (!cstring)
            return false;
        // NOTE: `m_characters` is not guaranteed to be null-terminated, but `cstring` is.
        const char* cp = cstring;
        for (size_t i = 0; i < m_length; ++i) {
            if (*cp == '\0')
                return false;
            if (m_characters[i] != *(cp++))
                return false;
        }
        return *cp == '\0';
    }

    constexpr bool operator!=(const char* cstring) const
    {
        return !(*this == cstring);
    }

    bool operator==(const String&) const;

    constexpr bool operator==(StringView other) const
    {
        if (is_null())
            return other.is_null();
        if (other.is_null())
            return false;
        if (length() != other.length())
            return false;
        return __builtin_memcmp(m_characters, other.m_characters, m_length) == 0;
    }

    constexpr bool operator!=(StringView other) const
    {
        return !(*this == other);
    }

    bool operator<(StringView other) const
    {
        if (int c = __builtin_memcmp(m_characters, other.m_characters, min(m_length, other.m_length)))
            return c < 0;
        return m_length < other.m_length;
    }

    [[nodiscard]] String to_string() const;

    [[nodiscard]] bool is_whitespace() const { return StringUtils::is_whitespace(*this); }

    [[nodiscard]] String replace(StringView needle, StringView replacement, bool all_occurrences = false) const;
    [[nodiscard]] size_t count(StringView needle) const { return StringUtils::count(*this, needle); }

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts&&... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

private:
    friend class String;
    const char* m_characters { nullptr };
    size_t m_length { 0 };
};

template<>
struct Traits<StringView> : public GenericTraits<StringView> {
    static unsigned hash(StringView s) { return s.hash(); }
};

}

[[nodiscard]] ALWAYS_INLINE constexpr AK::StringView operator"" sv(const char* cstring, size_t length)
{
    return AK::StringView(cstring, length);
}

using AK::StringView;
