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

#include <AK/Forward.h>
#include <AK/RefPtr.h>
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
//     s = String::format("%d little piggies", m_piggies);
//
//     StringBuilder builder;
//     builder.append("abc");
//     builder.append("123");
//     s = builder.to_string();

class String {
public:
    using ConstIterator = const char*;

    ~String() {}

    String() {}
    String(const StringView&);

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

    static String repeated(char, size_t count);
    bool matches(const StringView& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;

    int to_int(bool& ok) const;
    unsigned to_uint(bool& ok) const;

    String to_lowercase() const;
    String to_uppercase() const;

    enum class TrimMode {
        Left,
        Right,
        Both
    };
    String trim_whitespace(TrimMode mode = TrimMode::Both) const;

    bool equals_ignoring_case(const StringView&) const;

    bool contains(const String&) const;
    Optional<size_t> index_of(const String&) const;

    Vector<String> split_limit(char separator, size_t limit, bool keep_empty = false) const;
    Vector<String> split(char separator, bool keep_empty = false) const;
    String substring(size_t start, size_t length) const;

    Vector<StringView> split_view(char separator, bool keep_empty = false) const;
    StringView substring_view(size_t start, size_t length) const;

    bool is_null() const { return !m_impl; }
    bool is_empty() const { return length() == 0; }
    size_t length() const { return m_impl ? m_impl->length() : 0; }
    const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    const char& operator[](size_t i) const
    {
        return (*m_impl)[i];
    }

    ConstIterator begin() const { return characters(); }
    ConstIterator end() const { return begin() + length(); }

    bool starts_with(const StringView&) const;
    bool ends_with(const StringView&) const;
    bool starts_with(char) const;
    bool ends_with(char) const;

    bool operator==(const String&) const;
    bool operator!=(const String& other) const { return !(*this == other); }

    bool operator==(const StringView&) const;
    bool operator!=(const StringView& other) const { return !(*this == other); }

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

    String isolated_copy() const;

    static String empty();

    StringImpl* impl() { return m_impl.ptr(); }
    const StringImpl* impl() const { return m_impl.ptr(); }

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

    u32 hash() const
    {
        if (!m_impl)
            return 0;
        return m_impl->hash();
    }

    ByteBuffer to_byte_buffer() const;

    template<typename BufferType>
    static String copy(const BufferType& buffer, ShouldChomp should_chomp = NoChomp)
    {
        if (buffer.is_null())
            return {};
        if (buffer.is_empty())
            return empty();
        return String((const char*)buffer.data(), buffer.size(), should_chomp);
    }

    static String format(const char*, ...);
    static String number(unsigned);
    static String number(unsigned long);
    static String number(unsigned long long);
    static String number(int);
    static String number(long);
    static String number(long long);
    static String number(double, int scale = 10);

    StringView view() const;

    int replace(const String& needle, const String& replacement, bool all_occurences = false);

private:
    RefPtr<StringImpl> m_impl;
};

template<>
struct Traits<String> : public GenericTraits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.impl()->hash() : 0; }
};

struct CaseInsensitiveStringTraits : public AK::Traits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.to_lowercase().impl()->hash() : 0; }
    static bool equals(const String& a, const String& b) { return a.to_lowercase() == b.to_lowercase(); }
};

bool operator<(const char*, const String&);
bool operator>=(const char*, const String&);
bool operator>(const char*, const String&);
bool operator<=(const char*, const String&);

String escape_html_entities(const StringView& html);

}

using AK::CaseInsensitiveStringTraits;
using AK::escape_html_entities;
using AK::String;
