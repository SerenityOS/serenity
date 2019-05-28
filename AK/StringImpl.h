#pragma once

#include "RetainPtr.h"
#include "Retainable.h"
#include "Types.h"

namespace AK {

enum ShouldChomp {
    NoChomp,
    Chomp
};

class StringImpl : public Retainable<StringImpl> {
public:
    static Retained<StringImpl> create_uninitialized(ssize_t length, char*& buffer);
    static RetainPtr<StringImpl> create(const char* cstring, ShouldChomp = NoChomp);
    static RetainPtr<StringImpl> create(const char* cstring, ssize_t length, ShouldChomp = NoChomp);
    Retained<StringImpl> to_lowercase() const;
    Retained<StringImpl> to_uppercase() const;

    static StringImpl& the_empty_stringimpl();

    ~StringImpl();

    ssize_t length() const { return m_length; }
    const char* characters() const { return m_characters; }
    char operator[](ssize_t i) const
    {
        ASSERT(i >= 0 && i < m_length);
        return m_characters[i];
    }

    unsigned hash() const
    {
        if (!m_hasHash)
            compute_hash();
        return m_hash;
    }

private:
    enum ConstructTheEmptyStringImplTag {
        ConstructTheEmptyStringImpl
    };
    explicit StringImpl(ConstructTheEmptyStringImplTag)
        : m_characters("")
    {
    }

    enum ConstructWithInlineBufferTag {
        ConstructWithInlineBuffer
    };
    StringImpl(ConstructWithInlineBufferTag, ssize_t length);

    void compute_hash() const;

    ssize_t m_length { 0 };
    mutable bool m_hasHash { false };
    const char* m_characters { nullptr };
    mutable unsigned m_hash { 0 };
    char m_inline_buffer[0];
};

inline dword string_hash(const char* characters, int length)
{
    dword hash = 0;
    for (int i = 0; i < length; ++i) {
        hash += (dword)characters[i];
        hash += (hash << 10);
        hash ^= (hash >> 6);
    }
    hash += hash << 3;
    hash ^= hash >> 11;
    hash += hash << 15;
    return hash;
}

}

using AK::Chomp;
using AK::string_hash;
using AK::StringImpl;
