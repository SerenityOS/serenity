/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/BufferStream.h>
#include <LibIPC/Decoder.h>

namespace IPC {

bool Decoder::decode(bool& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(u8& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(u16& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(u32& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(u64& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(i8& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(i16& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(i32& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(i64& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(float& value)
{
    m_stream >> value;
    return !m_stream.handle_read_failure();
}

bool Decoder::decode(String& value)
{
    i32 length = 0;
    m_stream >> length;
    if (m_stream.handle_read_failure())
        return false;
    if (length < 0) {
        value = {};
        return true;
    }
    if (length == 0) {
        value = String::empty();
        return true;
    }
    char* text_buffer = nullptr;
    auto text_impl = StringImpl::create_uninitialized(static_cast<size_t>(length), text_buffer);
    for (size_t i = 0; i < static_cast<size_t>(length); ++i) {
        m_stream >> text_buffer[i];
    }
    value = *text_impl;
    return !m_stream.handle_read_failure();
}

}
