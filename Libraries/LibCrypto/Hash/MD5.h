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
namespace Hash {

struct MD5Digest {
    constexpr static size_t Size = 16;
    u8 data[Size];

    const u8* immutable_data() const { return data; }
    size_t data_length() { return Size; }
};

namespace MD5Constants {

constexpr u32 init_A = 0x67452301;
constexpr u32 init_B = 0xefcdab89;
constexpr u32 init_C = 0x98badcfe;
constexpr u32 init_D = 0x10325476;
constexpr u32 S11 = 7;
constexpr u32 S12 = 12;
constexpr u32 S13 = 17;
constexpr u32 S14 = 22;
constexpr u32 S21 = 5;
constexpr u32 S22 = 9;
constexpr u32 S23 = 14;
constexpr u32 S24 = 20;
constexpr u32 S31 = 4;
constexpr u32 S32 = 11;
constexpr u32 S33 = 16;
constexpr u32 S34 = 23;
constexpr u32 S41 = 6;
constexpr u32 S42 = 10;
constexpr u32 S43 = 15;
constexpr u32 S44 = 21;
constexpr u8 PADDING[] = {
    0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0
};

}

class MD5 final : public HashFunction<512, MD5Digest> {
public:
    MD5()
    {
        m_buffer = ByteBuffer::wrap(m_data_buffer, sizeof(m_data_buffer));
    }

    virtual void update(const u8*, size_t) override;
    virtual void update(const ByteBuffer& buffer) override { update(buffer.data(), buffer.size()); };
    virtual void update(const StringView& string) override { update((const u8*)string.characters_without_null_termination(), string.length()); };
    virtual DigestType digest() override;
    virtual DigestType peek() override;

    virtual String class_name() const override { return "MD5"; }

    inline static DigestType hash(const u8* data, size_t length)
    {
        MD5 md5;
        md5.update(data, length);
        return md5.digest();
    }

    inline static DigestType hash(const ByteBuffer& buffer) { return hash(buffer.data(), buffer.size()); }
    inline static DigestType hash(const StringView& buffer) { return hash((const u8*)buffer.characters_without_null_termination(), buffer.length()); }
    inline virtual void reset() override
    {
        m_A = MD5Constants::init_A;
        m_B = MD5Constants::init_B;
        m_C = MD5Constants::init_C;
        m_D = MD5Constants::init_D;

        m_count[0] = 0;
        m_count[1] = 0;

        __builtin_memset(m_data_buffer, 0, sizeof(m_data_buffer));
    }

private:
    inline void transform(const u8*);

    static void encode(const u32* from, u8* to, size_t length);
    static void decode(const u8* from, u32* to, size_t length);

    u32 m_A { MD5Constants::init_A }, m_B { MD5Constants::init_B }, m_C { MD5Constants::init_C }, m_D { MD5Constants::init_D };
    u32 m_count[2] { 0, 0 };
    ByteBuffer m_buffer;

    u8 m_data_buffer[64];
};

}

}
