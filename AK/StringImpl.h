#pragma once

#include "Retainable.h"
#include "RetainPtr.h"
#include "Types.h"

namespace AK {

enum ShouldChomp { NoChomp, Chomp };

class StringImpl : public Retainable<StringImpl> {
public:
    static Retained<StringImpl> create_uninitialized(size_t length, char*& buffer);
    static RetainPtr<StringImpl> create(const char* cstring, ShouldChomp = NoChomp);
    static RetainPtr<StringImpl> create(const char* cstring, size_t length, ShouldChomp = NoChomp);
    Retained<StringImpl> to_lowercase() const;
    Retained<StringImpl> to_uppercase() const;

    static StringImpl& the_empty_stringimpl();

    ~StringImpl();

    size_t length() const { return m_length; }
    const char* characters() const { return m_characters; }
    char operator[](size_t i) const { ASSERT(i < m_length); return m_characters[i]; }

    unsigned hash() const
    {
        if (!m_hasHash)
            compute_hash();
        return m_hash;
    }

private:
    enum ConstructTheEmptyStringImplTag { ConstructTheEmptyStringImpl };
    explicit StringImpl(ConstructTheEmptyStringImplTag) : m_characters("") { }

    enum ConstructWithInlineBufferTag { ConstructWithInlineBuffer };
    StringImpl(ConstructWithInlineBufferTag, size_t length);

    void compute_hash() const;

    size_t m_length { 0 };
    mutable bool m_hasHash { false };
    const char* m_characters { nullptr };
    mutable unsigned m_hash { 0 };
    char m_inline_buffer[0];
};

}

using AK::StringImpl;
using AK::Chomp;
