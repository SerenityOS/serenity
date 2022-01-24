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

// String is a convenience wrapper around StringImpl, suitable for passing
// around as a value type. It's basically the same as passing around a
// RefPtr<StringImpl>, with a bit of syntactic sugar.
//
// Note that StringImpl is an immutable object that cannot shrink or grow.
// Its allocation size is snugly tailored to the specific string it contains.
// Copying a String is very efficient, since the internal StringImpl is
// retainable and so copying only requires modifying the ref count.
//
// There are three main ways to construct a new String:
//
//     s = String("some literal");
//
//     s = String::formatted("{} little piggies", m_piggies);
//
//     StringBuilder builder;
//     builder.append("abc");
//     builder.append("123");
//     s = builder.to_string();

class String {
public:
    ~String() = default;

    String() = default;

    String(StringView view)
        : m_impl(StringImpl::create(view.characters_without_null_termination(), view.length()))
    {
    }

    String(const String& other)
        : m_impl(const_cast<String&>(other).m_impl)
    {
    }

    String(String&& other)
        : m_impl(move(other.m_impl))
    {
    }

    String(const char* cstring, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(cstring, shouldChomp))
    {
    }

    String(const char* cstring, size_t length, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(cstring, length, shouldChomp))
    {
    }

    explicit String(ReadonlyBytes bytes, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(bytes, shouldChomp))
    {
    }

    String(const StringImpl& impl)
        : m_impl(const_cast<StringImpl&>(impl))
    {
    }

    String(const StringImpl* impl)
        : m_impl(const_cast<StringImpl*>(impl))
    {
    }

    String(RefPtr<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    String(NonnullRefPtr<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    String(const FlyString&);

    [[nodiscard]] static String repeated(char, size_t count);
    [[nodiscard]] static String repeated(StringView, size_t count);

    [[nodiscard]] static String bijective_base_from(size_t value, unsigned base = 26, StringView map = {});
    [[nodiscard]] static String roman_number_from(size_t value);

    template<class SeparatorType, class CollectionType>
    [[nodiscard]] static String join(const SeparatorType& separator, const CollectionType& collection)
    {
        StringBuilder builder;
        builder.join(separator, collection);
        return builder.build();
    }

    [[nodiscard]] bool matches(StringView mask, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;
    [[nodiscard]] bool matches(StringView mask, Vector<MaskSpan>&, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;

    template<typename T = int>
    [[nodiscard]] Optional<T> to_int(TrimWhitespace = TrimWhitespace::Yes) const;
    template<typename T = unsigned>
    [[nodiscard]] Optional<T> to_uint(TrimWhitespace = TrimWhitespace::Yes) const;

    [[nodiscard]] String to_lowercase() const;
    [[nodiscard]] String to_uppercase() const;
    [[nodiscard]] String to_snakecase() const;
    [[nodiscard]] String to_titlecase() const;

    [[nodiscard]] bool is_whitespace() const { return StringUtils::is_whitespace(*this); }

#ifndef KERNEL
    [[nodiscard]] String trim(StringView characters, TrimMode mode = TrimMode::Both) const
    {
        return StringUtils::trim(view(), characters, mode);
    }

    [[nodiscard]] String trim_whitespace(TrimMode mode = TrimMode::Both) const
    {
        return StringUtils::trim_whitespace(view(), mode);
    }
#endif

    [[nodiscard]] bool equals_ignoring_case(StringView) const;

    [[nodiscard]] bool contains(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool contains(char, CaseSensitivity = CaseSensitivity::CaseSensitive) const;

    [[nodiscard]] Vector<String> split_limit(char separator, size_t limit, bool keep_empty = false) const;
    [[nodiscard]] Vector<String> split(char separator, bool keep_empty = false) const;
    [[nodiscard]] Vector<StringView> split_view(char separator, bool keep_empty = false) const;

    [[nodiscard]] Optional<size_t> find(char needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find(StringView needle, size_t start = 0) const { return StringUtils::find(*this, needle, start); }
    [[nodiscard]] Optional<size_t> find_last(char needle) const { return StringUtils::find_last(*this, needle); }
    // FIXME: Implement find_last(StringView) for API symmetry.
    Vector<size_t> find_all(StringView needle) const;
    using SearchDirection = StringUtils::SearchDirection;
    [[nodiscard]] Optional<size_t> find_any_of(StringView needles, SearchDirection direction) const { return StringUtils::find_any_of(*this, needles, direction); }

    [[nodiscard]] String substring(size_t start, size_t length) const;
    [[nodiscard]] String substring(size_t start) const;
    [[nodiscard]] StringView substring_view(size_t start, size_t length) const;
    [[nodiscard]] StringView substring_view(size_t start) const;

    [[nodiscard]] bool is_null() const { return !m_impl; }
    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return length() == 0; }
    [[nodiscard]] ALWAYS_INLINE size_t length() const { return m_impl ? m_impl->length() : 0; }
    // Includes NUL-terminator, if non-nullptr.
    [[nodiscard]] ALWAYS_INLINE const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }

    [[nodiscard]] bool copy_characters_to_buffer(char* buffer, size_t buffer_size) const;

    [[nodiscard]] ALWAYS_INLINE ReadonlyBytes bytes() const
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }

    [[nodiscard]] ALWAYS_INLINE const char& operator[](size_t i) const
    {
        VERIFY(!is_null());
        return (*m_impl)[i];
    }

    using ConstIterator = SimpleIterator<const String, const char>;

    [[nodiscard]] constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    [[nodiscard]] constexpr ConstIterator end() const { return ConstIterator::end(*this); }

    [[nodiscard]] bool starts_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool ends_with(StringView, CaseSensitivity = CaseSensitivity::CaseSensitive) const;
    [[nodiscard]] bool starts_with(char) const;
    [[nodiscard]] bool ends_with(char) const;

    bool operator==(const String&) const;
    bool operator!=(const String& other) const { return !(*this == other); }

    bool operator==(StringView) const;
    bool operator!=(StringView other) const { return !(*this == other); }

    bool operator==(const FlyString&) const;
    bool operator!=(const FlyString& other) const { return !(*this == other); }

    bool operator<(const String&) const;
    bool operator<(const char*) const;
    bool operator>=(const String& other) const { return !(*this < other); }
    bool operator>=(const char* other) const { return !(*this < other); }

    bool operator>(const String&) const;
    bool operator>(const char*) const;
    bool operator<=(const String& other) const { return !(*this > other); }
    bool operator<=(const char* other) const { return !(*this > other); }

    bool operator==(const char* cstring) const;
    bool operator!=(const char* cstring) const { return !(*this == cstring); }

    [[nodiscard]] String isolated_copy() const;

    [[nodiscard]] static String empty()
    {
        return StringImpl::the_empty_stringimpl();
    }

    [[nodiscard]] StringImpl* impl() { return m_impl.ptr(); }
    [[nodiscard]] const StringImpl* impl() const { return m_impl.ptr(); }

    String& operator=(String&& other)
    {
        if (this != &other)
            m_impl = move(other.m_impl);
        return *this;
    }

    String& operator=(const String& other)
    {
        if (this != &other)
            m_impl = const_cast<String&>(other).m_impl;
        return *this;
    }

    String& operator=(std::nullptr_t)
    {
        m_impl = nullptr;
        return *this;
    }

    String& operator=(ReadonlyBytes bytes)
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
    [[nodiscard]] static String copy(const BufferType& buffer, ShouldChomp should_chomp = NoChomp)
    {
        if (buffer.is_empty())
            return empty();
        return String((const char*)buffer.data(), buffer.size(), should_chomp);
    }

    [[nodiscard]] static String vformatted(StringView fmtstr, TypeErasedFormatParams&);

    template<typename... Parameters>
    [[nodiscard]] static String formatted(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
    {
        VariadicFormatParams variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    template<typename T>
    [[nodiscard]] static String number(T value) requires IsArithmetic<T>
    {
        return formatted("{}", value);
    }

    [[nodiscard]] StringView view() const
    {
        return { characters(), length() };
    }

    [[nodiscard]] String replace(StringView needle, StringView replacement, bool all_occurrences = false) const { return StringUtils::replace(*this, needle, replacement, all_occurrences); }
    [[nodiscard]] size_t count(StringView needle) const { return StringUtils::count(*this, needle); }
    [[nodiscard]] String reverse() const;

    template<typename... Ts>
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_one_of(Ts&&... strings) const
    {
        return (... || this->operator==(forward<Ts>(strings)));
    }

private:
    RefPtr<StringImpl> m_impl;
};

template<>
struct Traits<String> : public GenericTraits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.impl()->hash() : 0; }
};

struct CaseInsensitiveStringTraits : public Traits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.to_lowercase().impl()->hash() : 0; }
    static bool equals(const String& a, const String& b) { return a.to_lowercase() == b.to_lowercase(); }
};

bool operator<(const char*, const String&);
bool operator>=(const char*, const String&);
bool operator>(const char*, const String&);
bool operator<=(const char*, const String&);

String escape_html_entities(StringView html);

InputStream& operator>>(InputStream& stream, String& string);

}

using AK::CaseInsensitiveStringTraits;
using AK::escape_html_entities;
using AK::String;
