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

void KBufferBuilder::append_bytes(ReadonlyBytes bytes)
{
    if (!check_expand(bytes.size()))
        return;
    memcpy(insertion_ptr(), bytes.data(), bytes.size());
    m_size += bytes.size();
}

void KBufferBuilder::append(StringView const& str)
{
    if (str.is_empty())
        return;
    if (!check_expand(str.length()))
        return;
    memcpy(insertion_ptr(), str.characters_without_null_termination(), str.length());
    m_size += str.length();
}

void KBufferBuilder::append(char const* characters, int length)
{
    if (!length)
        return;
    if (!check_expand(length))
        return;
    memcpy(insertion_ptr(), characters, length);
    m_size += length;
}

void KBufferBuilder::append(char ch)
{
    if (!check_expand(1))
        return;
    insertion_ptr()[0] = ch;
    m_size += 1;
}

void KBufferBuilder::append_escaped_for_json(StringView const& string)
{
    for (auto ch : string) {
        switch (ch) {
        case '\e':
            append("\\u001B");
            break;
        case '\b':
            append("\\b");
            break;
        case '\n':
            append("\\n");
            break;
        case '\t':
            append("\\t");
            break;
        case '\"':
            append("\\\"");
            break;
        case '\\':
            append("\\\\");
            break;
        default:
            append(ch);
        }
    }
}

}
