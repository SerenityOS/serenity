/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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
#include <Kernel/Tracing/SimpleBufferBuilder.h>

SimpleBufferBuilder::SimpleBufferBuilder(u8* buffer, size_t buffer_size)
    : m_buffer(buffer)
    , m_buffer_size(buffer_size)
{
}

u8* SimpleBufferBuilder::try_to_append(size_t size)
{
    if (!m_overflown && m_nwritten + size <= m_buffer_size) {
        u8* ptr = m_buffer + m_nwritten;
        m_nwritten += size;
        return ptr;
    } else {
        m_overflown = true;
        return nullptr;
    }
}

void SimpleBufferBuilder::append(const StringView& s)
{
    u8* ptr = try_to_append(s.length());
    if (ptr)
        memcpy(ptr, s.characters_without_null_termination(), s.length());
}

void SimpleBufferBuilder::append(char c)
{
    u8* ptr = try_to_append(1);
    if (ptr)
        *ptr = c;
}

void SimpleBufferBuilder::append(const char* characters, int length)
{
    u8* ptr = try_to_append(length);
    if (ptr)
        memcpy(ptr, characters, length);
}

void SimpleBufferBuilder::appendf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    appendvf(fmt, ap);
    va_end(ap);
}

void SimpleBufferBuilder::appendvf(const char* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}
