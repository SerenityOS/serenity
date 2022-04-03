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
    ALWAYS_INLINE constexpr StringView(char const* characters, size_t length)
        : m_characters(characters)
        , m_length(length)
    {
        if (!is_constant_evaluated())
            VERIFY(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
    }
    ALWAYS_INLINE StringView(unsigned char const* characters, size_t length)
        : m_characters((char const*)characters)
        , m_length(length)
    {
        VERIFY(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)characters, length));
    }
    ALWAYS_INLINE constexpr StringView(char const* cstring)
        : m_characters(cstring)
        , m_length(cstring ? __builtin_strlen(cstring) : 0)
    {
    }
    ALWAYS_INLINE StringView(ReadonlyBytes bytes)
        : m_characters(reinterpret_cast<char const*>(bytes.data()))
        , m_length(bytes.size())
    {
    }

    StringView(ByteBuffer const&);
#ifndef KERNEL
    StringView(String const&);
    StringView(FlyString const&);
#endif

    explicit StringView(ByteBuffer&&) = delete;
#ifndef KERNEL
    explicit StringView(String&&) = delete;
    explicit StringView(FlyString&&) = delete;
#endif

    [[nodiscard]] constexpr bool is_null() const
    {
        return m_characters == nullptr;
    }
    [[nodiscard]] constexpr bool is_empty() const { return m_length == 0; }

    [[nodiscard]] constexpr char const* characters_without_null_termination() const { return m_characters; }
    [[nodiscard]] constexpr size_t length() const { return m_length; }

    [[nodiscard]] ReadonlyBytes bytes() const { return { m_characters, m_length }; }

    constexpr char const& operator[](size_t index) const { return m_characters[index]; }

    using ConstIterator = SimpleIterator<const StringView, char const>;

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

#ifndef KERNEL
    [[nodiscard]] String to_lowercase_string() const;
    [[nodiscard]] String to_uppercase_string() const;
    [[nodiscard]] String to_titlecase_string() const;
#endif

    [[nodiscard]] Optional<size_t> find(char needle, size_t start = 0) const
    {
        return StringUtils::find(*this, needle, start);
    }
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

    template<VoidFunction<StringView> Callback>
    void for_each_split_view(char separator, bool keep_empty, Callback callback) const
    {
        StringView seperator_view { &separator, 1 };
        for_each_split_view(seperator_view, keep_empty, callback);
    }

    template<VoidFunction<StringView> Callback>
    void for_each_split_view(StringView separator, bool keep_empty, Callback callback) const
    {
        VERIFY(!separator.is_empty());

        if (is_empty())
            return;

        StringView view { *this };

        auto maybe_separator_index = find(separator);
        while (maybe_separator_index.has_value()) {
            auto separator_index = maybe_separator_index.value();
            auto part_with_separator = view.substring_view(0, separator_index + separator.length());
            if (keep_empty || separator_index > 0)
                callback(part_with_separator.substring_view(0, separator_index));
            view = view.substring_view_starting_after_substring(part_with_separator);
            maybe_separator_index = view.find(separator);
        }
        if (keep_empty || !view.is_empty())
            callback(view);
    }

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

    [[nodiscard]] bool copy_characters_to_buffer(char* buffer, size_t buffer_size) const;

    constexpr bool operator==(char const* cstring) const
    {
        if (is_null())
            return cstring == nullptr;
        if (!cstring)
            return false;
        // NOTE: `m_characters` is not guaranteed to be null-terminated, but `cstring` is.
        char const* cp = cstring;
        for (size_t i = 0; i < m_length; ++i) {
            if (*cp == '\0')
                return false;
            if (m_characters[i] != *(cp++))
                return false;
        }
        return *cp == '\0';
    }

    constexpr bool operator!=(char const* cstring) const
    {
        return !(*this == cstring);
    }

#ifndef KERNEL
    bool operator==(String const&) const;
#endif

    [[nodiscard]] constexpr int compare(StringView other) const
    {
        if (m_characters == nullptr)
            return other.m_characters ? -1 : 0;

        if (other.m_characters == nullptr)
            return 1;

        size_t rlen = min(m_length, other.m_length);
        int c = __builtin_memcmp(m_characters, other.m_characters, rlen);
        if (c == 0) {
            if (length() < other.length())
                return -1;
            if (length() == other.length())
                return 0;
            return 1;
        }
        return c;
    }

    constexpr bool operator==(StringView other) const
    {
        return length() == other.length() && compare(other) == 0;
    }

    constexpr bool operator!=(StringView other) const
    {
        return length() != other.length() || compare(other) != 0;
    }

    constexpr bool operator<(StringView other) const { return compare(other) < 0; }

    constexpr bool operator<=(StringView other) const { return compare(other) <= 0; }

    constexpr bool operator>(StringView other) const { return compare(other) > 0; }

    constexpr bool operator>=(StringView other) const { return compare(other) >= 0; }

#ifndef KERNEL
    [[nodiscard]] String to_string() const;
#endif

    [[nodiscard]] bool is_whitespace() const
    {
        return StringUtils::is_whitespace(*this);
    }

#ifndef KERNEL
    [[nodiscard]] String replace(StringView needle, StringView replacement, bool all_occurrences = false) const;
#endif
    [[nodiscard]] size_t count(StringView needle) const
    {
        return StringUtils::count(*this, needle);
    }

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts&&... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of_ignoring_case(Ts&&... strings) const
    {
        return (... ||
                [this, &strings]() -> bool {
            if constexpr (requires(Ts a) { a.view()->StringView; })
                return this->equals_ignoring_case(forward<Ts>(strings.view()));
            else
                return this->equals_ignoring_case(forward<Ts>(strings));
        }());
    }

private:
    friend class String;
    char const* m_characters { nullptr };
    size_t m_length { 0 };
};

template<>
struct Traits<StringView> : public GenericTraits<StringView> {
    static unsigned hash(StringView s) { return s.hash(); }
};

struct CaseInsensitiveStringViewTraits : public Traits<StringView> {
    static unsigned hash(StringView s)
    {
        if (s.is_empty())
            return 0;
        return case_insensitive_string_hash(s.characters_without_null_termination(), s.length());
    }
};

}

// FIXME: Remove this when clang fully supports consteval (specifically in the context of default parameter initialization).
// See: https://stackoverflow.com/questions/68789984/immediate-function-as-default-function-argument-initializer-in-clang
#if defined(__clang__)
#    define AK_STRING_VIEW_LITERAL_CONSTEVAL constexpr
#else
#    define AK_STRING_VIEW_LITERAL_CONSTEVAL consteval
#endif

[[nodiscard]] ALWAYS_INLINE AK_STRING_VIEW_LITERAL_CONSTEVAL AK::StringView operator"" sv(char const* cstring, size_t length)
{
    return AK::StringView(cstring, length);
}

using AK::CaseInsensitiveStringViewTraits;
using AK::StringView;
