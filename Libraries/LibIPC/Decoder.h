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

#pragma once

#include <AK/Forward.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
inline bool decode(Decoder&, T&)
{
    static_assert(DependentFalse<T>, "Base IPC::decoder() instantiated");
    ASSERT_NOT_REACHED();
}

class Decoder {
public:
    explicit Decoder(InputMemoryStream& stream)
        : m_stream(stream)
    {
    }

    bool decode(bool&);
    bool decode(u8&);
    bool decode(u16&);
    bool decode(u32&);
    bool decode(u64&);
    bool decode(i8&);
    bool decode(i16&);
    bool decode(i32&);
    bool decode(i64&);
    bool decode(float&);
    bool decode(String&);
    bool decode(URL&);
    bool decode(Dictionary&);

    template<typename T>
    bool decode(T& value)
    {
        return IPC::decode(*this, value);
    }

    template<typename T>
    bool decode(Vector<T>& vector)
    {
        u64 size;
        if (!decode(size) || size > NumericLimits<i32>::max())
            return false;
        for (size_t i = 0; i < size; ++i) {
            T value;
            if (!decode(value))
                return false;
            vector.append(move(value));
        }
        return true;
    }

    template<typename T>
    bool decode(Optional<T>& optional)
    {
        bool has_value;
        if (!decode(has_value))
            return false;
        if (!has_value) {
            optional = {};
            return true;
        }
        T value;
        if (!decode(value))
            return false;
        optional = move(value);
        return true;
    }

private:
    InputMemoryStream& m_stream;
};

}
