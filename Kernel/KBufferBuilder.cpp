/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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
    return make<KBuffer>(move(m_buffer));
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
