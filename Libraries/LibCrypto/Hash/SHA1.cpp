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

#include <AK/Endian.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/SHA1.h>

namespace Crypto {
namespace Hash {

inline static constexpr auto ROTATE_LEFT(u32 value, size_t bits)
{
    return (value << bits) | (value >> (32 - bits));
}

inline void SHA1::transform(const u8* data)
{
    u32 blocks[80];
    for (size_t i = 0; i < 16; ++i)
        blocks[i] = AK::convert_between_host_and_network_endian(((const u32*)data)[i]);

    // w[i] = (w[i-3] xor w[i-8] xor w[i-14] xor w[i-16]) leftrotate 1
    for (size_t i = 16; i < Rounds; ++i)
        blocks[i] = ROTATE_LEFT(blocks[i - 3] ^ blocks[i - 8] ^ blocks[i - 14] ^ blocks[i - 16], 1);

    auto a = m_state[0], b = m_state[1], c = m_state[2], d = m_state[3], e = m_state[4];
    u32 f, k;

    for (size_t i = 0; i < Rounds; ++i) {
        if (i <= 19) {
            f = (b & c) | ((~b) & d);
            k = SHA1Constants::RoundConstants[0];
        } else if (i <= 39) {
            f = b ^ c ^ d;
            k = SHA1Constants::RoundConstants[1];
        } else if (i <= 59) {
            f = (b & c) | (b & d) | (c & d);
            k = SHA1Constants::RoundConstants[2];
        } else {
            f = b ^ c ^ d;
            k = SHA1Constants::RoundConstants[3];
        }
        auto temp = ROTATE_LEFT(a, 5) + f + e + k + blocks[i];
        e = d;
        d = c;
        c = ROTATE_LEFT(b, 30);
        b = a;
        a = temp;
    }

    m_state[0] += a;
    m_state[1] += b;
    m_state[2] += c;
    m_state[3] += d;
    m_state[4] += e;

    // "security" measures, as if SHA1 is secure
    a = 0;
    b = 0;
    c = 0;
    d = 0;
    e = 0;
    __builtin_memset(blocks, 0, 16 * sizeof(u32));
}

void SHA1::update(const u8* message, size_t length)
{
    for (size_t i = 0; i < length; ++i) {
        if (m_data_length == BlockSize) {
            transform(m_data_buffer);
            m_bit_length += 512;
            m_data_length = 0;
        }
        m_data_buffer[m_data_length++] = message[i];
    }
}

SHA1::DigestType SHA1::digest()
{
    auto digest = peek();
    reset();
    return digest;
}

SHA1::DigestType SHA1::peek()
{
    DigestType digest;
    size_t i = m_data_length;

    // make a local copy of the data as we modify it
    u8 data[BlockSize];
    u32 state[5];
    __builtin_memcpy(data, m_data_buffer, m_data_length);
    __builtin_memcpy(state, m_state, 20);

    if (BlockSize == m_data_length) {
        transform(m_data_buffer);
        m_bit_length += BlockSize * 8;
        m_data_length = 0;
        i = 0;
    }

    if (m_data_length < FinalBlockDataSize) {
        m_data_buffer[i++] = 0x80;
        while (i < FinalBlockDataSize)
            m_data_buffer[i++] = 0x00;

    } else {
        // First, complete a block with some padding.
        m_data_buffer[i++] = 0x80;
        while (i < BlockSize)
            m_data_buffer[i++] = 0x00;
        transform(m_data_buffer);

        // Then start another block with BlockSize - 8 bytes of zeros
        __builtin_memset(m_data_buffer, 0, FinalBlockDataSize);
    }

    // append total message length
    m_bit_length += m_data_length * 8;
    m_data_buffer[BlockSize - 1] = m_bit_length;
    m_data_buffer[BlockSize - 2] = m_bit_length >> 8;
    m_data_buffer[BlockSize - 3] = m_bit_length >> 16;
    m_data_buffer[BlockSize - 4] = m_bit_length >> 24;
    m_data_buffer[BlockSize - 5] = m_bit_length >> 32;
    m_data_buffer[BlockSize - 6] = m_bit_length >> 40;
    m_data_buffer[BlockSize - 7] = m_bit_length >> 48;
    m_data_buffer[BlockSize - 8] = m_bit_length >> 56;

    transform(m_data_buffer);

    for (size_t i = 0; i < 4; ++i) {
        digest.data[i + 0] = (m_state[0] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 4] = (m_state[1] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 8] = (m_state[2] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 12] = (m_state[3] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 16] = (m_state[4] >> (24 - i * 8)) & 0x000000ff;
    }
    // restore the data
    __builtin_memcpy(m_data_buffer, data, m_data_length);
    __builtin_memcpy(m_state, state, 20);
    return digest;
}

}
}
