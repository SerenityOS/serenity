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
#include <LibCrypto/Hash/HashFunction.h>

namespace Crypto {
namespace Hash {

namespace SHA1Constants {

constexpr static u32 InitializationHashes[5] { 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };

constexpr static u32 RoundConstants[4] {
    0X5a827999,
    0X6ed9eba1,
    0X8f1bbcdc,
    0Xca62c1d6,
};

}

template<size_t Bytes>
struct SHA1Digest {
    u8 data[Bytes];
    constexpr static size_t Size = Bytes;

    const u8* immutable_data() const { return data; }
    size_t data_length() { return Bytes; }
};

class SHA1 final : public HashFunction<512, SHA1Digest<160 / 8>> {
public:
    SHA1()
    {
        reset();
    }

    virtual void update(const u8*, size_t) override;

    virtual void update(const ByteBuffer& buffer) override { update(buffer.data(), buffer.size()); };
    virtual void update(const StringView& string) override { update((const u8*)string.characters_without_null_termination(), string.length()); };

    virtual DigestType digest() override;
    virtual DigestType peek() override;

    inline static DigestType hash(const u8* data, size_t length)
    {
        SHA1 sha;
        sha.update(data, length);
        return sha.digest();
    }

    inline static DigestType hash(const ByteBuffer& buffer) { return hash(buffer.data(), buffer.size()); }
    inline static DigestType hash(const StringView& buffer) { return hash((const u8*)buffer.characters_without_null_termination(), buffer.length()); }

    virtual String class_name() const override
    {
        return "SHA1";
    };
    inline virtual void reset() override
    {
        m_data_length = 0;
        m_bit_length = 0;
        for (auto i = 0; i < 5; ++i)
            m_state[i] = SHA1Constants::InitializationHashes[i];
    }

private:
    inline void transform(const u8*);

    u8 m_data_buffer[BlockSize];
    size_t m_data_length { 0 };

    u64 m_bit_length { 0 };
    u32 m_state[5];

    constexpr static auto FinalBlockDataSize = BlockSize - 8;
    constexpr static auto Rounds = 80;
};

}
}
