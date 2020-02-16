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

#include <AK/PrintfImplementation.h>
#include <AK/StdLibExtras.h>
#include <KBufferBuilder.h>
#include <stdarg.h>

namespace Kernel {

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

}
