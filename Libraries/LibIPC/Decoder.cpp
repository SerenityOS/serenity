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

#include <AK/MemoryStream.h>
#include <AK/URL.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>

namespace IPC {

bool Decoder::decode(bool& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u8& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u16& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u32& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u64& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i8& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i16& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i32& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i64& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(float& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(String& value)
{
    i32 length = 0;
    m_stream >> length;
    if (m_stream.handle_any_error())
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
    m_stream >> Bytes { text_buffer, static_cast<size_t>(length) };
    value = *text_impl;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(URL& value)
{
    String string;
    if (!decode(string))
        return false;
    value = URL(string);
    return true;
}

bool Decoder::decode(Dictionary& dictionary)
{
    u64 size = 0;
    m_stream >> size;
    if (m_stream.handle_any_error())
        return false;
    if (size >= (size_t)NumericLimits<i32>::max()) {
        ASSERT_NOT_REACHED();
    }

    for (size_t i = 0; i < size; ++i) {
        String key;
        if (!decode(key))
            return false;
        String value;
        if (!decode(value))
            return false;
        dictionary.add(move(key), move(value));
    }

    return true;
}

}
