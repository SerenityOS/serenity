#include "StringImpl.h"
#include "StdLibExtras.h"
#include "kmalloc.h"
#include "HashTable.h"

#ifdef DEBUG_STRINGIMPL
unsigned g_stringimpl_count;
static HashTable<StringImpl*>* g_all_live_stringimpls;

void dump_all_stringimpls()
{
    unsigned i = 0;
    for (auto& it : *g_all_live_stringimpls) {
        dbgprintf("%u: \"%s\"\n", i, (*it).characters());
        ++i;
    }
}
#endif

namespace AK {

static StringImpl* s_the_empty_stringimpl = nullptr;

void StringImpl::initialize_globals()
{
    s_the_empty_stringimpl = nullptr;
#ifdef DEBUG_STRINGIMPL
    g_stringimpl_count = 0;
    g_all_live_stringimpls = new HashTable<StringImpl*>;
#endif
}

StringImpl& StringImpl::the_empty_stringimpl()
{
    if (!s_the_empty_stringimpl)
        s_the_empty_stringimpl = new StringImpl(ConstructTheEmptyStringImpl);;
    return *s_the_empty_stringimpl;
}

StringImpl::StringImpl(ConstructWithInlineBufferTag, size_t length)
    : m_length(length)
    , m_characters(m_inline_buffer)
{
#ifdef DEBUG_STRINGIMPL
    ++g_stringimpl_count;
    g_all_live_stringimpls->set(this);
#endif
}

StringImpl::~StringImpl()
{
#ifdef DEBUG_STRINGIMPL
    --g_stringimpl_count;
    g_all_live_stringimpls->remove(this);
#endif
}

static inline size_t allocation_size_for_stringimpl(size_t length)
{
    return sizeof(StringImpl) + (sizeof(char) * length) + sizeof(char);
}

RetainPtr<StringImpl> StringImpl::create_uninitialized(size_t length, char*& buffer)
{
    ASSERT(length);
    void* slot = kmalloc(allocation_size_for_stringimpl(length));
    if (!slot)
        return nullptr;

    auto new_stringimpl = adopt(*new (slot) StringImpl(ConstructWithInlineBuffer, length));
    buffer = const_cast<char*>(new_stringimpl->m_characters);
    buffer[length] = '\0';
    return new_stringimpl;
}

RetainPtr<StringImpl> StringImpl::create(const char* cstring, size_t length, ShouldChomp shouldChomp)
{
    if (!cstring)
        return nullptr;

    if (!*cstring)
        return the_empty_stringimpl();

    if (!length)
        return the_empty_stringimpl();

    char* buffer;
    auto new_stringimpl = create_uninitialized(length, buffer);
    if (!new_stringimpl)
        return nullptr;
    memcpy(buffer, cstring, length * sizeof(char));

    if (shouldChomp && buffer[length - 1] == '\n') {
        buffer[length - 1] = '\0';
        --new_stringimpl->m_length;
    }

    return new_stringimpl;
}

RetainPtr<StringImpl> StringImpl::create(const char* cstring, ShouldChomp shouldChomp)
{
    if (!cstring)
        return nullptr;

    return create(cstring, strlen(cstring), shouldChomp);
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

RetainPtr<StringImpl> StringImpl::to_lowercase() const
{
    if (!m_length)
        return const_cast<StringImpl*>(this);

    for (size_t i = 0; i < m_length; ++i) {
        if (!is_ascii_lowercase(m_characters[i]))
            goto slow_path;
    }
    return const_cast<StringImpl*>(this);

slow_path:
    char* buffer;
    auto lowercased = create_uninitialized(m_length, buffer);
    if (!lowercased)
        return nullptr;
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = to_ascii_lowercase(m_characters[i]);

    return lowercased;
}

RetainPtr<StringImpl> StringImpl::to_uppercase() const
{
    if (!m_length)
        return const_cast<StringImpl*>(this);

    for (size_t i = 0; i < m_length; ++i) {
        if (!is_ascii_uppercase(m_characters[i]))
            goto slow_path;
    }
    return const_cast<StringImpl*>(this);

slow_path:
    char* buffer;
    auto uppercased = create_uninitialized(m_length, buffer);
    if (!uppercased)
        return nullptr;
    for (size_t i = 0; i < m_length; ++i)
        buffer[i] = to_ascii_uppercase(m_characters[i]);

    return uppercased;
}

void StringImpl::compute_hash() const
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

