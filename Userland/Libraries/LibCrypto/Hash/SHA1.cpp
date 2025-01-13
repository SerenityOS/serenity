/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CPUFeatures.h>
#include <AK/Endian.h>
#include <AK/Memory.h>
#include <AK/Platform.h>
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/SHA1.h>

namespace Crypto::Hash {

static constexpr auto ROTATE_LEFT(u32 value, size_t bits)
{
    return (value << bits) | (value >> (32 - bits));
}

template<>
void SHA1::transform_impl<CPUFeatures::None>()
{
    auto& data = m_data_buffer;

    u32 blocks[16];
    for (size_t i = 0; i < 16; ++i)
        blocks[i] = AK::convert_between_host_and_network_endian(((u32 const*)data)[i]);

    auto a = m_state[0], b = m_state[1], c = m_state[2], d = m_state[3], e = m_state[4];
    u32 f, k;

    for (size_t i = 0; i < Rounds; ++i) {
        // w[i] = (w[i-3] xor w[i-8] xor w[i-14] xor w[i-16]) leftrotate 1
        if (i >= 16)
            blocks[i % 16] = ROTATE_LEFT(blocks[(i - 3) % 16] ^ blocks[(i - 8) % 16] ^ blocks[(i - 14) % 16] ^ blocks[(i - 16) % 16], 1);

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
        auto temp = ROTATE_LEFT(a, 5) + f + e + k + blocks[i % 16];
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
    secure_zero(blocks, 16 * sizeof(u32));
}

// Note: The SHA extension was introduced with
//       Intel Goldmont (SSE4.2), Ice Lake (AVX512), Rocket Lake (AVX512), and AMD Zen (AVX2)
//       So it's safe to assume that if we have SHA we have at least SSE4.2
//      ~https://en.wikipedia.org/wiki/Intel_SHA_extensions
#if AK_CAN_CODEGEN_FOR_X86_SHA && AK_CAN_CODEGEN_FOR_X86_SSE42
template<>
[[gnu::target("sha,sse4.2")]] void SHA1::transform_impl<CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42>()
{
#    define SHA_TARGET gnu::target("sha"), gnu::always_inline

    auto& state = m_state;
    auto& data = m_data_buffer;

    using AK::SIMD::u32x4, AK::SIMD::i32x4;
    // Note: These need to be unsigned, as we add to them and expect them to wrap around,
    //       and signed overflow is UB;
    //  BUT: GCC -for some reason- expects the input types to the SHA intrinsics to be signed
    //       and there does not seem to be a way to temporarily enable -flax-vector-conversions
    //       so this leads to a few bit_casts further down
    u32x4 abcd[2] {};
    u32x4 msgs[4] {};

    abcd[0] = AK::SIMD::load_unaligned<u32x4>(&state[0]);
    abcd[0] = AK::SIMD::item_reverse(abcd[0]);
    u32x4 e { 0, 0, 0, state[4] };

    auto sha_msg1 = [] [[SHA_TARGET]] (u32x4 a, u32x4 b) { return bit_cast<u32x4>(__builtin_ia32_sha1msg1(bit_cast<i32x4>(a), bit_cast<i32x4>(b))); };
    auto sha_msg2 = [] [[SHA_TARGET]] (u32x4 a, u32x4 b) { return bit_cast<u32x4>(__builtin_ia32_sha1msg2(bit_cast<i32x4>(a), bit_cast<i32x4>(b))); };
    auto sha_next_e = [] [[SHA_TARGET]] (u32x4 a, u32x4 b) { return bit_cast<u32x4>(__builtin_ia32_sha1nexte(bit_cast<i32x4>(a), bit_cast<i32x4>(b))); };
    auto sha_rnds4 = []<int i> [[SHA_TARGET]] (u32x4 a, u32x4 b) { return bit_cast<u32x4>(__builtin_ia32_sha1rnds4(bit_cast<i32x4>(a), bit_cast<i32x4>(b), i)); };

    auto group = [&]<int i_group> [[SHA_TARGET]] () {
        for (size_t i_pack = 0; i_pack != 5; ++i_pack) {
            size_t i_msg = i_group * 5 + i_pack;
            if (i_msg < 4) {
                msgs[i_msg] = AK::SIMD::load_unaligned<u32x4>(&data[i_msg * 16]);
                msgs[i_msg] = AK::SIMD::byte_reverse(msgs[i_msg]);
            } else {
                msgs[i_msg % 4] = sha_msg1(msgs[i_msg % 4], msgs[(i_msg + 1) % 4]);
                msgs[i_msg % 4] ^= msgs[(i_msg + 2) % 4];
                msgs[i_msg % 4] = sha_msg2(msgs[i_msg % 4], msgs[(i_msg + 3) % 4]);
            }
            if (i_msg == 0) {
                e += msgs[0];
            } else {
                e = sha_next_e(abcd[(i_msg + 1) % 2], msgs[(i_msg + 0) % 4]);
            }
            abcd[(i_msg + 1) % 2] = sha_rnds4.operator()<i_group>(abcd[(i_msg + 0) % 2], e);
        }
    };

    auto old_abcd = abcd[0];
    auto old_e = e;
    group.operator()<0>();
    group.operator()<1>();
    group.operator()<2>();
    group.operator()<3>();
    e = sha_next_e(abcd[1], u32x4 {});
    abcd[0] += old_abcd;
    e += old_e;

    abcd[0] = AK::SIMD::item_reverse(abcd[0]);
    AK::SIMD::store_unaligned(&state[0], abcd[0]);
    state[4] = e[3];

#    undef SHA_TARGET
}
#endif

decltype(SHA1::transform_dispatched) SHA1::transform_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42)) {
        if (has_flag(features, CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42))
            return &SHA1::transform_impl<CPUFeatures::X86_SHA | CPUFeatures::X86_SSE42>;
    }

    return &SHA1::transform_impl<CPUFeatures::None>;
}();

void SHA1::update(u8 const* message, size_t length)
{
    while (length > 0) {
        size_t copy_bytes = AK::min(length, BlockSize - m_data_length);
        __builtin_memcpy(m_data_buffer + m_data_length, message, copy_bytes);
        message += copy_bytes;
        length -= copy_bytes;
        m_data_length += copy_bytes;
        if (m_data_length == BlockSize) {
            transform();
            m_bit_length += BlockSize * 8;
            m_data_length = 0;
        }
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

    if (m_data_length < FinalBlockDataSize) {
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

    for (i = 0; i < 4; ++i) {
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
