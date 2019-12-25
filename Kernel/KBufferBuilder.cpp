#include <AK/PrintfImplementation.h>
#include <AK/StdLibExtras.h>
#include <KBufferBuilder.h>
#include <stdarg.h>

inline bool KBufferBuilder::can_append(size_t size) const
{
    bool has_space = ((m_size + size) < m_buffer.size());
    ASSERT(has_space);
    return has_space;
}

KBuffer KBufferBuilder::build()
{
    m_buffer.set_size(m_size);
    return m_buffer;
}

KBufferBuilder::KBufferBuilder()
    : m_buffer(KBuffer::create_with_size(4 * MB, Region::Access::Read | Region::Access::Write))
{
}

void KBufferBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    if (!can_append(str.length()))
        return;
    memcpy(insertion_ptr(), str.characters_without_null_termination(), str.length());
    m_size += str.length();
}

void KBufferBuilder::append(const char* characters, int length)
{
    if (!length)
        return;
    if (!can_append(length))
        return;
    memcpy(insertion_ptr() + m_size, characters, length);
    m_size += length;
}

void KBufferBuilder::append(char ch)
{
    if (!can_append(1))
        return;
    insertion_ptr()[0] = ch;
    m_size += 1;
}

void KBufferBuilder::appendvf(const char* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

void KBufferBuilder::appendf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    appendvf(fmt, ap);
    va_end(ap);
}
