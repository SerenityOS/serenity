/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/KBufferBuilder.h>

namespace Kernel {

inline bool KBufferBuilder::check_expand(size_t size)
{
    if (!m_buffer)
        return false;
    if ((m_size + size) < m_buffer->capacity())
        return true;
    if (Checked<size_t>::addition_would_overflow(m_size, size))
        return false;
    size_t new_buffer_size = m_size + size;
    if (Checked<size_t>::addition_would_overflow(new_buffer_size, 1 * MiB))
        return false;
    new_buffer_size = Memory::page_round_up(new_buffer_size + 1 * MiB);
    return m_buffer->expand(new_buffer_size);
}

bool KBufferBuilder::flush()
{
    if (!m_buffer)
        return false;
    m_buffer->set_size(m_size);
    return true;
}

OwnPtr<KBuffer> KBufferBuilder::build()
{
    if (!flush())
        return {};

    return try_make<KBuffer>(move(m_buffer));
}

KBufferBuilder::KBufferBuilder()
    : m_buffer(KBufferImpl::try_create_with_size(4 * MiB, Memory::Region::Access::ReadWrite))
{
}

KResult KBufferBuilder::append_bytes(ReadonlyBytes bytes)
{
    if (!check_expand(bytes.size()))
        return ENOMEM;
    memcpy(insertion_ptr(), bytes.data(), bytes.size());
    m_size += bytes.size();
    return KSuccess;
}

KResult KBufferBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return KSuccess;
    if (!check_expand(str.length()))
        return ENOMEM;
    memcpy(insertion_ptr(), str.characters_without_null_termination(), str.length());
    m_size += str.length();
    return KSuccess;
}

KResult KBufferBuilder::append(const char* characters, int length)
{
    if (!length)
        return KSuccess;
    if (!check_expand(length))
        return ENOMEM;
    memcpy(insertion_ptr(), characters, length);
    m_size += length;
    return KSuccess;
}

KResult KBufferBuilder::append(char ch)
{
    if (!check_expand(1))
        return ENOMEM;
    insertion_ptr()[0] = ch;
    m_size += 1;
    return KSuccess;
}

KResult KBufferBuilder::append_escaped_for_json(const StringView& string)
{
    for (auto ch : string) {
        switch (ch) {
        case '\e':
            TRY(append("\\u001B"));
            break;
        case '\b':
            TRY(append("\\b"));
            break;
        case '\n':
            TRY(append("\\n"));
            break;
        case '\t':
            TRY(append("\\t"));
            break;
        case '\"':
            TRY(append("\\\""));
            break;
        case '\\':
            TRY(append("\\\\"));
            break;
        default:
            TRY(append(ch));
        }
    }
    return KSuccess;
}

}
