#pragma once

#include "ByteBuffer.h"
#include "RetainPtr.h"
#include "StringImpl.h"
#include "Traits.h"
#include "Vector.h"
#include "kstdio.h"

namespace AK {

class String {
public:
    ~String() { }

    String() { }
    String(const String& other)
        : m_impl(const_cast<String&>(other).m_impl.copy_ref())
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

    String(const char* cstring, ssize_t length, ShouldChomp shouldChomp = NoChomp)
        : m_impl(StringImpl::create(cstring, length, shouldChomp))
    {
    }

    String(const StringImpl& impl)
        : m_impl(const_cast<StringImpl&>(impl))
    {
    }

    String(RetainPtr<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    String(Retained<StringImpl>&& impl)
        : m_impl(move(impl))
    {
    }

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

    Vector<String> split(char separator) const;
    String substring(ssize_t start, ssize_t length) const;

    bool is_null() const { return !m_impl; }
    bool is_empty() const { return length() == 0; }
    ssize_t length() const { return m_impl ? m_impl->length() : 0; }
    const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    char operator[](ssize_t i) const { ASSERT(m_impl); return (*m_impl)[i]; }

    bool ends_with(const String&) const;

    bool operator==(const String&) const;
    bool operator!=(const String& other) const { return !(*this == other); }
    bool operator<(const String&) const;

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
            m_impl = const_cast<String&>(other).m_impl.copy_ref();
        return *this;
    }

    ByteBuffer to_byte_buffer() const;
    static String from_byte_buffer(const ByteBuffer&, ShouldChomp = NoChomp);

    static String format(const char*, ...);

private:
    RetainPtr<StringImpl> m_impl;
};

template<>
struct Traits<String> {
    static unsigned hash(const String& s) { return s.impl() ? s.impl()->hash() : 0; }
    static void dump(const String& s) { kprintf("%s", s.characters()); }
};

}

using AK::String;
