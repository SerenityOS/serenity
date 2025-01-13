/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CPUFeatures.h>
#include <AK/Platform.h>
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/SHA2.h>

namespace Crypto::Hash {
constexpr static auto ROTRIGHT(u32 a, size_t b) { return (a >> b) | (a << (32 - b)); }
constexpr static auto CH(u32 x, u32 y, u32 z) { return (x & y) ^ (z & ~x); }
constexpr static auto MAJ(u32 x, u32 y, u32 z) { return (x & y) ^ (x & z) ^ (y & z); }
constexpr static auto EP0(u32 x) { return ROTRIGHT(x, 2) ^ ROTRIGHT(x, 13) ^ ROTRIGHT(x, 22); }
constexpr static auto EP1(u32 x) { return ROTRIGHT(x, 6) ^ ROTRIGHT(x, 11) ^ ROTRIGHT(x, 25); }
constexpr static auto SIGN0(u32 x) { return ROTRIGHT(x, 7) ^ ROTRIGHT(x, 18) ^ (x >> 3); }
constexpr static auto SIGN1(u32 x) { return ROTRIGHT(x, 17) ^ ROTRIGHT(x, 19) ^ (x >> 10); }

constexpr static auto ROTRIGHT(u64 a, size_t b) { return (a >> b) | (a << (64 - b)); }
constexpr static auto CH(u64 x, u64 y, u64 z) { return (x & y) ^ (z & ~x); }
constexpr static auto MAJ(u64 x, u64 y, u64 z) { return (x & y) ^ (x & z) ^ (y & z); }
constexpr static auto EP0(u64 x) { return ROTRIGHT(x, 28) ^ ROTRIGHT(x, 34) ^ ROTRIGHT(x, 39); }
constexpr static auto EP1(u64 x) { return ROTRIGHT(x, 14) ^ ROTRIGHT(x, 18) ^ ROTRIGHT(x, 41); }
constexpr static auto SIGN0(u64 x) { return ROTRIGHT(x, 1) ^ ROTRIGHT(x, 8) ^ (x >> 7); }
constexpr static auto SIGN1(u64 x) { return ROTRIGHT(x, 19) ^ ROTRIGHT(x, 61) ^ (x >> 6); }

template<>
void SHA256::transform_impl<CPUFeatures::None>()
{
    auto& data = m_data_buffer;

    u32 m[16];

    size_t i = 0;
    for (size_t j = 0; i < 16; ++i, j += 4) {
        m[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | data[j + 3];
    }

    auto a = m_state[0], b = m_state[1],
         c = m_state[2], d = m_state[3],
         e = m_state[4], f = m_state[5],
         g = m_state[6], h = m_state[7];

    for (i = 0; i < Rounds; ++i) {
        if (i >= 16)
            m[i % 16] = SIGN1(m[(i - 2) % 16]) + m[(i - 7) % 16] + SIGN0(m[(i - 15) % 16]) + m[(i - 16) % 16];

        auto temp0 = h + EP1(e) + CH(e, f, g) + SHA256Constants::RoundConstants[i] + m[i % 16];
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

// Note: The SHA extension was introduced with
//       Intel Goldmont (SSE4.2), Ice Lake (AVX512), Rocket Lake (AVX512), and AMD Zen (AVX2)
//       So it's safe to assume that if we have SHA we have at least SSE4.2
//      ~https://en.wikipedia.org/wiki/Intel_SHA_extensions
#if AK_CAN_CODEGEN_FOR_X86_SHA && AK_CAN_CODEGEN_FOR_X86_SSE42
template<>
[[gnu::target("sha,sse4.2")]] void SHA256::transform_impl<CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42>()
{
    using AK::SIMD::i32x4, AK::SIMD::u32x4;

    auto& state = m_state;
    auto& data = m_data_buffer;

    u32x4 states[2] {};
    states[0] = AK::SIMD::load_unaligned<u32x4>(&state[0]);
    states[1] = AK::SIMD::load_unaligned<u32x4>(&state[4]);
    auto tmp = u32x4 { states[0][1], states[0][0], states[0][3], states[0][2] };
    states[1] = u32x4 { states[1][3], states[1][2], states[1][1], states[1][0] };
    states[0] = u32x4 { states[1][2], states[1][3], tmp[0], tmp[1] };
    states[1] = u32x4 { states[1][0], states[1][1], tmp[2], tmp[3] };

    u32x4 msgs[4] {};
    u32x4 old[2] { states[0], states[1] };
    for (int i = 0; i != 16; ++i) {
        u32x4 msg {};
        if (i < 4) {
            msgs[i] = AK::SIMD::load_unaligned<u32x4>(&data[i * 16]);
            msgs[i] = AK::SIMD::elementwise_byte_reverse(msgs[i]);
            tmp = AK::SIMD::load_unaligned<u32x4>(&SHA256Constants::RoundConstants[i * 4]);
            msg = msgs[i] + tmp;
        } else {
            msgs[(i + 0) % 4] = bit_cast<u32x4>(__builtin_ia32_sha256msg1(bit_cast<i32x4>(msgs[(i + 0) % 4]), bit_cast<i32x4>(msgs[(i + 1) % 4])));
            tmp = u32x4 { msgs[(i + 2) % 4][1], msgs[(i + 2) % 4][2], msgs[(i + 2) % 4][3], msgs[(i + 3) % 4][0] };
            msgs[(i + 0) % 4] += tmp;
            msgs[(i + 0) % 4] = bit_cast<u32x4>(__builtin_ia32_sha256msg2(bit_cast<i32x4>(msgs[(i + 0) % 4]), bit_cast<i32x4>(msgs[(i + 3) % 4])));
            tmp = AK::SIMD::load_unaligned<u32x4>(&SHA256Constants::RoundConstants[i * 4]);
            msg = msgs[(i + 0) % 4] + tmp;
        }
        states[1] = bit_cast<u32x4>(__builtin_ia32_sha256rnds2(bit_cast<i32x4>(states[1]), bit_cast<i32x4>(states[0]), bit_cast<i32x4>(msg)));
        msg = u32x4 { msg[2], msg[3], 0, 0 };
        states[0] = bit_cast<u32x4>(__builtin_ia32_sha256rnds2(bit_cast<i32x4>(states[0]), bit_cast<i32x4>(states[1]), bit_cast<i32x4>(msg)));
    }
    states[0] += old[0];
    states[1] += old[1];

    tmp = u32x4 { states[0][3], states[0][2], states[0][1], states[0][0] };
    states[1] = u32x4 { states[1][1], states[1][0], states[1][3], states[1][2] };
    states[0] = u32x4 { tmp[0], tmp[1], states[1][2], states[1][3] };
    states[1] = u32x4 { tmp[2], tmp[3], states[1][0], states[1][1] };
    AK::SIMD::store_unaligned(&state[0], states[0]);
    AK::SIMD::store_unaligned(&state[4], states[1]);
}
#endif

decltype(SHA256::transform_dispatched) SHA256::transform_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42)) {
        if (has_flag(features, CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42))
            return &SHA256::transform_impl<CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42>;
    }

    return &SHA256::transform_impl<CPUFeatures::None>;
}();

template<size_t BlockSize, typename Callback>
void update_buffer(u8* buffer, u8 const* input, size_t length, size_t& data_length, Callback callback)
{
    while (length > 0) {
        size_t copy_bytes = AK::min(length, BlockSize - data_length);
        __builtin_memcpy(buffer + data_length, input, copy_bytes);
        input += copy_bytes;
        length -= copy_bytes;
        data_length += copy_bytes;
        if (data_length == BlockSize) {
            callback();
            data_length = 0;
        }
    }
}

void SHA256::update(u8 const* message, size_t length)
{
    update_buffer<BlockSize>(m_data_buffer, message, length, m_data_length, [&]() {
        transform();
        m_bit_length += BlockSize * 8;
    });
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

    if (i < FinalBlockDataSize) {
        m_data_buffer[i++] = 0x80;
        while (i < FinalBlockDataSize)
            m_data_buffer[i++] = 0x00;
    } else {
        // First, complete a block with some padding.
        m_data_buffer[i++] = 0x80;
        while (i < BlockSize)
            m_data_buffer[i++] = 0x00;
        transform();

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

    transform();

    // SHA uses big-endian and we assume little-endian
    // FIXME: looks like a thing for AK::NetworkOrdered,
    //        but that doesn't support shifting operations
    for (i = 0; i < 4; ++i) {
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

inline void SHA384::transform(u8 const* data)
{
    u64 m[16];

    size_t i = 0;
    for (size_t j = 0; i < 16; ++i, j += 8) {
        m[i] = ((u64)data[j] << 56) | ((u64)data[j + 1] << 48) | ((u64)data[j + 2] << 40) | ((u64)data[j + 3] << 32) | ((u64)data[j + 4] << 24) | ((u64)data[j + 5] << 16) | ((u64)data[j + 6] << 8) | (u64)data[j + 7];
    }

    auto a = m_state[0], b = m_state[1],
         c = m_state[2], d = m_state[3],
         e = m_state[4], f = m_state[5],
         g = m_state[6], h = m_state[7];

    for (i = 0; i < Rounds; ++i) {
        if (i >= 16)
            m[i % 16] = SIGN1(m[(i - 2) % 16]) + m[(i - 7) % 16] + SIGN0(m[(i - 15) % 16]) + m[(i - 16) % 16];
        // Note : SHA384 uses the SHA512 constants.
        auto temp0 = h + EP1(e) + CH(e, f, g) + SHA512Constants::RoundConstants[i] + m[i % 16];
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

void SHA384::update(u8 const* message, size_t length)
{
    update_buffer<BlockSize>(m_data_buffer, message, length, m_data_length, [&]() {
        transform(m_data_buffer);
        m_bit_length += BlockSize * 8;
    });
}

SHA384::DigestType SHA384::digest()
{
    auto digest = peek();
    reset();
    return digest;
}

SHA384::DigestType SHA384::peek()
{
    DigestType digest;
    size_t i = m_data_length;

    if (i < FinalBlockDataSize) {
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
    // FIXME: Theoretically we should keep track of the number of bits as a u128, now we can only hash up to 2 EiB.
    m_data_buffer[BlockSize - 9] = 0;
    m_data_buffer[BlockSize - 10] = 0;
    m_data_buffer[BlockSize - 11] = 0;
    m_data_buffer[BlockSize - 12] = 0;
    m_data_buffer[BlockSize - 13] = 0;
    m_data_buffer[BlockSize - 14] = 0;
    m_data_buffer[BlockSize - 15] = 0;
    m_data_buffer[BlockSize - 16] = 0;

    transform(m_data_buffer);

    // SHA uses big-endian and we assume little-endian
    // FIXME: looks like a thing for AK::NetworkOrdered,
    //        but that doesn't support shifting operations
    for (i = 0; i < 8; ++i) {
        digest.data[i + 0] = (m_state[0] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 8] = (m_state[1] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 16] = (m_state[2] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 24] = (m_state[3] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 32] = (m_state[4] >> (56 - i * 8)) & 0x000000ff;
        digest.data[i + 40] = (m_state[5] >> (56 - i * 8)) & 0x000000ff;
    }
    return digest;
}

inline void SHA512::transform(u8 const* data)
{
    u64 m[16];

    size_t i = 0;
    for (size_t j = 0; i < 16; ++i, j += 8) {
        m[i] = ((u64)data[j] << 56) | ((u64)data[j + 1] << 48) | ((u64)data[j + 2] << 40) | ((u64)data[j + 3] << 32) | ((u64)data[j + 4] << 24) | ((u64)data[j + 5] << 16) | ((u64)data[j + 6] << 8) | (u64)data[j + 7];
    }

    auto a = m_state[0], b = m_state[1],
         c = m_state[2], d = m_state[3],
         e = m_state[4], f = m_state[5],
         g = m_state[6], h = m_state[7];

    for (i = 0; i < Rounds; ++i) {
        if (i >= 16)
            m[i % 16] = SIGN1(m[(i - 2) % 16]) + m[(i - 7) % 16] + SIGN0(m[(i - 15) % 16]) + m[(i - 16) % 16];

        auto temp0 = h + EP1(e) + CH(e, f, g) + SHA512Constants::RoundConstants[i] + m[i % 16];
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

void SHA512::update(u8 const* message, size_t length)
{
    update_buffer<BlockSize>(m_data_buffer, message, length, m_data_length, [&]() {
        transform(m_data_buffer);
        m_bit_length += BlockSize * 8;
    });
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

    if (i < FinalBlockDataSize) {
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
    // FIXME: Theoretically we should keep track of the number of bits as a u128, now we can only hash up to 2 EiB.
    m_data_buffer[BlockSize - 9] = 0;
    m_data_buffer[BlockSize - 10] = 0;
    m_data_buffer[BlockSize - 11] = 0;
    m_data_buffer[BlockSize - 12] = 0;
    m_data_buffer[BlockSize - 13] = 0;
    m_data_buffer[BlockSize - 14] = 0;
    m_data_buffer[BlockSize - 15] = 0;
    m_data_buffer[BlockSize - 16] = 0;

    transform(m_data_buffer);

    // SHA uses big-endian and we assume little-endian
    // FIXME: looks like a thing for AK::NetworkOrdered,
    //        but that doesn't support shifting operations
    for (i = 0; i < 8; ++i) {
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
