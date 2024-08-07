/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CPUFeatures.h>
#include <AK/Platform.h>
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <AK/StringBuilder.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Cipher/AESTables.h>

namespace Crypto::Cipher {

template<typename T>
constexpr u32 get_key(T pt)
{
    return ((u32)(pt)[0] << 24) ^ ((u32)(pt)[1] << 16) ^ ((u32)(pt)[2] << 8) ^ ((u32)(pt)[3]);
}

constexpr void swap_keys(u32* keys, size_t i, size_t j)
{
    u32 temp = keys[i];
    keys[i] = keys[j];
    keys[j] = temp;
}

#ifndef KERNEL
ByteString AESCipherBlock::to_byte_string() const
{
    StringBuilder builder;
    for (auto value : m_data)
        builder.appendff("{:02x}", value);
    return builder.to_byte_string();
}

ByteString AESCipherKey::to_byte_string() const
{
    StringBuilder builder;
    for (size_t i = 0; i < (rounds() + 1) * 4; ++i)
        builder.appendff("{:02x}", m_rd_keys[i]);
    return builder.to_byte_string();
}
#endif

template<>
void AESCipherKey::expand_encrypt_key_impl<CPUFeatures::None>(ReadonlyBytes user_key, size_t bits)
{
    u32* round_key;
    u32 temp;
    size_t i { 0 };

    VERIFY(!user_key.is_null());
    VERIFY(is_valid_key_size(bits));
    VERIFY(user_key.size() == bits / 8);

    round_key = round_keys();

    if (bits == 128) {
        m_rounds = 10;
    } else if (bits == 192) {
        m_rounds = 12;
    } else {
        m_rounds = 14;
    }

    round_key[0] = get_key(user_key.data());
    round_key[1] = get_key(user_key.data() + 4);
    round_key[2] = get_key(user_key.data() + 8);
    round_key[3] = get_key(user_key.data() + 12);
    if (bits == 128) {
        for (;;) {
            temp = round_key[3];
            // clang-format off
            round_key[4] = round_key[0] ^
                    (AESTables::Encode2[(temp >> 16) & 0xff] & 0xff000000) ^
                    (AESTables::Encode3[(temp >>  8) & 0xff] & 0x00ff0000) ^
                    (AESTables::Encode0[(temp      ) & 0xff] & 0x0000ff00) ^
                    (AESTables::Encode1[(temp >> 24)       ] & 0x000000ff) ^ AESTables::RCON[i];
            // clang-format on
            round_key[5] = round_key[1] ^ round_key[4];
            round_key[6] = round_key[2] ^ round_key[5];
            round_key[7] = round_key[3] ^ round_key[6];
            ++i;
            if (i == 10)
                break;
            round_key += 4;
        }
        return;
    }

    round_key[4] = get_key(user_key.data() + 16);
    round_key[5] = get_key(user_key.data() + 20);
    if (bits == 192) {
        for (;;) {
            temp = round_key[5];
            // clang-format off
            round_key[6] = round_key[0] ^
                    (AESTables::Encode2[(temp >> 16) & 0xff] & 0xff000000) ^
                    (AESTables::Encode3[(temp >>  8) & 0xff] & 0x00ff0000) ^
                    (AESTables::Encode0[(temp      ) & 0xff] & 0x0000ff00) ^
                    (AESTables::Encode1[(temp >> 24)       ] & 0x000000ff) ^ AESTables::RCON[i];
            // clang-format on
            round_key[7] = round_key[1] ^ round_key[6];
            round_key[8] = round_key[2] ^ round_key[7];
            round_key[9] = round_key[3] ^ round_key[8];

            ++i;
            if (i == 8)
                break;

            round_key[10] = round_key[4] ^ round_key[9];
            round_key[11] = round_key[5] ^ round_key[10];

            round_key += 6;
        }
        return;
    }

    round_key[6] = get_key(user_key.data() + 24);
    round_key[7] = get_key(user_key.data() + 28);
    if (true) { // bits == 256
        for (;;) {
            temp = round_key[7];
            // clang-format off
            round_key[8] = round_key[0] ^
                    (AESTables::Encode2[(temp >> 16) & 0xff] & 0xff000000) ^
                    (AESTables::Encode3[(temp >>  8) & 0xff] & 0x00ff0000) ^
                    (AESTables::Encode0[(temp      ) & 0xff] & 0x0000ff00) ^
                    (AESTables::Encode1[(temp >> 24)       ] & 0x000000ff) ^ AESTables::RCON[i];
            // clang-format on
            round_key[9] = round_key[1] ^ round_key[8];
            round_key[10] = round_key[2] ^ round_key[9];
            round_key[11] = round_key[3] ^ round_key[10];

            ++i;
            if (i == 7)
                break;

            temp = round_key[11];
            // clang-format off
            round_key[12] = round_key[4] ^
                    (AESTables::Encode2[(temp >> 24)       ] & 0xff000000) ^
                    (AESTables::Encode3[(temp >> 16) & 0xff] & 0x00ff0000) ^
                    (AESTables::Encode0[(temp >>  8) & 0xff] & 0x0000ff00) ^
                    (AESTables::Encode1[(temp      ) & 0xff] & 0x000000ff) ;
            // clang-format on
            round_key[13] = round_key[5] ^ round_key[12];
            round_key[14] = round_key[6] ^ round_key[13];
            round_key[15] = round_key[7] ^ round_key[14];

            round_key += 8;
        }
        return;
    }
}

#if AK_CAN_CODEGEN_FOR_X86_AES
[[gnu::target("aes")]] static auto AESCipherKey_expand_encrypt_key_128_assist(auto const& a, auto const& b)
{
    AK::SIMD::i32x4 ta {};
    AK::SIMD::i32x4 tb {};
    AK::SIMD::i32x4 tc {};

    static_assert(sizeof(a) == 16);
    static_assert(sizeof(b) == 16);
    static_assert(IsSame<decltype(a), decltype(b)>);

    ta = bit_cast<AK::SIMD::i32x4>(a);
    tb = bit_cast<AK::SIMD::i32x4>(b);
    tb = AK::SIMD::i32x4 { tb[3], tb[3], tb[3], tb[3] };
    tc = AK::SIMD::i32x4 { 0, ta[0], ta[1], ta[2] };
    ta ^= tc;
    tc = AK::SIMD::i32x4 { 0, ta[0], ta[1], ta[2] };
    ta ^= tc;
    tc = AK::SIMD::i32x4 { 0, ta[0], ta[1], ta[2] };
    ta ^= tc;
    ta ^= tb;
    return bit_cast<RemoveReference<decltype(a)>>(ta);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_128(u32* round_key, ReadonlyBytes user_key)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    auto const* user_ptr = user_key.data();
    illx2 ta {};
    illx2 tb {};

    ta = AK::SIMD::load_unaligned<illx2>(&user_ptr[0 * 16]);
    AK::SIMD::store_unaligned(&round_key[0 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x01);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[1 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x02);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[2 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x04);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[3 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x08);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[4 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x10);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[5 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x20);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[6 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x40);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[7 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x80);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[8 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x1b);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[9 * 4], ta);

    tb = __builtin_ia32_aeskeygenassist128(ta, 0x36);
    ta = AESCipherKey_expand_encrypt_key_128_assist(ta, tb);
    AK::SIMD::store_unaligned(&round_key[10 * 4], ta);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_192_assist(AK::SIMD::i32x4& a, AK::SIMD::i32x4& b, AK::SIMD::i32x4& c)
{
    AK::SIMD::i32x4 ta {};
    AK::SIMD::i32x4 tb {};
    AK::SIMD::i32x4 tc {};
    AK::SIMD::i32x4 td {};

    ta = bit_cast<AK::SIMD::i32x4>(a);
    tb = bit_cast<AK::SIMD::i32x4>(b);
    tc = bit_cast<AK::SIMD::i32x4>(c);
    tb = AK::SIMD::i32x4 { tb[1], tb[1], tb[1], tb[1] };
    td = AK::SIMD::i32x4 { 0, ta[0], ta[1], ta[2] };
    ta ^= td;
    td = AK::SIMD::i32x4 { 0, td[0], td[1], td[2] };
    ta ^= td;
    td = AK::SIMD::i32x4 { 0, td[0], td[1], td[2] };
    ta ^= td;
    ta ^= tb;
    tb = AK::SIMD::i32x4 { ta[3], ta[3], ta[3], ta[3] };
    td = AK::SIMD::i32x4 { 0, tc[0], tc[1], tc[2] };
    tc ^= td;
    tc ^= tb;
    a = bit_cast<AK::SIMD::i32x4>(ta);
    b = bit_cast<AK::SIMD::i32x4>(tb);
    c = bit_cast<AK::SIMD::i32x4>(tc);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_192(u32* round_key, ReadonlyBytes user_key)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    auto const* user_ptr = user_key.data();
    AK::SIMD::i32x4 ta {};
    AK::SIMD::i32x4 tb {};
    AK::SIMD::i32x4 tc {};
    AK::SIMD::i32x4 td {};

    ta = AK::SIMD::load_unaligned<AK::SIMD::i32x4>(&user_ptr[0 * 16]);
    tb = bit_cast<AK::SIMD::i32x4>(AK::SIMD::i64x2 { (i64)ByteReader::load64(&user_ptr[1 * 16]), 0 });
    AK::SIMD::store_unaligned(&round_key[0 * 4], ta);
#    define ROUND(i1, i2, i3, k1, k2)                                                                  \
        td = tb;                                                                                       \
        tc = bit_cast<AK::SIMD::i32x4>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), k1));    \
        AESCipherKey_expand_encrypt_key_192_assist(ta, tc, tb);                                        \
        AK::SIMD::store_unaligned(&round_key[i1 * 4], AK::SIMD::i32x4 { td[0], td[1], ta[0], ta[1] }); \
        AK::SIMD::store_unaligned(&round_key[i2 * 4], AK::SIMD::i32x4 { ta[2], ta[3], tb[0], tb[1] }); \
        tc = bit_cast<AK::SIMD::i32x4>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), k2));    \
        AESCipherKey_expand_encrypt_key_192_assist(ta, tc, tb);                                        \
        AK::SIMD::store_unaligned(&round_key[i3 * 4], ta);

    ROUND(1, 2, 3, 0x01, 0x02);
    ROUND(4, 5, 6, 0x04, 0x08);
    ROUND(7, 8, 9, 0x10, 0x20);
    ROUND(10, 11, 12, 0x40, 0x80);
    AK::SIMD::store_unaligned(&round_key[12 * 4], ta);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_256_assist_a(AK::SIMD::i64x2& a, AK::SIMD::i64x2& b)
{
    AK::SIMD::i32x4 ta {};
    AK::SIMD::i32x4 tb {};
    AK::SIMD::i32x4 tc {};

    ta = bit_cast<AK::SIMD::i32x4>(a);
    tb = bit_cast<AK::SIMD::i32x4>(b);
    tb = AK::SIMD::i32x4 { tb[3], tb[3], tb[3], tb[3] };
    tc = AK::SIMD::i32x4 { 0, ta[0], ta[1], ta[2] };
    ta ^= tc;
    tc = AK::SIMD::i32x4 { 0, tc[0], tc[1], tc[2] };
    ta ^= tc;
    tc = AK::SIMD::i32x4 { 0, tc[0], tc[1], tc[2] };
    ta ^= tc;
    ta ^= tb;
    a = bit_cast<AK::SIMD::i64x2>(ta);
    b = bit_cast<AK::SIMD::i64x2>(tb);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_256_assist_b(AK::SIMD::i64x2 const& a, AK::SIMD::i64x2& b)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    AK::SIMD::i32x4 tb {};
    AK::SIMD::i32x4 tc {};
    AK::SIMD::i32x4 td {};

    tb = bit_cast<AK::SIMD::i32x4>(b);
    tc = bit_cast<AK::SIMD::i32x4>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(a), 0x00));
    td = AK::SIMD::i32x4 { tc[2], tc[2], tc[2], tc[2] };
    tc = AK::SIMD::i32x4 { 0, tb[0], tb[1], tb[2] };
    tb ^= tc;
    tc = AK::SIMD::i32x4 { 0, tc[0], tc[1], tc[2] };
    tb ^= tc;
    tc = AK::SIMD::i32x4 { 0, tc[0], tc[1], tc[2] };
    tb ^= tc;
    tb ^= td;
    b = bit_cast<AK::SIMD::i64x2>(tb);
}

[[gnu::target("aes")]] static void AESCipherKey_expand_encrypt_key_256(u32* round_key, ReadonlyBytes user_key)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    auto const* user_ptr = user_key.data();
    AK::SIMD::i64x2 ta {};
    AK::SIMD::i64x2 tb {};
    AK::SIMD::i64x2 tc {};

    ta = AK::SIMD::load_unaligned<AK::SIMD::i64x2>(&user_ptr[0 * 16]);
    tb = AK::SIMD::load_unaligned<AK::SIMD::i64x2>(&user_ptr[1 * 16]);
    AK::SIMD::store_unaligned(&round_key[0 * 4], ta);
    AK::SIMD::store_unaligned(&round_key[1 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x01));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[2 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[3 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x02));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[4 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[5 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x04));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[6 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[7 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x08));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[8 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[9 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x10));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[10 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[11 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x20));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[12 * 4], ta);

    AESCipherKey_expand_encrypt_key_256_assist_b(ta, tb);
    AK::SIMD::store_unaligned(&round_key[13 * 4], tb);

    tc = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aeskeygenassist128(bit_cast<illx2>(tb), 0x40));
    AESCipherKey_expand_encrypt_key_256_assist_a(ta, tc);
    AK::SIMD::store_unaligned(&round_key[14 * 4], ta);
}

template<>
[[gnu::target("aes")]] void AESCipherKey::expand_encrypt_key_impl<CPUFeatures::X86_AES>(ReadonlyBytes user_key, size_t bits)
{
    switch (bits) {
    case 128:
        m_rounds = 10;
        AESCipherKey_expand_encrypt_key_128(m_rd_keys, user_key);
        break;
    case 192:
        m_rounds = 12;
        AESCipherKey_expand_encrypt_key_192(m_rd_keys, user_key);
        break;
    case 256:
        m_rounds = 14;
        AESCipherKey_expand_encrypt_key_256(m_rd_keys, user_key);
        break;
    }
}

template<>
[[gnu::target("aes")]] void AESCipherKey::expand_decrypt_key_impl<CPUFeatures::X86_AES>(ReadonlyBytes user_key, size_t bits)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    u32* round_key = m_rd_keys;
    expand_encrypt_key_impl<CPUFeatures::X86_AES>(user_key, bits);
    for (size_t i_round = 1; i_round != m_rounds; ++i_round) {
        AK::SIMD::i64x2 ta = AK::SIMD::load_unaligned<AK::SIMD::i64x2>(&round_key[i_round * 4]);
        ta = bit_cast<AK::SIMD::i64x2>(__builtin_ia32_aesimc128(bit_cast<illx2>(ta)));
        AK::SIMD::store_unaligned(&round_key[i_round * 4], ta);
    }
}
#endif

template<>
void AESCipherKey::expand_decrypt_key_impl<CPUFeatures::None>(ReadonlyBytes user_key, size_t bits)
{
    u32* round_key;

    expand_encrypt_key(user_key, bits);

    round_key = round_keys();

    // reorder round keys
    for (size_t i = 0, j = 4 * rounds(); i < j; i += 4, j -= 4) {
        swap_keys(round_key, i, j);
        swap_keys(round_key, i + 1, j + 1);
        swap_keys(round_key, i + 2, j + 2);
        swap_keys(round_key, i + 3, j + 3);
    }

    // apply inverse mix-column to middle rounds
    for (size_t i = 1; i < rounds(); ++i) {
        round_key += 4;
        // clang-format off
        round_key[0] =
                AESTables::Decode0[AESTables::Encode1[(round_key[0] >> 24)       ] & 0xff] ^
                AESTables::Decode1[AESTables::Encode1[(round_key[0] >> 16) & 0xff] & 0xff] ^
                AESTables::Decode2[AESTables::Encode1[(round_key[0] >>  8) & 0xff] & 0xff] ^
                AESTables::Decode3[AESTables::Encode1[(round_key[0]      ) & 0xff] & 0xff] ;
        round_key[1] =
                AESTables::Decode0[AESTables::Encode1[(round_key[1] >> 24)       ] & 0xff] ^
                AESTables::Decode1[AESTables::Encode1[(round_key[1] >> 16) & 0xff] & 0xff] ^
                AESTables::Decode2[AESTables::Encode1[(round_key[1] >>  8) & 0xff] & 0xff] ^
                AESTables::Decode3[AESTables::Encode1[(round_key[1]      ) & 0xff] & 0xff] ;
        round_key[2] =
                AESTables::Decode0[AESTables::Encode1[(round_key[2] >> 24)       ] & 0xff] ^
                AESTables::Decode1[AESTables::Encode1[(round_key[2] >> 16) & 0xff] & 0xff] ^
                AESTables::Decode2[AESTables::Encode1[(round_key[2] >>  8) & 0xff] & 0xff] ^
                AESTables::Decode3[AESTables::Encode1[(round_key[2]      ) & 0xff] & 0xff] ;
        round_key[3] =
                AESTables::Decode0[AESTables::Encode1[(round_key[3] >> 24)       ] & 0xff] ^
                AESTables::Decode1[AESTables::Encode1[(round_key[3] >> 16) & 0xff] & 0xff] ^
                AESTables::Decode2[AESTables::Encode1[(round_key[3] >>  8) & 0xff] & 0xff] ^
                AESTables::Decode3[AESTables::Encode1[(round_key[3]      ) & 0xff] & 0xff] ;
        // clang-format on
    }
}

decltype(AESCipherKey::expand_encrypt_key_dispatched) AESCipherKey::expand_encrypt_key_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_AES)) {
        if (has_flag(features, CPUFeatures::X86_AES))
            return &AESCipherKey::expand_encrypt_key_impl<CPUFeatures::X86_AES>;
    }

    return &AESCipherKey::expand_encrypt_key_impl<CPUFeatures::None>;
}();

decltype(AESCipherKey::expand_decrypt_key_dispatched) AESCipherKey::expand_decrypt_key_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_AES)) {
        if (has_flag(features, CPUFeatures::X86_AES))
            return &AESCipherKey::expand_decrypt_key_impl<CPUFeatures::X86_AES>;
    }

    return &AESCipherKey::expand_decrypt_key_impl<CPUFeatures::None>;
}();

template<>
void AESCipher::encrypt_block_impl<CPUFeatures::None>(AESCipherBlock const& in, AESCipherBlock& out)
{
    u32 s0, s1, s2, s3, t0, t1, t2, t3;
    size_t r { 0 };

    auto const& dec_key = key();
    auto const* round_keys = dec_key.round_keys();

    s0 = get_key(in.bytes().offset_pointer(0)) ^ round_keys[0];
    s1 = get_key(in.bytes().offset_pointer(4)) ^ round_keys[1];
    s2 = get_key(in.bytes().offset_pointer(8)) ^ round_keys[2];
    s3 = get_key(in.bytes().offset_pointer(12)) ^ round_keys[3];

    r = dec_key.rounds() >> 1;

    // apply the first |r - 1| rounds
    for (;;) {
        // clang-format off
        t0 = AESTables::Encode0[(s0 >> 24)       ] ^
             AESTables::Encode1[(s1 >> 16) & 0xff] ^
             AESTables::Encode2[(s2 >>  8) & 0xff] ^
             AESTables::Encode3[(s3      ) & 0xff] ^ round_keys[4];
        t1 = AESTables::Encode0[(s1 >> 24)       ] ^
             AESTables::Encode1[(s2 >> 16) & 0xff] ^
             AESTables::Encode2[(s3 >>  8) & 0xff] ^
             AESTables::Encode3[(s0      ) & 0xff] ^ round_keys[5];
        t2 = AESTables::Encode0[(s2 >> 24)       ] ^
             AESTables::Encode1[(s3 >> 16) & 0xff] ^
             AESTables::Encode2[(s0 >>  8) & 0xff] ^
             AESTables::Encode3[(s1      ) & 0xff] ^ round_keys[6];
        t3 = AESTables::Encode0[(s3 >> 24)       ] ^
             AESTables::Encode1[(s0 >> 16) & 0xff] ^
             AESTables::Encode2[(s1 >>  8) & 0xff] ^
             AESTables::Encode3[(s2      ) & 0xff] ^ round_keys[7];
        // clang-format on

        round_keys += 8;
        --r;
        if (r == 0)
            break;

        // clang-format off
        s0 = AESTables::Encode0[(t0 >> 24)       ] ^
             AESTables::Encode1[(t1 >> 16) & 0xff] ^
             AESTables::Encode2[(t2 >>  8) & 0xff] ^
             AESTables::Encode3[(t3      ) & 0xff] ^ round_keys[0];
        s1 = AESTables::Encode0[(t1 >> 24)       ] ^
             AESTables::Encode1[(t2 >> 16) & 0xff] ^
             AESTables::Encode2[(t3 >>  8) & 0xff] ^
             AESTables::Encode3[(t0      ) & 0xff] ^ round_keys[1];
        s2 = AESTables::Encode0[(t2 >> 24)       ] ^
             AESTables::Encode1[(t3 >> 16) & 0xff] ^
             AESTables::Encode2[(t0 >>  8) & 0xff] ^
             AESTables::Encode3[(t1      ) & 0xff] ^ round_keys[2];
        s3 = AESTables::Encode0[(t3 >> 24)       ] ^
             AESTables::Encode1[(t0 >> 16) & 0xff] ^
             AESTables::Encode2[(t1 >>  8) & 0xff] ^
             AESTables::Encode3[(t2      ) & 0xff] ^ round_keys[3];
        // clang-format on
    }

    // apply the last round and put the encrypted data into out
    // clang-format off
    s0 = (AESTables::Encode2[(t0 >> 24)       ] & 0xff000000) ^
         (AESTables::Encode3[(t1 >> 16) & 0xff] & 0x00ff0000) ^
         (AESTables::Encode0[(t2 >>  8) & 0xff] & 0x0000ff00) ^
         (AESTables::Encode1[(t3      ) & 0xff] & 0x000000ff) ^ round_keys[0];
    out.put(0, s0);

    s1 = (AESTables::Encode2[(t1 >> 24)       ] & 0xff000000) ^
         (AESTables::Encode3[(t2 >> 16) & 0xff] & 0x00ff0000) ^
         (AESTables::Encode0[(t3 >>  8) & 0xff] & 0x0000ff00) ^
         (AESTables::Encode1[(t0      ) & 0xff] & 0x000000ff) ^ round_keys[1];
    out.put(4, s1);

    s2 = (AESTables::Encode2[(t2 >> 24)       ] & 0xff000000) ^
         (AESTables::Encode3[(t3 >> 16) & 0xff] & 0x00ff0000) ^
         (AESTables::Encode0[(t0 >>  8) & 0xff] & 0x0000ff00) ^
         (AESTables::Encode1[(t1      ) & 0xff] & 0x000000ff) ^ round_keys[2];
    out.put(8, s2);

    s3 = (AESTables::Encode2[(t3 >> 24)       ] & 0xff000000) ^
         (AESTables::Encode3[(t0 >> 16) & 0xff] & 0x00ff0000) ^
         (AESTables::Encode0[(t1 >>  8) & 0xff] & 0x0000ff00) ^
         (AESTables::Encode1[(t2      ) & 0xff] & 0x000000ff) ^ round_keys[3];
    out.put(12, s3);
    // clang-format on
}

#if AK_CAN_CODEGEN_FOR_X86_AES
template<>
[[gnu::target("aes")]] void AESCipher::encrypt_block_impl<CPUFeatures::X86_AES>(AESCipherBlock const& in, AESCipherBlock& out)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    AESCipherKey const& key = m_key;
    auto round_keys = key.round_keys();
    auto n_rounds = static_cast<int>(key.rounds());
    auto input_ptr = in.bytes().data();
    auto output_ptr = out.bytes().data();

    auto value = AK::SIMD::load_unaligned<illx2>(input_ptr);
    auto round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[(0 + 0) * 4]);
    value ^= round_key;
    for (int i_round = 0; i_round != n_rounds - 1; ++i_round) {
        round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[(1 + i_round) * 4]);
        value = __builtin_ia32_aesenc128(value, round_key);
    }
    round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[(1 + (n_rounds - 1)) * 4]);
    value = __builtin_ia32_aesenclast128(value, round_key);
    AK::SIMD::store_unaligned(output_ptr, value);
}
#endif

template<>
void AESCipher::decrypt_block_impl<CPUFeatures::None>(AESCipherBlock const& in, AESCipherBlock& out)
{
    u32 s0, s1, s2, s3, t0, t1, t2, t3;
    size_t r { 0 };

    auto const& dec_key = key();
    auto const* round_keys = dec_key.round_keys();

    s0 = get_key(in.bytes().offset_pointer(0)) ^ round_keys[0];
    s1 = get_key(in.bytes().offset_pointer(4)) ^ round_keys[1];
    s2 = get_key(in.bytes().offset_pointer(8)) ^ round_keys[2];
    s3 = get_key(in.bytes().offset_pointer(12)) ^ round_keys[3];

    r = dec_key.rounds() >> 1;

    // apply the first |r - 1| rounds
    for (;;) {
        // clang-format off
        t0 = AESTables::Decode0[(s0 >> 24)       ] ^
             AESTables::Decode1[(s3 >> 16) & 0xff] ^
             AESTables::Decode2[(s2 >>  8) & 0xff] ^
             AESTables::Decode3[(s1      ) & 0xff] ^ round_keys[4];
        t1 = AESTables::Decode0[(s1 >> 24)       ] ^
             AESTables::Decode1[(s0 >> 16) & 0xff] ^
             AESTables::Decode2[(s3 >>  8) & 0xff] ^
             AESTables::Decode3[(s2      ) & 0xff] ^ round_keys[5];
        t2 = AESTables::Decode0[(s2 >> 24)       ] ^
             AESTables::Decode1[(s1 >> 16) & 0xff] ^
             AESTables::Decode2[(s0 >>  8) & 0xff] ^
             AESTables::Decode3[(s3      ) & 0xff] ^ round_keys[6];
        t3 = AESTables::Decode0[(s3 >> 24)       ] ^
             AESTables::Decode1[(s2 >> 16) & 0xff] ^
             AESTables::Decode2[(s1 >>  8) & 0xff] ^
             AESTables::Decode3[(s0      ) & 0xff] ^ round_keys[7];
        // clang-format on

        round_keys += 8;
        --r;
        if (r == 0)
            break;

        // clang-format off
        s0 = AESTables::Decode0[(t0 >> 24)       ] ^
             AESTables::Decode1[(t3 >> 16) & 0xff] ^
             AESTables::Decode2[(t2 >>  8) & 0xff] ^
             AESTables::Decode3[(t1      ) & 0xff] ^ round_keys[0];
        s1 = AESTables::Decode0[(t1 >> 24)       ] ^
             AESTables::Decode1[(t0 >> 16) & 0xff] ^
             AESTables::Decode2[(t3 >>  8) & 0xff] ^
             AESTables::Decode3[(t2      ) & 0xff] ^ round_keys[1];
        s2 = AESTables::Decode0[(t2 >> 24)       ] ^
             AESTables::Decode1[(t1 >> 16) & 0xff] ^
             AESTables::Decode2[(t0 >>  8) & 0xff] ^
             AESTables::Decode3[(t3      ) & 0xff] ^ round_keys[2];
        s3 = AESTables::Decode0[(t3 >> 24)       ] ^
             AESTables::Decode1[(t2 >> 16) & 0xff] ^
             AESTables::Decode2[(t1 >>  8) & 0xff] ^
             AESTables::Decode3[(t0      ) & 0xff] ^ round_keys[3];
        // clang-format on
    }

    // apply the last round and put the decrypted data into out
    // clang-format off
    s0 = ((u32)AESTables::Decode4[(t0 >> 24)       ] << 24) ^
         ((u32)AESTables::Decode4[(t3 >> 16) & 0xff] << 16) ^
         ((u32)AESTables::Decode4[(t2 >>  8) & 0xff] <<  8) ^
         ((u32)AESTables::Decode4[(t1      ) & 0xff]      ) ^ round_keys[0];
    out.put(0, s0);

    s1 = ((u32)AESTables::Decode4[(t1 >> 24)       ] << 24) ^
         ((u32)AESTables::Decode4[(t0 >> 16) & 0xff] << 16) ^
         ((u32)AESTables::Decode4[(t3 >>  8) & 0xff] <<  8) ^
         ((u32)AESTables::Decode4[(t2      ) & 0xff]      ) ^ round_keys[1];
    out.put(4, s1);

    s2 = ((u32)AESTables::Decode4[(t2 >> 24)       ] << 24) ^
         ((u32)AESTables::Decode4[(t1 >> 16) & 0xff] << 16) ^
         ((u32)AESTables::Decode4[(t0 >>  8) & 0xff] <<  8) ^
         ((u32)AESTables::Decode4[(t3      ) & 0xff]      ) ^ round_keys[2];
    out.put(8, s2);

    s3 = ((u32)AESTables::Decode4[(t3 >> 24)       ] << 24) ^
         ((u32)AESTables::Decode4[(t2 >> 16) & 0xff] << 16) ^
         ((u32)AESTables::Decode4[(t1 >>  8) & 0xff] <<  8) ^
         ((u32)AESTables::Decode4[(t0      ) & 0xff]      ) ^ round_keys[3];
    out.put(12, s3);
    // clang-format on
}

#if AK_CAN_CODEGEN_FOR_X86_AES
template<>
[[gnu::target("aes")]] void AESCipher::decrypt_block_impl<CPUFeatures::X86_AES>(AESCipherBlock const& in, AESCipherBlock& out)
{
    using illx2 = signed long long int __attribute__((vector_size(16)));

    AESCipherKey const& key = m_key;
    auto round_keys = key.round_keys();
    auto n_rounds = static_cast<int>(key.rounds());
    auto input_ptr = in.bytes().data();
    auto output_ptr = out.bytes().data();

    auto value = AK::SIMD::load_unaligned<illx2>(input_ptr);
    auto round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[n_rounds * 4]);
    value ^= round_key;
    for (int i_round = 0; i_round != n_rounds - 1; ++i_round) {
        int const idx = (n_rounds - 1) - i_round;
        round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[idx * 4]);
        value = __builtin_ia32_aesdec128(value, round_key);
    }
    round_key = AK::SIMD::load_unaligned<illx2>(&round_keys[0]);
    value = __builtin_ia32_aesdeclast128(value, round_key);
    AK::SIMD::store_unaligned(output_ptr, value);
}
#endif

decltype(AESCipher::encrypt_block_dispatched) AESCipher::encrypt_block_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_AES)) {
        if (has_flag(features, CPUFeatures::X86_AES))
            return &AESCipher::encrypt_block_impl<CPUFeatures::X86_AES>;
    }

    return &AESCipher::encrypt_block_impl<CPUFeatures::None>;
}();

decltype(AESCipher::decrypt_block_dispatched) AESCipher::decrypt_block_dispatched = [] {
    CPUFeatures features = detect_cpu_features();

    if constexpr (is_valid_feature(CPUFeatures::X86_AES)) {
        if (has_flag(features, CPUFeatures::X86_AES))
            return &AESCipher::decrypt_block_impl<CPUFeatures::X86_AES>;
    }

    return &AESCipher::decrypt_block_impl<CPUFeatures::None>;
}();

void AESCipherBlock::overwrite(ReadonlyBytes bytes)
{
    auto data = bytes.data();
    auto length = bytes.size();

    VERIFY(length <= this->data_size());
    this->bytes().overwrite(0, data, length);
    if (length < this->data_size()) {
        switch (padding_mode()) {
        case PaddingMode::Null:
            // fill with zeros
            __builtin_memset(m_data + length, 0, this->data_size() - length);
            break;
        case PaddingMode::CMS:
            // fill with the length of the padding bytes
            __builtin_memset(m_data + length, this->data_size() - length, this->data_size() - length);
            break;
        case PaddingMode::RFC5246:
            // fill with the length of the padding bytes minus one
            __builtin_memset(m_data + length, this->data_size() - length - 1, this->data_size() - length);
            break;
        default:
            // FIXME: We should handle the rest of the common padding modes
            VERIFY_NOT_REACHED();
            break;
        }
    }
}

}
