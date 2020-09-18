/*
 * Copyright (c) 2020, the SerenityOS developers
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

#include <AK/LogStream.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/ChecksumFunction.h>

namespace Crypto::Checksum {

struct Table {
    u32 data[256];

    constexpr Table()
        : data()
    {
        for (auto i = 0; i < 256; i++) {
            u32 value = i;

            for (auto j = 0; j < 8; j++) {
                if (value & 1) {
                    value = 0xEDB88320 ^ (value >> 1);
                } else {
                    value = value >> 1;
                }
            }

            data[i] = value;
        }
    }

    constexpr u32 operator[](int index) const
    {
        return data[index];
    }
};

constexpr static auto table = Table();

class CRC32 : public ChecksumFunction<u32> {
public:
    CRC32() { }
    CRC32(ReadonlyBytes data)
    {
        update(data);
    }

    CRC32(u32 initial_state, ReadonlyBytes data)
        : m_state(initial_state)
    {
        update(data);
    }

    void update(ReadonlyBytes data);
    u32 digest();

private:
    u32 m_state { ~0u };
};

}
