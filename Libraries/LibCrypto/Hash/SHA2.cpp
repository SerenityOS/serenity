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

#include <AK/Types.h>
#include <LibCrypto/Hash/SHA2.h>

namespace Crypto {
namespace Hash {
constexpr inline static auto ROTRIGHT(u32 a, size_t b) { return (a >> b) | (a << (32 - b)); }
constexpr inline static auto CH(u32 x, u32 y, u32 z) { return (x & y) ^ (z & ~x); }
constexpr inline static auto MAJ(u32 x, u32 y, u32 z) { return (x & y) ^ (x & z) ^ (y & z); }
constexpr inline static auto EP0(u32 x) { return ROTRIGHT(x, 2) ^ ROTRIGHT(x, 13) ^ ROTRIGHT(x, 22); }
constexpr inline static auto EP1(u32 x) { return ROTRIGHT(x, 6) ^ ROTRIGHT(x, 11) ^ ROTRIGHT(x, 25); }
constexpr inline static auto SIGN0(u32 x) { return ROTRIGHT(x, 7) ^ ROTRIGHT(x, 18) ^ (x >> 3); }
constexpr inline static auto SIGN1(u32 x) { return ROTRIGHT(x, 17) ^ ROTRIGHT(x, 19) ^ (x >> 10); }

constexpr inline static auto ROTRIGHT(u64 a, size_t b) { return (a >> b) | (a << (64 - b)); }
constexpr inline static auto CH(u64 x, u64 y, u64 z) { return (x & y) ^ (z & ~x); }
constexpr inline static auto MAJ(u64 x, u64 y, u64 z) { return (x & y) ^ (x & z) ^ (y & z); }
constexpr inline static auto EP0(u64 x) { return ROTRIGHT(x, 28) ^ ROTRIGHT(x, 34) ^ ROTRIGHT(x, 39); }
constexpr inline static auto EP1(u64 x) { return ROTRIGHT(x, 14) ^ ROTRIGHT(x, 18) ^ ROTRIGHT(x, 41); }
constexpr inline static auto SIGN0(u64 x) { return ROTRIGHT(x, 1) ^ ROTRIGHT(x, 8) ^ (x >> 7); }
constexpr inline static auto SIGN1(u64 x) { return ROTRIGHT(x, 19) ^ ROTRIGHT(x, 61) ^ (x >> 6); }

inline void SHA256::transform(const u8* data)
{
    u32 m[64];

    size_t i = 0;
    for (size_t j = 0; i < 16; ++i, j += 4) {
        m[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | data[j + 3];
    }

    for (; i < BlockSize; ++i) {
        m[i] = SIGN1(m[i - 2]) + m[i - 7] + SIGN0(m[i - 15]) + m[i - 16];
    }

    auto a = m_state[0], b = m_state[1],
         c = m_state[2], d = m_state[3],
         e = m_state[4], f = m_state[5],
         g = m_state[6], h = m_state[7];

    for (size_t i = 0; i < Rounds; ++i) {
        auto temp0 = h + EP1(e) + CH(e, f, g) + SHA256Constants::RoundConstants[i] + m[i];
        auto temp1 = EP0(a) + MAJ(a, b, c);
        h = g;
        g = f;
        f = e;
        e = d + temp0;
        d = c;
        c = b;
        b = a;
        a = temp0 + temp1;
    }

    m_state[0] += a;
    m_state[1] += b;
    m_state[2] += c;
    m_state[3] += d;
    m_state[4] += e;
    m_state[5] += f;
    m_state[6] += g;
    m_state[7] += h;
}

void SHA256::update(const u8* message, size_t length)
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

SHA256::DigestType SHA256::digest()
{
    auto digest = peek();
    reset();
    return digest;
}

SHA256::DigestType SHA256::peek()
{
    DigestType digest;
    size_t i = m_data_length;

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

    // SHA uses big-endian and we assume little-endian
    // FIXME: looks like a thing for AK::NetworkOrdered,
    //        but he doesn't support shifting operations
    for (size_t i = 0; i < 4; ++i) {
        digest.data[i + 0] = (m_state[0] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 4] = (m_state[1] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 8] = (m_state[2] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 12] = (m_state[3] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 16] = (m_state[4] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 20] = (m_state[5] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 24] = (m_state[6] >> (24 - i * 8)) & 0x000000ff;
        digest.data[i + 28] = (m_state[7] >> (24 - i * 8)) & 0x000000ff;
    }
    return digest;
}

inline void SHA512::transform(const u8* data)
{
    u64 m[80];

    size_t i = 0;
    for (size_t j = 0; i < 16; ++i, j += 8) {
        m[i] = ((u64)data[j] << 56) | ((u64)data[j + 1] << 48) | ((u64)data[j + 2] << 40) | ((u64)data[j + 3] << 32) | ((u64)data[j + 4] << 24) | ((u64)data[j + 5] << 16) | ((u64)data[j + 6] << 8) | (u64)data[j + 7];
    }

    for (; i < Rounds; ++i) {
        m[i] = SIGN1(m[i - 2]) + m[i - 7] + SIGN0(m[i - 15]) + m[i - 16];
    }

    auto a = m_state[0], b = m_state[1],
         c = m_state[2], d = m_state[3],
         e = m_state[4], f = m_state[5],
         g = m_state[6], h = m_state[7];

    for (size_t i = 0; i < Rounds; ++i) {
        auto temp0 = h + EP1(e) + CH(e, f, g) + SHA512Constants::RoundConstants[i] + m[i];
        auto temp1 = EP0(a) + MAJ(a, b, c);
        h = g;
        g = f;
        f = e;
        e = d + temp0;
        d = c;
        c = b;
        b = a;
        a = temp0 + temp1;
    }

    m_state[0] += a;
    m_state[1] += b;
    m_state[2] += c;
    m_state[3] += d;
    m_state[4] += e;
    m_state[5] += f;
    m_state[6] += g;
    m_state[7] += h;
}

void SHA512::update(const u8* message, size_t length)
{
    for (size_t i = 0; i < length; ++i) {
        if (m_data_length == BlockSize) {
            transform(m_data_buffer);
            m_bit_length += 1024;
            m_data_length = 0;
        }
        m_data_buffer[m_data_length++] = message[i];
    }
}

SHA512::DigestType SHA512::digest()
{
    auto digest = peek();
    reset();
    return digest;
}

SHA512::DigestType SHA512::peek()
{
    DigestType digest;
    size_t i = m_data_length;

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

    // SHA uses big-endian and we assume little-endian
    // FIXME: looks like a thing for AK::NetworkOrdered,
    //        but he doesn't support shifting operations
    for (size_t i = 0; i < 8; ++i) {
        digest.data[i + 0] = (m_state[0] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 8] = (m_state[1] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 16] = (m_state[2] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 24] = (m_state[3] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 32] = (m_state[4] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 40] = (m_state[5] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 48] = (m_state[6] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 56] = (m_state[7] >> (56 - i * 8)) & 0x000000ff;
    }
    return digest;
}
}
}
