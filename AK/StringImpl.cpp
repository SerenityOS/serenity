#include "StringImpl.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <new> 
#include "kmalloc.h"

namespace AK {

StringImpl& StringImpl::theEmptyStringImpl()
{
    static StringImpl* s = new StringImpl(ConstructTheEmptyStringImpl);
    return *s;
}

StringImpl::~StringImpl()
{
}

static inline size_t allocationSizeForStringImpl(size_t length)
{
    return sizeof(StringImpl) + (sizeof(char) * length) + sizeof(char);
}

RetainPtr<StringImpl> StringImpl::createUninitialized(size_t length, char*& buffer)
{
    if (!length)
        return theEmptyStringImpl();

    void* slot = kmalloc(allocationSizeForStringImpl(length));
    if (!slot)
        return nullptr;

    auto newStringImpl = adopt(*new (slot) StringImpl(ConstructWithInlineBuffer, length));
    buffer = const_cast<char*>(newStringImpl->m_characters);
    buffer[length] = '\0';
    return newStringImpl;
}

RetainPtr<StringImpl> StringImpl::create(const char* cstring, size_t length)
{
    if (!cstring)
        return nullptr;

    if (!*cstring)
        return theEmptyStringImpl();

    char* buffer;
    auto newStringImpl = createUninitialized(length, buffer);
    memcpy(buffer, cstring, length * sizeof(char));

    return newStringImpl;
}

RetainPtr<StringImpl> StringImpl::create(const char* cstring)
{
    if (!cstring)
        return nullptr;

    return create(cstring, strlen(cstring));
}

static inline bool isASCIILowercase(char c)
{
    return c >= 'a' && c <= 'z';
}

static inline bool isASCIIUppercase(char c)
{
    return c >= 'A' && c <= 'Z';
}

static inline char toASCIILowercase(char c)
{
    if (isASCIIUppercase(c))
        return c | 0x20;
    return c;
}

static inline char toASCIIUppercase(char c)
{
    if (isASCIILowercase(c))
        return c & ~0x20;
    return c;
}

RetainPtr<StringImpl> StringImpl::toLowercase() const
{
    if (!m_length)
        return const_cast<StringImpl*>(this);

    for (size_t i = 0; i < m_length; ++i) {
        if (!isASCIILowercase(m_characters[i]))
            goto slowPath;
    }
    return const_cast<StringImpl*>(this);

slowPath:
    char* buffer;
    auto lowercased = createUninitialized(m_length, buffer);
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = toASCIILowercase(m_characters[i]);

    return lowercased;
}

RetainPtr<StringImpl> StringImpl::toUppercase() const
{
    if (!m_length)
        return const_cast<StringImpl*>(this);

    for (size_t i = 0; i < m_length; ++i) {
        if (!isASCIIUppercase(m_characters[i]))
            goto slowPath;
    }
    return const_cast<StringImpl*>(this);

slowPath:
    char* buffer;
    auto uppercased = createUninitialized(m_length, buffer);
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = toASCIIUppercase(m_characters[i]);

    return uppercased;
}

void StringImpl::computeHash() const
{
    if (!length()) {
        m_hash = 0;
    } else {
        unsigned hash = 0;
        for (size_t i = 0; i < m_length; ++i) {
            hash += m_characters[i];
            hash += (hash << 10);
            hash ^= (hash >> 6);
        }
        hash += hash << 3;
        hash ^= hash >> 11;
        hash += hash << 15;
        m_hash = hash;
    }
    m_hasHash = true;
}

}

