/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <AK/String.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/HashFunction.h>

namespace Crypto {
namespace Authentication {

void galois_multiply(u32 (&z)[4], const u32 (&x)[4], const u32 (&y)[4]);

struct GHashDigest {
    constexpr static size_t Size = 16;
    u8 data[Size];

    const u8* immutable_data() const { return data; }
    size_t data_length() { return Size; }
};

class GHash final {
public:
    using TagType = GHashDigest;

    template<size_t N>
    explicit GHash(const char (&key)[N])
        : GHash({ key, N })
    {
    }

    explicit GHash(const ReadonlyBytes& key)
    {
        for (size_t i = 0; i < 16; i += 4)
            m_key[i / 4] = AK::convert_between_host_and_big_endian(*(const u32*)(key.offset(i)));
    }

    constexpr static size_t digest_size() { return TagType::Size; }

    String class_name() const { return "GHash"; }

    TagType process(ReadonlyBytes aad, ReadonlyBytes cipher);

private:
    inline void transform(ReadonlyBytes, ReadonlyBytes);

    u32 m_key[4];
};

}

}
