#pragma once

#include "Retainable.h"
#include "RetainPtr.h"

namespace AK {

class StringImpl : public Retainable<StringImpl> {
public:
    static RetainPtr<StringImpl> createUninitialized(unsigned length, char*& buffer);
    static RetainPtr<StringImpl> create(const char* cstring);
    static RetainPtr<StringImpl> create(const char* cstring, size_t length);
    RetainPtr<StringImpl> toLowercase() const;
    RetainPtr<StringImpl> toUppercase() const;

    static StringImpl& theEmptyStringImpl();

    ~StringImpl();

    unsigned length() const { return m_length; }
    const char* characters() const { return m_characters; }
    char operator[](unsigned i) const { ASSERT(i < m_length); return m_characters[i]; }

    unsigned hash() const
    {
        if (!m_hasHash)
            computeHash();
        return m_hash;
    }

private:
    enum ConstructTheEmptyStringImplTag { ConstructTheEmptyStringImpl };
    explicit StringImpl(ConstructTheEmptyStringImplTag) : m_characters("") { }

    enum ConstructWithInlineBufferTag { ConstructWithInlineBuffer };
    explicit StringImpl(ConstructWithInlineBufferTag, unsigned length) : m_length(length), m_characters(m_inlineBuffer) { }

    void computeHash() const;

    unsigned m_length { 0 };
    bool m_ownsBuffer { true };
    mutable bool m_hasHash { false };
    const char* m_characters { nullptr };
    mutable unsigned m_hash { 0 };
    char m_inlineBuffer[0];
};

}

using AK::StringImpl;
