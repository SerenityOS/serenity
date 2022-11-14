/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <AK/Stream.h>
#include <AK/StringBuilder.h>
#include <AK/StringImpl.h>
#include <AK/StringUtils.h>
#include <AK/Traits.h>

namespace AK {

// DeprecatedString is a convenience wrapper around StringImpl, suitable for passing
// around as a value type. It's basically the same as passing around a
// RefPtr<StringImpl>, with a bit of syntactic sugar.
//
// Note that StringImpl is an immutable object that cannot shrink or grow.
// Its allocation size is snugly tailored to the specific string it contains.
// Copying a DeprecatedString is very efficient, since the internal StringImpl is
// retainable and so copying only requires modifying the ref count.
//
// There are three main ways to construct a new DeprecatedString:
//
//     s = DeprecatedString("some literal");
//
//     s = DeprecatedString::formatted("{} little piggies", m_piggies);
//
//     StringBuilder builder;
//     builder.append("abc");
//     builder.append("123");
//     s = builder.to_deprecated_string();

class DeprecatedString {
public:
    ~DeprecatedString() = default;

    DeprecatedString() = default;

    DeprecatedString(StringView view)
        : m_impl(StringImpl::create(view.characters_without_null_termination(), view.length()))
    {
    }

    DeprecatedString(DeprecatedString const& other)
        : m_impl(const_cast<DeprecatedString&>(other).m_impl)
    {
    }

    DeprecatedString(DeprecatedString&& other)
        : m_impl(move(other.m_impl))
    {
    }

    DeprecatedString(char const* cstring, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(cstring, shouldChomp))
    {
    }

    DeprecatedString(char const* cstring, size_t length, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(cstring, length, shouldChomp))
    {
    }

    explicit DeprecatedString(ReadonlyBytes bytes, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(bytes, shouldChomp))
    {
    }

    DeprecatedString(StringImpl const& impl)
        : m_impl(const_cast<StringImpl&>(impl))
    {
    }

    DeprecatedString(StringImpl const* impl)
        : m_impl(const_cast<StringImpl*>(impl))
    {
    }

    DeprecatedString(RefPtr<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    DeprecatedString(NonnullRefPtr<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    DeprecatedString(FlyString const&);

    [[nodiscard]] static DeprecatedString repeated(char, size_t count);
    [[nodiscard]] static DeprecatedString repeated(StringView, size_t count);

    [[nodiscard]] static DeprecatedString bijective_base_from(size_t value, unsigned base = 26, StringView map = {});
    [[nodiscard]] static DeprecatedString roman_number_from(size_t value);

    template<class SeparatorType, class CollectionType>
    [[nodiscard]] static DeprecatedString join(SeparatorType const& separator, CollectionType const& collection, StringView fmtstr = "{}"sv)
    {
        StringBuilder builder;
        builder.join(separator, collection, fmtstr);
        return builder.build();
    }

    [[nodiscard]] bool matches(StringView mask, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;
    [[nodiscard]] bool matches(StringView mask, Vector<MaskSpan>&, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;

    template<typename T = int>
    [[nodiscard]] Optional<T> to_int(TrimWhitespace = TrimWhitespace::Yes) const;
    template<typename T = unsigned>
    [[nodiscard]] Optional<T> to_uint(TrimWhitespace = TrimWhitespace::Yes) const;
#ifndef KERNEL
    [[nodiscard]] Optional<double> to_double(TrimWhitespace = TrimWhitespace::Yes) const;
    [[nodiscard]] Optional<float> to_float(TrimWhitespace = TrimWhitespace::Yes) const;
#endif

    [[nodiscard]] DeprecatedString to_lowercase() const;
    [[nodiscard]] DeprecatedString to_uppercase() const;
    [[nodiscard]] DeprecatedString to_snakecase() const;
    [[nodiscard]] DeprecatedString to_titlecase() const;
    [[nodiscard]] DeprecatedString invert_case() const;

    [[nodiscard]] bool is_whitespace() const { return StringUtils::is_whitespace(*this); }

    [[nodiscard]] DeprecatedString trim(StringView characters, TrimMode mode = TrimMode::Both) const
    {
        auto trimmed_view = StringUtils::trim(view(), characters, mode);
        if (view() == trimmed_view)
            return *this;
        return trimmed_view;
    }

    [[nodiscard]] DeprecatedString trim_whitespace(TrimMode mode = TrimMode::Both) const
    {
        auto trimmed_view = StringUtils::trim_whitespace(view(), mode);
        if (view() == trimmed_view)
            return *this;
        return trimmed_view;
    }

    [[nodiscard]] bool equals_ignoring_case(StringView) const;

    [[nodiscard]] bool contains(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool contains(char, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    [[nodiscard]] Vector<DeprecatedString> split_limit(char separator, size_t limit, SplitBehavior = SplitBehavior::Nothing) const;
    [[nodiscard]] Vector<DeprecatedString> split(char separator, SplitBehavior = SplitBehavior::Nothing) const;
    [[nodiscard]] Vector<StringView> split_view(char separator, SplitBehavior = SplitBehavior::Nothing) const;
    [[nodiscard]] Vector<StringView> split_view(Function<bool(char)> separator, SplitBehavior = SplitBehavior::Nothing) const;

    [[nodiscard]] Optional<size_t> find(char needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find(StringView needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find_last(char needle) const { return StringUtils::find_last(*this, needle); }
    // FIXME: Implement find_last(StringView) for API symmetry.
    Vector<size_t> find_all(StringView needle) const;
    using SearchDirection = StringUtils::SearchDirection;
    [[nodiscard]] Optional<size_t> find_any_of(StringView needles, SearchDirection direction) const { return StringUtils::find_any_of(*this, needles, direction); }

    [[nodiscard]] StringView find_last_split_view(char separator) const { return view().find_last_split_view(separator); }

    [[nodiscard]] DeprecatedString substring(size_t start, size_t length) const;
    [[nodiscard]] DeprecatedString substring(size_t start) const;
    [[nodiscard]] StringView substring_view(size_t start, size_t length) const;
    [[nodiscard]] StringView substring_view(size_t start) const;

    [[nodiscard]] bool is_null() const { return !m_impl; }
    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return length() == 0; }
    [[nodiscard]] ALWAYS_INLINE size_t length() const { return m_impl ? m_impl->length() : 0; }
    // Includes NUL-terminator, if non-nullptr.
    [[nodiscard]] ALWAYS_INLINE char const* characters() const { return m_impl ? m_impl->characters() : nullptr; }

    [[nodiscard]] bool copy_characters_to_buffer(char* buffer, size_t buffer_size) const;

    [[nodiscard]] ALWAYS_INLINE ReadonlyBytes bytes() const
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }

    [[nodiscard]] ALWAYS_INLINE char const& operator[](size_t i) const
    {
        VERIFY(!is_null());
        return (*m_impl)[i];
    }

    using ConstIterator = SimpleIterator<const DeprecatedString, char const>;

    [[nodiscard]] constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    [[nodiscard]] constexpr ConstIterator end() const { return ConstIterator::end(*this); }

    [[nodiscard]] bool starts_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool ends_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool starts_with(char) const;
    [[nodiscard]] bool ends_with(char) const;

    bool operator==(DeprecatedString const&) const;

    bool operator==(StringView) const;

    bool operator==(FlyString const&) const;

    bool operator<(DeprecatedString const&) const;
    bool operator<(char const*) const;
    bool operator>=(DeprecatedString const& other) const { return !(*this < other); }
    bool operator>=(char const* other) const { return !(*this < other); }

    bool operator>(DeprecatedString const&) const;
    bool operator>(char const*) const;
    bool operator<=(DeprecatedString const& other) const { return !(*this > other); }
    bool operator<=(char const* other) const { return !(*this > other); }

    bool operator==(char const* cstring) const;

    [[nodiscard]] DeprecatedString isolated_copy() const;

    [[nodiscard]] static DeprecatedString empty()
    {
        return StringImpl::the_empty_stringimpl();
    }

    [[nodiscard]] StringImpl* impl() { return m_impl.ptr(); }
    [[nodiscard]] StringImpl const* impl() const { return m_impl.ptr(); }

    DeprecatedString& operator=(DeprecatedString&& other)
    {
        if (this != &other)
            m_impl = move(other.m_impl);
        return *this;
    }

    DeprecatedString& operator=(DeprecatedString const& other)
    {
        if (this != &other)
            m_impl = const_cast<DeprecatedString&>(other).m_impl;
        return *this;
    }

    DeprecatedString& operator=(std::nullptr_t)
    {
        m_impl = nullptr;
        return *this;
    }

    DeprecatedString& operator=(ReadonlyBytes bytes)
    {
        m_impl = StringImpl::create(bytes);
        return *this;
    }

    [[nodiscard]] u32 hash() const
    {
        if (!m_impl)
            return 0;
        return m_impl->hash();
    }

    [[nodiscard]] ByteBuffer to_byte_buffer() const;

    template<typename BufferType>
    [[nodiscard]] static DeprecatedString copy(BufferType const& buffer, ShouldChomp should_chomp = NoChomp)
    {
        if (buffer.is_empty())
            return empty();
        return DeprecatedString((char const*)buffer.data(), buffer.size(), should_chomp);
    }

    [[nodiscard]] static DeprecatedString vformatted(StringView fmtstr, TypeErasedFormatParams&);

    template<typename... Parameters>
    [[nodiscard]] static DeprecatedString formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    template<Arithmetic T>
    [[nodiscard]] static DeprecatedString number(T value)
    {
        return formatted("{}", value);
    }

    [[nodiscard]] StringView view() const
    {
        return { characters(), length() };
    }

    [[nodiscard]] DeprecatedString replace(StringView needle, StringView replacement, ReplaceMode replace_mode) const { return StringUtils::replace(*this, needle, replacement, replace_mode); }
    [[nodiscard]] size_t count(StringView needle) const { return StringUtils::count(*this, needle); }
    [[nodiscard]] DeprecatedString reverse() const;

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
    RefPtr<StringImpl> m_impl;
};

template<>
struct Traits<DeprecatedString> : public GenericTraits<DeprecatedString> {
    static unsigned hash(DeprecatedString const& s) { return s.impl() ? s.impl()->hash() : 0; }
};

struct CaseInsensitiveStringTraits : public Traits<DeprecatedString> {
    static unsigned hash(DeprecatedString const& s) { return s.impl() ? s.impl()->case_insensitive_hash() : 0; }
    static bool equals(DeprecatedString const& a, DeprecatedString const& b) { return a.equals_ignoring_case(b); }
};

DeprecatedString escape_html_entities(StringView html);

InputStream& operator>>(InputStream& stream, DeprecatedString& string);

}

#if USING_AK_GLOBALLY
using AK::CaseInsensitiveStringTraits;
using AK::escape_html_entities;
#endif
