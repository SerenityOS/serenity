/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/FlyString.h>
#include <AK/HashTable.h>
#include <AK/Memory.h>
#include <AK/StdLibExtras.h>
#include <AK/StringHash.h>
#include <AK/StringImpl.h>
#include <AK/kmalloc.h>

namespace AK {

static StringImpl* s_the_empty_stringimpl = nullptr;

StringImpl& StringImpl::the_empty_stringimpl()
{
    if (!s_the_empty_stringimpl) {
        void* slot = kmalloc(sizeof(StringImpl) + sizeof(char));
        s_the_empty_stringimpl = new (slot) StringImpl(ConstructTheEmptyStringImpl);
    }
    return *s_the_empty_stringimpl;
}

StringImpl::StringImpl(ConstructWithInlineBufferTag, size_t length)
    : m_length(length)
{
}

StringImpl::~StringImpl()
{
    if (m_fly)
        FlyString::did_destroy_impl({}, *this);
}

NonnullRefPtr<StringImpl> StringImpl::create_uninitialized(size_t length, char*& buffer)
{
    VERIFY(length);
    void* slot = kmalloc(allocation_size_for_stringimpl(length));
    VERIFY(slot);
    auto new_stringimpl = adopt_ref(*new (slot) StringImpl(ConstructWithInlineBuffer, length));
    buffer = const_cast<char*>(new_stringimpl->characters());
    buffer[length] = '\0';
    return new_stringimpl;
}

RefPtr<StringImpl> StringImpl::create(const char* cstring, size_t length, ShouldChomp should_chomp)
{
    if (!cstring)
        return nullptr;

    if (should_chomp) {
        while (length) {
            char last_ch = cstring[length - 1];
            if (!last_ch || last_ch == '\n' || last_ch == '\r')
                --length;
            else
                break;
        }
    }

    if (!length)
        return the_empty_stringimpl();

    char* buffer;
    auto new_stringimpl = create_uninitialized(length, buffer);
    memcpy(buffer, cstring, length * sizeof(char));

    return new_stringimpl;
}

RefPtr<StringImpl> StringImpl::create(const char* cstring, ShouldChomp shouldChomp)
{
    if (!cstring)
        return nullptr;

    return create(cstring, strlen(cstring), shouldChomp);
}

RefPtr<StringImpl> StringImpl::create(ReadonlyBytes bytes, ShouldChomp shouldChomp)
{
    return StringImpl::create(reinterpret_cast<const char*>(bytes.data()), bytes.size(), shouldChomp);
}

RefPtr<StringImpl> StringImpl::create_lowercased(char const* cstring, size_t length)
{
    if (!cstring)
        return nullptr;
    if (!length)
        return the_empty_stringimpl();
    char* buffer;
    auto impl = create_uninitialized(length, buffer);
    for (size_t i = 0; i < length; ++i)
        buffer[i] = (char)to_ascii_lowercase(cstring[i]);
    return impl;
}

RefPtr<StringImpl> StringImpl::create_uppercased(char const* cstring, size_t length)
{
    if (!cstring)
        return nullptr;
    if (!length)
        return the_empty_stringimpl();
    char* buffer;
    auto impl = create_uninitialized(length, buffer);
    for (size_t i = 0; i < length; ++i)
        buffer[i] = (char)to_ascii_uppercase(cstring[i]);
    return impl;
}

NonnullRefPtr<StringImpl> StringImpl::to_lowercase() const
{
    for (size_t i = 0; i < m_length; ++i) {
        if (is_ascii_upper_alpha(characters()[i]))
            return create_lowercased(characters(), m_length).release_nonnull();
    }
    return const_cast<StringImpl&>(*this);
}

NonnullRefPtr<StringImpl> StringImpl::to_uppercase() const
{
    for (size_t i = 0; i < m_length; ++i) {
        if (is_ascii_lower_alpha(characters()[i]))
            return create_uppercased(characters(), m_length).release_nonnull();
    }
    return const_cast<StringImpl&>(*this);
}

void StringImpl::compute_hash() const
{
    if (!length())
        m_hash = 0;
    else
        m_hash = string_hash(characters(), m_length);
    m_has_hash = true;
}

}
