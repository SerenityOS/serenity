#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefPtr.h>
#include <AK/StringImpl.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Vector.h>
#include <AK/kstdio.h>

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
    ~String() {}

    String() {}

    String(const StringView& view)
    {
        if (view.m_impl)
            m_impl = *view.m_impl;
        else
            m_impl = StringImpl::create(view.characters_without_null_termination(), view.length());
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

    enum class CaseSensitivity {
        CaseInsensitive,
        CaseSensitive,
    };

    static String repeated(char, size_t count);
    bool matches(const StringView& pattern, CaseSensitivity = CaseSensitivity::CaseInsensitive) const;

    // FIXME: These should be shared between String and StringView somehow!
    int to_int(bool& ok) const;
    unsigned to_uint(bool& ok) const;

    String to_lowercase() const
    {
        if (!m_impl)
            return String();
        return m_impl->to_lowercase();
    }

    String to_uppercase() const
    {
        if (!m_impl)
            return String();
        return m_impl->to_uppercase();
    }

    bool equals_ignoring_case(const StringView&) const;

    bool contains(const String&) const;

    Vector<String> split_limit(char separator, size_t limit) const;
    Vector<String> split(char separator) const;
    String substring(size_t start, size_t length) const;

    Vector<StringView> split_view(char separator, bool keep_empty = false) const;
    StringView substring_view(size_t start, size_t length) const;

    bool is_null() const { return !m_impl; }
    bool is_empty() const { return length() == 0; }
    size_t length() const { return m_impl ? m_impl->length() : 0; }
    const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    char operator[](size_t i) const
    {
        ASSERT(m_impl);
        return (*m_impl)[i];
    }

    bool starts_with(const StringView&) const;
    bool ends_with(const StringView&) const;

    bool operator==(const String&) const;
    bool operator!=(const String& other) const { return !(*this == other); }

    bool operator==(const StringView&) const;
    bool operator!=(const StringView& other) const { return !(*this == other); }

    bool operator<(const String&) const;
    bool operator<(const char*) const;
    bool operator>=(const String& other) const { return !(*this < other); }
    bool operator>=(const char* other) const { return !(*this < other); }

    bool operator>(const String&) const;
    bool operator>(const char*) const;
    bool operator<=(const String& other) const { return !(*this > other); }
    bool operator<=(const char* other) const { return !(*this > other); }

    bool operator==(const char* cstring) const
    {
        if (is_null())
            return !cstring;
        if (!cstring)
            return false;
        return !strcmp(characters(), cstring);
    }

    bool operator!=(const char* cstring) const
    {
        return !(*this == cstring);
    }

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
    static String number(u64);
    static String number(u32);
    static String number(i32);

#ifdef __serenity__
    static String number(size_t n)
    {
        return number((u32)n);
    }
#endif

    StringView view() const
    {
        return { characters(), length() };
    }

private:
    bool match_helper(const StringView& mask) const;
    RefPtr<StringImpl> m_impl;
};

inline bool StringView::operator==(const String& string) const
{
    if (string.is_null())
        return !m_characters;
    if (!m_characters)
        return false;
    if (m_length != string.length())
        return false;
    if (m_characters == string.characters())
        return true;
    return !memcmp(m_characters, string.characters(), m_length);
}

template<>
struct Traits<String> : public GenericTraits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.impl()->hash() : 0; }
    static void dump(const String& s) { kprintf("%s", s.characters()); }
};

struct CaseInsensitiveStringTraits : public AK::Traits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.to_lowercase().impl()->hash() : 0; }
    static bool equals(const String& a, const String& b) { return a.to_lowercase() == b.to_lowercase(); }
};

inline bool operator<(const char* characters, const String& string)
{
    if (!characters)
        return !string.is_null();

    if (string.is_null())
        return false;

    return strcmp(characters, string.characters()) < 0;
}

inline bool operator>=(const char* characters, const String& string)
{
    return !(characters < string);
}

inline bool operator>(const char* characters, const String& string)
{
    if (!characters)
        return !string.is_null();

    if (string.is_null())
        return false;

    return strcmp(characters, string.characters()) > 0;
}

inline bool operator<=(const char* characters, const String& string)
{
    return !(characters > string);
}

}

using AK::CaseInsensitiveStringTraits;
using AK::String;
