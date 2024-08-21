/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Debug.h>
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <AK/Types.h>
#include <LibCrypto/Authentication/GHash.h>

namespace {

static u32 to_u32(u8 const* b)
{
    return AK::convert_between_host_and_big_endian(ByteReader::load32(b));
}

static void to_u8s(u8* b, u32 const* w)
{
    for (auto i = 0; i < 4; ++i) {
        ByteReader::store(b + i * 4, AK::convert_between_host_and_big_endian(w[i]));
    }
}

}

namespace Crypto::Authentication {

GHash::TagType GHash::process(ReadonlyBytes aad, ReadonlyBytes cipher)
{
    u32 tag[4] { 0, 0, 0, 0 };

    auto transform_one = [&](auto& buf) {
        size_t i = 0;
        for (; i < buf.size(); i += 16) {
            if (i + 16 <= buf.size()) {
                for (auto j = 0; j < 4; ++j) {
                    tag[j] ^= to_u32(buf.offset(i + j * 4));
                }
                galois_multiply(tag, m_key, tag);
            }
        }

        if (i > buf.size()) {
            u8 buffer[16] = {};
            Bytes buffer_bytes { buffer, 16 };
            buf.slice(i - 16).copy_to(buffer_bytes);

            for (auto j = 0; j < 4; ++j) {
                tag[j] ^= to_u32(buffer_bytes.offset(j * 4));
            }
            galois_multiply(tag, m_key, tag);
        }
    };

    transform_one(aad);
    transform_one(cipher);

    auto aad_bits = 8 * (u64)aad.size();
    auto cipher_bits = 8 * (u64)cipher.size();

    auto high = [](u64 value) -> u32 { return value >> 32; };
    auto low = [](u64 value) -> u32 { return value & 0xffffffff; };

    if constexpr (GHASH_PROCESS_DEBUG) {
        dbgln("AAD bits: {} : {}", high(aad_bits), low(aad_bits));
        dbgln("Cipher bits: {} : {}", high(cipher_bits), low(cipher_bits));
        dbgln("Tag bits: {} : {} : {} : {}", tag[0], tag[1], tag[2], tag[3]);
    }

    tag[0] ^= high(aad_bits);
    tag[1] ^= low(aad_bits);
    tag[2] ^= high(cipher_bits);
    tag[3] ^= low(cipher_bits);

    dbgln_if(GHASH_PROCESS_DEBUG, "Tag bits: {} : {} : {} : {}", tag[0], tag[1], tag[2], tag[3]);

    galois_multiply(tag, m_key, tag);

    TagType digest;
    to_u8s(digest.data, tag);

    return digest;
}

void galois_multiply(u32 (&_z)[4], u32 const (&_x)[4], u32 const (&_y)[4])
{
    using namespace AK::SIMD;

    static auto const rotate_left = [](u32x4 const& x) -> u32x4 {
        return u32x4 { x[3], x[0], x[1], x[2] };
    };

    static auto const mul_32_x_32_64 = [](u32x4 const& a, u32x4 const& b) -> u64x4 {
        u64x2 r1;
        u64x2 r2;

#if defined __has_builtin
#    if __has_builtin(__builtin_ia32_pmuludq128)
        if (true) {
            r1 = simd_cast<u64x2>(__builtin_ia32_pmuludq128(simd_cast<i32x4>(u32x4 { a[0], 0, a[1], 0 }), simd_cast<i32x4>(u32x4 { b[0], 0, b[1], 0 })));
            r2 = simd_cast<u64x2>(__builtin_ia32_pmuludq128(simd_cast<i32x4>(u32x4 { a[2], 0, a[3], 0 }), simd_cast<i32x4>(u32x4 { b[2], 0, b[3], 0 })));
        } else
#    endif
#endif
        {
            r1 = u64x2 { static_cast<u64>(a[0]) * static_cast<u64>(b[0]), static_cast<u64>(a[1]) * static_cast<u64>(b[1]) };
            r2 = u64x2 { static_cast<u64>(a[2]) * static_cast<u64>(b[2]), static_cast<u64>(a[3]) * static_cast<u64>(b[3]) };
        }
        return u64x4 { r1[0], r1[1], r2[0], r2[1] };
    };

    static auto const clmul_32_x_32_64 = [](u32 const& a, u32 const& b, u32& lo, u32& hi) -> void {
        constexpr u32x4 mask32 = { 0x11111111, 0x22222222, 0x44444444, 0x88888888 };
        constexpr u64x4 mask64 = { 0x1111111111111111ull, 0x2222222222222222ull, 0x4444444444444444ull, 0x8888888888888888ull };

        u32x4 ta;
        u32x4 tb;
        u64x4 tu64;
        u64x4 tc;
        u64 cc;

        ta = a & mask32;
        tb = b & mask32;
        tb = item_reverse(tb);

        tb = rotate_left(tb);
        tu64 = mul_32_x_32_64(ta, tb);
        tc[0] = reduce_xor(u64x4 { tu64[0], tu64[1], tu64[2], tu64[3] });

        tb = rotate_left(tb);
        tu64 = mul_32_x_32_64(ta, tb);
        tc[1] = reduce_xor(u64x4 { tu64[0], tu64[1], tu64[2], tu64[3] });

        tb = rotate_left(tb);
        tu64 = mul_32_x_32_64(ta, tb);
        tc[2] = reduce_xor(u64x4 { tu64[0], tu64[1], tu64[2], tu64[3] });

        tb = rotate_left(tb);
        tu64 = mul_32_x_32_64(ta, tb);
        tc[3] = reduce_xor(u64x4 { tu64[0], tu64[1], tu64[2], tu64[3] });

        tc &= mask64;
        cc = reduce_or(tc);
        lo = static_cast<u32>((cc >> (0 * 32)) & 0xfffffffful);
        hi = static_cast<u32>((cc >> (1 * 32)) & 0xfffffffful);
    };

    u32 aa[4];
    u32 bb[4];
    u32 ta[9];
    u32 tb[9];
    u32 tc[4];
    u32 tu32[4];
    u32 td[4];
    u32 te[4];
    u32 z[8];

    aa[3] = _x[0];
    aa[2] = _x[1];
    aa[1] = _x[2];
    aa[0] = _x[3];
    bb[3] = _y[0];
    bb[2] = _y[1];
    bb[1] = _y[2];
    bb[0] = _y[3];
    ta[0] = aa[0];
    ta[1] = aa[1];
    ta[2] = ta[0] ^ ta[1];
    ta[3] = aa[2];
    ta[4] = aa[3];
    ta[5] = ta[3] ^ ta[4];
    ta[6] = ta[0] ^ ta[3];
    ta[7] = ta[1] ^ ta[4];
    ta[8] = ta[6] ^ ta[7];
    tb[0] = bb[0];
    tb[1] = bb[1];
    tb[2] = tb[0] ^ tb[1];
    tb[3] = bb[2];
    tb[4] = bb[3];
    tb[5] = tb[3] ^ tb[4];
    tb[6] = tb[0] ^ tb[3];
    tb[7] = tb[1] ^ tb[4];
    tb[8] = tb[6] ^ tb[7];
    for (int i = 0; i != 9; ++i) {
        clmul_32_x_32_64(ta[i], tb[i], ta[i], tb[i]);
    }
    tc[0] = ta[0];
    tc[1] = ta[0] ^ ta[1] ^ ta[2] ^ tb[0];
    tc[2] = ta[1] ^ tb[0] ^ tb[1] ^ tb[2];
    tc[3] = tb[1];
    td[0] = ta[3];
    td[1] = ta[3] ^ ta[4] ^ ta[5] ^ tb[3];
    td[2] = ta[4] ^ tb[3] ^ tb[4] ^ tb[5];
    td[3] = tb[4];
    te[0] = ta[6];
    te[1] = ta[6] ^ ta[7] ^ ta[8] ^ tb[6];
    te[2] = ta[7] ^ tb[6] ^ tb[7] ^ tb[8];
    te[3] = tb[7];
    te[0] ^= (tc[0] ^ td[0]);
    te[1] ^= (tc[1] ^ td[1]);
    te[2] ^= (tc[2] ^ td[2]);
    te[3] ^= (tc[3] ^ td[3]);
    tc[2] ^= te[0];
    tc[3] ^= te[1];
    td[0] ^= te[2];
    td[1] ^= te[3];
    z[0] = tc[0] << 1;
    z[1] = (tc[1] << 1) | (tc[0] >> 31);
    z[2] = (tc[2] << 1) | (tc[1] >> 31);
    z[3] = (tc[3] << 1) | (tc[2] >> 31);
    z[4] = (td[0] << 1) | (tc[3] >> 31);
    z[5] = (td[1] << 1) | (td[0] >> 31);
    z[6] = (td[2] << 1) | (td[1] >> 31);
    z[7] = (td[3] << 1) | (td[2] >> 31);
    for (int i = 0; i != 4; ++i) {
        tu32[0] = z[i] << 31;
        tu32[1] = z[i] << 30;
        tu32[2] = z[i] << 25;
        z[i + 3] ^= (tu32[0] ^ tu32[1] ^ tu32[2]);
        tu32[0] = z[i] >> 0;
        tu32[1] = z[i] >> 1;
        tu32[2] = z[i] >> 2;
        tu32[3] = z[i] >> 7;
        z[i + 4] ^= (tu32[0] ^ tu32[1] ^ tu32[2] ^ tu32[3]);
    }
    _z[0] = z[7];
    _z[1] = z[6];
    _z[2] = z[5];
    _z[3] = z[4];
}

}
