/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <Kernel/StdLib.h>
#include <LibCrypto/Cipher/Cipher.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Hash/SHA2.h>

namespace Kernel {

template<typename CipherT, typename HashT, int KeySize>
class FortunaPRNG {
public:
    constexpr static size_t pool_count = 32;
    constexpr static size_t reseed_threshold = 16;

    using CipherType = CipherT;
    using BlockType = CipherT::BlockType;
    using HashType = HashT;
    using DigestType = HashT::DigestType;

    void get_random_bytes(u8* buffer, size_t n)
    {
        if (m_p0_len >= reseed_threshold) {
            this->reseed();
        }

        ASSERT(m_counter != 0);

        // FIXME: More than 2^20 bytes cannot be generated without refreshing the key.
        ASSERT(n < (1 << 20));

        CipherType cipher(m_key, KeySize);

        size_t block_size = CipherType::BlockSizeInBits / 8;

        for (size_t i = 0; i < n; i += block_size) {
            this->generate_block(cipher, &buffer[i], min(block_size, n - i));
        }

        // Extract a new key from the prng stream.

        for (size_t i = 0; i < KeySize/8; i += block_size) {
            this->generate_block(cipher, &(m_key[i]), min(block_size, KeySize - i));
        }

    }

    template<typename T>
    void add_random_event(const T& event_data, size_t pool)
    {
        pool %= pool_count;
        if (pool == 0) {
            m_p0_len++;
        }
        m_pools[pool].update(reinterpret_cast<const u8*>(&event_data), sizeof(T));
    }

private:
    void generate_block(CipherType cipher, u8* buffer, size_t size)
    {

        BlockType input((u8*)&m_counter, sizeof(m_counter));
        BlockType output;
        cipher.encrypt_block(input, output);
        m_counter++;

        memcpy(buffer, output.get().data(), size);
    }

    void reseed()
    {
        HashType new_key;
        new_key.update(m_key);
        for (size_t i = 0; i < pool_count; ++i) {
            if (m_reseed_number % (1 << i) == 0) {
                DigestType digest = m_pools[i].digest();
                new_key.update(digest.immutable_data(), digest.data_length());
            }
        }
        DigestType digest = new_key.digest();
        m_key = ByteBuffer::copy(digest.immutable_data(),
            digest.data_length());

        m_counter++;
        m_reseed_number++;
        m_p0_len = 0;
    }

    size_t m_counter { 0 };
    size_t m_reseed_number { 0 };
    size_t m_p0_len { 0 };
    ByteBuffer m_key;
    HashType m_pools[pool_count];
};

class KernelRng : public FortunaPRNG<Crypto::Cipher::AESCipher, Crypto::Hash::SHA256, 256> {
    AK_MAKE_ETERNAL;
public:
    static KernelRng& the();

private:
    KernelRng();

};

// NOTE: These API's are primarily about expressing intent/needs in the calling code.
//       We don't make any guarantees about actual fastness or goodness yet.

void get_fast_random_bytes(u8*, size_t);
void get_good_random_bytes(u8*, size_t);

template<typename T>
inline T get_fast_random()
{
    T value;
    get_fast_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

template<typename T>
inline T get_good_random()
{
    T value;
    get_good_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

}
