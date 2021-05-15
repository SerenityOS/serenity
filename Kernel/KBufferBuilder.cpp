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
    if (!m_can_expand)
        return false;
    if (Checked<size_t>::addition_would_overflow(m_size, size))
        return false;
    size_t new_buffer_size = m_size + size;
    if (Checked<size_t>::addition_would_overflow(new_buffer_size, 1 * MiB))
        return false;
    new_buffer_size = page_round_up(new_buffer_size + 1 * MiB);
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

    return adopt_own_if_nonnull(new KBuffer(move(m_buffer)));
}

KBufferBuilder::KBufferBuilder(bool can_expand)
    : m_buffer(KBufferImpl::try_create_with_size(4 * MiB, Region::Access::Read | Region::Access::Write))
    , m_can_expand(can_expand)
{
}

KBufferBuilder::KBufferBuilder(RefPtr<KBufferImpl>& buffer, bool can_expand)
    : m_buffer(buffer)
    , m_can_expand(can_expand)
{
    if (!m_buffer)
        m_buffer = buffer = KBufferImpl::try_create_with_size(4 * MiB, Region::Access::Read | Region::Access::Write);
}

void KBufferBuilder::append_bytes(ReadonlyBytes bytes)
{
    if (!check_expand(bytes.size()))
        return;
    memcpy(insertion_ptr(), bytes.data(), bytes.size());
    m_size += bytes.size();
}

void KBufferBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    if (!check_expand(str.length()))
        return;
    memcpy(insertion_ptr(), str.characters_without_null_termination(), str.length());
    m_size += str.length();
}

void KBufferBuilder::append(const char* characters, int length)
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

void KBufferBuilder::append_escaped_for_json(const StringView& string)
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
