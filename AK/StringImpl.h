/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

enum ShouldChomp {
    NoChomp,
    Chomp
};

size_t allocation_size_for_stringimpl(size_t length);

class StringImpl : public RefCounted<StringImpl> {
public:
    static NonnullRefPtr<StringImpl> create_uninitialized(size_t length, char*& buffer);
    static RefPtr<StringImpl> create(const char* cstring, ShouldChomp = NoChomp);
    static RefPtr<StringImpl> create(const char* cstring, size_t length, ShouldChomp = NoChomp);
    static RefPtr<StringImpl> create(ReadonlyBytes, ShouldChomp = NoChomp);
    static RefPtr<StringImpl> create_lowercased(char const* cstring, size_t length);
    static RefPtr<StringImpl> create_uppercased(char const* cstring, size_t length);

    NonnullRefPtr<StringImpl> to_lowercase() const;
    NonnullRefPtr<StringImpl> to_uppercase() const;

    void operator delete(void* ptr)
    {
        kfree_sized(ptr, allocation_size_for_stringimpl(static_cast<StringImpl*>(ptr)->m_length));
    }

    static StringImpl& the_empty_stringimpl();

    ~StringImpl();

    size_t length() const { return m_length; }
    // Includes NUL-terminator.
    const char* characters() const { return &m_inline_buffer[0]; }

    ALWAYS_INLINE ReadonlyBytes bytes() const { return { characters(), length() }; }
    ALWAYS_INLINE StringView view() const { return { characters(), length() }; }

    const char& operator[](size_t i) const
    {
        VERIFY(i < m_length);
        return characters()[i];
    }

    bool operator==(const StringImpl& other) const
    {
        if (length() != other.length())
            return false;
        return !__builtin_memcmp(characters(), other.characters(), length());
    }

    unsigned hash() const
    {
        if (!m_has_hash)
            compute_hash();
        return m_hash;
    }

    unsigned existing_hash() const
    {
        return m_hash;
    }

    bool is_fly() const { return m_fly; }
    void set_fly(Badge<FlyString>, bool fly) const { m_fly = fly; }

private:
    enum ConstructTheEmptyStringImplTag {
        ConstructTheEmptyStringImpl
    };
    explicit StringImpl(ConstructTheEmptyStringImplTag)
        : m_fly(true)
    {
        m_inline_buffer[0] = '\0';
    }

    enum ConstructWithInlineBufferTag {
        ConstructWithInlineBuffer
    };
    StringImpl(ConstructWithInlineBufferTag, size_t length);

    void compute_hash() const;

    size_t m_length { 0 };
    mutable unsigned m_hash { 0 };
    mutable bool m_has_hash { false };
    mutable bool m_fly { false };
    char m_inline_buffer[0];
};

inline size_t allocation_size_for_stringimpl(size_t length)
{
    return sizeof(StringImpl) + (sizeof(char) * length) + sizeof(char);
}

template<>
struct Formatter<StringImpl> : Formatter<StringView> {
    void format(FormatBuilder& builder, const StringImpl& value)
    {
        Formatter<StringView>::format(builder, { value.characters(), value.length() });
    }
};

}

using AK::Chomp;
using AK::NoChomp;
using AK::StringImpl;
