/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Kernel/Library/KBufferBuilder.h>

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
    auto rounded_new_buffer_size_or_error = Memory::page_round_up(new_buffer_size + 1 * MiB);
    if (rounded_new_buffer_size_or_error.is_error()) {
        return false;
    }
    auto new_buffer_or_error = KBuffer::try_create_with_size("KBufferBuilder"sv, rounded_new_buffer_size_or_error.value());
    if (new_buffer_or_error.is_error())
        return false;
    auto new_buffer = new_buffer_or_error.release_value();
    memcpy(new_buffer->data(), m_buffer->data(), m_buffer->size());
    m_buffer = move(new_buffer);
    return true;
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

    return move(m_buffer);
}

ErrorOr<KBufferBuilder> KBufferBuilder::try_create()
{
    auto buffer = TRY(KBuffer::try_create_with_size("KBufferBuilder"sv, 4 * MiB, Memory::Region::Access::ReadWrite));
    return KBufferBuilder { move(buffer) };
}

KBufferBuilder::KBufferBuilder(NonnullOwnPtr<KBuffer> buffer)
    : m_buffer(move(buffer))
{
}

ErrorOr<void> KBufferBuilder::append_bytes(ReadonlyBytes bytes)
{
    if (!check_expand(bytes.size()))
        return ENOMEM;
    memcpy(insertion_ptr(), bytes.data(), bytes.size());
    m_size += bytes.size();
    return {};
}

ErrorOr<void> KBufferBuilder::append(StringView str)
{
    if (str.is_empty())
        return {};
    if (!check_expand(str.length()))
        return ENOMEM;
    memcpy(insertion_ptr(), str.characters_without_null_termination(), str.length());
    m_size += str.length();
    return {};
}

ErrorOr<void> KBufferBuilder::append(char const* characters, int length)
{
    if (!length)
        return {};
    if (!check_expand(length))
        return ENOMEM;
    memcpy(insertion_ptr(), characters, length);
    m_size += length;
    return {};
}

ErrorOr<void> KBufferBuilder::append(char ch)
{
    if (!check_expand(1))
        return ENOMEM;
    insertion_ptr()[0] = ch;
    m_size += 1;
    return {};
}

ErrorOr<void> KBufferBuilder::append_escaped_for_json(StringView string)
{
    for (auto ch : string) {
        switch (ch) {
        case '\b':
            TRY(append("\\b"sv));
            break;
        case '\n':
            TRY(append("\\n"sv));
            break;
        case '\t':
            TRY(append("\\t"sv));
            break;
        case '\"':
            TRY(append("\\\""sv));
            break;
        case '\\':
            TRY(append("\\\\"sv));
            break;
        default:
            if (ch >= 0 && ch <= 0x1f)
                TRY(appendff("\\u{:04x}", ch));
            else
                TRY(append(ch));
        }
    }
    return {};
}

}
