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

#pragma once

#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
bool encode(Encoder&, T&)
{
    static_assert(DependentFalse<T>, "Base IPC::encode() was instantiated");
    ASSERT_NOT_REACHED();
}

class Encoder {
public:
    explicit Encoder(MessageBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    Encoder& operator<<(bool);
    Encoder& operator<<(u8);
    Encoder& operator<<(u16);
    Encoder& operator<<(u32);
    Encoder& operator<<(u64);
    Encoder& operator<<(i8);
    Encoder& operator<<(i16);
    Encoder& operator<<(i32);
    Encoder& operator<<(i64);
    Encoder& operator<<(float);
    Encoder& operator<<(const char*);
    Encoder& operator<<(const StringView&);
    Encoder& operator<<(const String&);
    Encoder& operator<<(const URL&);
    Encoder& operator<<(const Dictionary&);

    template<typename T>
    Encoder& operator<<(const Vector<T>& vector)
    {
        *this << (u64)vector.size();
        for (auto& value : vector)
            *this << value;
        return *this;
    }

    template<typename T>
    Encoder& operator<<(const T& value)
    {
        encode(value);
        return *this;
    }

    template<typename T>
    Encoder& operator<<(const Optional<T>& optional)
    {
        *this << optional.has_value();
        if (optional.has_value())
            *this << optional.value();
        return *this;
    }

    template<typename T>
    void encode(const T& value)
    {
        IPC::encode(*this, value);
    }

private:
    MessageBuffer& m_buffer;
};

}
