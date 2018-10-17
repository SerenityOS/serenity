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
        : m_impl(const_cast<String&>(other).m_impl.copyRef())
    {
    }

    String(String&& other)
        : m_impl(move(other.m_impl))
    {
    }

    String(const char* cstring)
        : m_impl(StringImpl::create(cstring))
    {
    }

    String(const char* cstring, size_t length)
        : m_impl(StringImpl::create(cstring, length))
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

    String toLowercase() const
    {
        if (!m_impl)
            return String();
        return m_impl->toLowercase();
    }

    String toUppercase() const
    {
        if (!m_impl)
            return String();
        return m_impl->toUppercase();
    }

    Vector<String> split(char separator) const;
    String substring(size_t start, size_t length) const;

    bool isEmpty() const { return length() == 0; }
    unsigned length() const { return m_impl ? m_impl->length() : 0; }
    const char* characters() const { return m_impl ? m_impl->characters() : nullptr; }
    char operator[](unsigned i) const { ASSERT(m_impl); return (*m_impl)[i]; }

    bool operator==(const String&) const;
    bool operator!=(const String& other) const { return !(*this == other); }

    static String empty();

    StringImpl* impl() { return m_impl.ptr(); }
    const StringImpl* impl() const { return m_impl.ptr(); }

    String& operator=(String&& other)
    {
        if (this != &other) {
            m_impl = move(other.m_impl);
        }
        return *this;
    }

    ByteBuffer toByteBuffer() const;

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
