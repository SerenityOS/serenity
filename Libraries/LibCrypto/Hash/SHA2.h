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
#include <AK/StringBuilder.h>
#include <LibCrypto/Hash/HashFunction.h>

namespace Crypto {
namespace Hash {

    namespace SHA256Constants {
        constexpr static u32 RoundConstants[64] {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
        };

        constexpr static u32 InitializationHashes[8] = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };
    }

    template <size_t Bytes>
    struct SHA2Digest {
        u8 data[Bytes];
    };

    // FIXME: I want template<size_t BlockSize> but the compiler gets confused
    class SHA256 final : public HashFunction<512, SHA2Digest<256 / 8>> {
    public:
        SHA256()
        {
            reset();
        }

        virtual void update(const u8*, size_t) override;

        virtual void update(const ByteBuffer& buffer) override { update(buffer.data(), buffer.size()); };
        virtual void update(const StringView& string) override { update((const u8*)string.characters_without_null_termination(), string.length()); };

        virtual DigestType digest() override;

        inline static DigestType hash(const u8* data, size_t length)
        {
            SHA256 sha;
            sha.update(data, length);
            return sha.digest();
        }

        inline static DigestType hash(const ByteBuffer& buffer) { return hash(buffer.data(), buffer.size()); }
        inline static DigestType hash(const StringView& buffer) { return hash((const u8*)buffer.characters_without_null_termination(), buffer.length()); }

        virtual String class_name() const override
        {
            StringBuilder builder;
            builder.append("SHA");
            builder.appendf("%zu", this->DigestSize * 8);
            return builder.build();
        };

    private:
        inline void transform(const u8*);
        inline void reset()
        {
            m_data_length = 0;
            m_bit_length = 0;
            for (size_t i = 0; i < 8; ++i)
                m_state[i] = SHA256Constants::InitializationHashes[i];
        }

        u8 m_data_buffer[BlockSize];
        size_t m_data_length { 0 };

        u64 m_bit_length { 0 };
        u32 m_state[8];

        constexpr static auto FinalBlockDataSize = BlockSize - 8;
    };

}
}
