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

#include <AK/FlyString.h>
#include <AK/HashTable.h>
#include <AK/Memory.h>
#include <AK/StdLibExtras.h>
#include <AK/StringImpl.h>
#include <AK/kmalloc.h>

//#define DEBUG_STRINGIMPL

#ifdef DEBUG_STRINGIMPL
unsigned g_stringimpl_count;
static HashTable<StringImpl*>* g_all_live_stringimpls;

void dump_all_stringimpls();
void dump_all_stringimpls()
{
    unsigned i = 0;
    for (auto& it : *g_all_live_stringimpls) {
        dbgln("{}: \"{}\"", i, *it);
        ++i;
    }
}
#endif

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
#ifdef DEBUG_STRINGIMPL
    if (!g_all_live_stringimpls)
        g_all_live_stringimpls = new HashTable<StringImpl*>;
    ++g_stringimpl_count;
    g_all_live_stringimpls->set(this);
#endif
}

StringImpl::~StringImpl()
{
    if (m_fly)
        FlyString::did_destroy_impl({}, *this);
#ifdef DEBUG_STRINGIMPL
    --g_stringimpl_count;
    g_all_live_stringimpls->remove(this);
#endif
}

static inline size_t allocation_size_for_stringimpl(size_t length)
{
    return sizeof(StringImpl) + (sizeof(char) * length) + sizeof(char);
}

NonnullRefPtr<StringImpl> StringImpl::create_uninitialized(size_t length, char*& buffer)
{
    ASSERT(length);
    void* slot = kmalloc(allocation_size_for_stringimpl(length));
    ASSERT(slot);
    auto new_stringimpl = adopt(*new (slot) StringImpl(ConstructWithInlineBuffer, length));
    buffer = const_cast<char*>(new_stringimpl->characters());
    buffer[length] = '\0';
    return new_stringimpl;
}

RefPtr<StringImpl> StringImpl::create(const char* cstring, size_t length, ShouldChomp should_chomp)
{
    if (!cstring)
        return nullptr;

    if (!length || !*cstring)
        return the_empty_stringimpl();

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

static inline bool is_ascii_lowercase(char c)
{
    return c >= 'a' && c <= 'z';
}

static inline bool is_ascii_uppercase(char c)
{
    return c >= 'A' && c <= 'Z';
}

static inline char to_ascii_lowercase(char c)
{
    if (is_ascii_uppercase(c))
        return c | 0x20;
    return c;
}

static inline char to_ascii_uppercase(char c)
{
    if (is_ascii_lowercase(c))
        return c & ~0x20;
    return c;
}

NonnullRefPtr<StringImpl> StringImpl::to_lowercase() const
{
    for (size_t i = 0; i < m_length; ++i) {
        if (!is_ascii_lowercase(characters()[i]))
            goto slow_path;
    }
    return const_cast<StringImpl&>(*this);

slow_path:
    char* buffer;
    auto lowercased = create_uninitialized(m_length, buffer);
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = to_ascii_lowercase(characters()[i]);
    return lowercased;
}

NonnullRefPtr<StringImpl> StringImpl::to_uppercase() const
{
    for (size_t i = 0; i < m_length; ++i) {
        if (!is_ascii_uppercase(characters()[i]))
            goto slow_path;
    }
    return const_cast<StringImpl&>(*this);

slow_path:
    char* buffer;
    auto uppercased = create_uninitialized(m_length, buffer);
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = to_ascii_uppercase(characters()[i]);
    return uppercased;
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
