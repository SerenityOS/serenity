/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MLKEM.h"

#include <AK/Array.h>
#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <AK/Math/Rounding.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibCrypto/Hash/SHA3.h>

namespace Crypto::KEM::MLKEM {

namespace {

// 7. The ML-KEM Key-Encapsulation Mechanism
constexpr u32 ML_KEM_N = 256;
constexpr u32 ML_KEM_Q = 3329;

using Polynomial = Array<u16, ML_KEM_N>;

struct ParameterSet {
    u32 k {};
    u32 eta_1 {};
    u32 eta_2 {};
    u32 d_u {};
    u32 d_v {};
};

// 8. Parameter Sets
constexpr ParameterSet PARAMS_512 = {
    .k = 2,
    .eta_1 = 3,
    .eta_2 = 2,
    .d_u = 10,
    .d_v = 4,
};

// 8. Parameter Sets
constexpr ParameterSet PARAMS_768 = {
    .k = 3,
    .eta_1 = 2,
    .eta_2 = 2,
    .d_u = 10,
    .d_v = 4,
};

// 8. Parameter Sets
constexpr ParameterSet PARAMS_1024 = {
    .k = 4,
    .eta_1 = 2,
    .eta_2 = 2,
    .d_u = 11,
    .d_v = 5,
};

// 4.2.1 Conversion and Compression Algorithms
// Algorithm 5 ByteEncode (𝐹)
ByteBuffer ByteEncode(u8 d, Polynomial const& F)
{
    VERIFY(d >= 1 && d <= 12);
    // 𝐵 ∈ 𝔹32𝑑
    ByteBuffer B;
    B.resize(32u * d);
    Bitmap b(B.data(), B.size() * 8);

    for (u32 i = 0; i < 256; ++i) {
        u32 a = F[i];
        for (u32 j = 0; j < d; ++j) {
            // 𝑏[𝑖 ⋅ 𝑑 + 𝑗] ← 𝑎 mod 2
            b.set(i * d + j, mod(a, 2));
            // 𝑎 ← (𝑎 − 𝑏[𝑖 ⋅ 𝑑 + 𝑗])/2
            a = (a - b.get(i * d + j)) / 2;
        }
    }
    return B;
}

// 4.2.1 Conversion and Compression Algorithms
// Algorithm 6 ByteDecode𝑑(𝐵)
Polynomial ByteDecode(u8 d, ReadonlyBytes B)
{
    VERIFY(d >= 1 && d <= 12);
    VERIFY(B.size() == 32 * d);
    BitmapView b(const_cast<u8*>(B.data()), B.size() * 8);
    Polynomial F {};
    auto m = d < 12 ? 1u << d : ML_KEM_Q;
    for (u32 i = 0; i < ML_KEM_N; ++i) {
        F[i] = mod([&] {
            u16 sum = 0;
            for (u8 j = 0; j < d; ++j)
                sum += b.get(i * d + j) * (1u << j);
            return sum;
        }(),
            m);
    }
    return F;
}

// 7.2 ML-KEM Encapsulation
bool encapsulation_key_check(ParameterSet const& params, ReadonlyBytes encapsulation_key)
{
    // "1. (Type check) If ek is not an array of bytes of length 384𝑘 + 32 for the value of 𝑘 specified
    // by the relevant parameter set, then input checking failed."
    if (encapsulation_key.size() != 384 * params.k + 32)
        return false;

    // "2. (Modulus check) Perform the computation
    //     test ← ByteEncode12(ByteDecode12(ek[0 ∶ 384𝑘]))
    //  If test ≠ ek[0 ∶ 384𝑘], then input checking failed. This check ensures
    //  that the integers encoded in the public key are in the valid range [0, 𝑞 − 1]."
    auto seedless_ek = encapsulation_key.trim(384 * params.k);
    ByteBuffer test;
    for (u8 i = 0; i < params.k; ++i)
        test.append(ByteEncode(12, ByteDecode(12, seedless_ek.slice(i * 384, 384))));

    AK_IGNORE_DIAGNOSTIC("-Wstringop-overread",
        if (test.span() != seedless_ek) return false;)
    return true;
}

// 4.1 Cryptographic Functions
// H(𝑠) ∶= SHA3-256(𝑠)
auto H(ReadonlyBytes s)
{
    return Hash::SHA3_256::hash(StringView { s });
}

// 4.1 Cryptographic Functions
// G(𝑐) ∶= SHA3-512(𝑐)
// "The two outputs of G will be denoted by (𝑎, 𝑏) ← G(𝑐), where
// 𝑎, 𝑏 ∈ 𝔹32 , 𝑐 ∈ 𝔹∗ , and G(𝑐) = 𝑎‖𝑏."
struct A_B {
    Array<u8, 32> A;
    Array<u8, 32> B;
};

A_B G(ReadonlyBytes s)
{
    auto hash = Hash::SHA3_512::hash(StringView { s });
    VERIFY(hash.bytes().size() == 64);
    auto a = Array<u8, 32>::from_span(hash.bytes().trim(32));
    auto b = Array<u8, 32>::from_span(hash.bytes().slice_from_end(32));
    return { a, b };
}

// 4.1 Cryptographic Functions
// PRF𝜂(𝑠, 𝑏) ∶= SHAKE256(𝑠‖𝑏, 8 ⋅ 64 ⋅ 𝜂)
auto PRF(u8 eta, ReadonlyBytes s, u8 b)
{
    Array<u8, 33> concatenated;
    VERIFY(s.size() == concatenated.size() - 1);
    s.copy_to(concatenated.span());
    concatenated.last() = b;

    Hash::SHAKE256 hash;
    hash.update(concatenated.span());
    return hash.peek_with_size(64 * eta);
}

// 4.2.2 Sampling Algorithms
// Algorithm 8 SamplePolyCBD (𝐵)
Polynomial SamplePolyCBD(u8 eta, ReadonlyBytes B)
{
    Polynomial f {};

    // 1: 𝑏 ← BytesToBits(𝐵)
    BitmapView b(const_cast<u8*>(B.data()), B.size() * 8);

    // 2: for (𝑖 ← 0; 𝑖 < 256; 𝑖++)
    for (u32 i = 0; i < ML_KEM_N; ++i) {
        // 3: 𝑥 ← ∑𝜂−1 𝑗←0 𝑏[2𝑖𝜂 + 𝑗]
        auto x = [&] {
            i32 result = 0;
            for (u8 j = 0; j < eta; ++j)
                result += b.get(2 * i * eta + j);
            return result;
        }();

        // 4: 𝑦 ← ∑𝜂−1 𝑗←0 𝑏[2𝑖𝜂 + 𝜂 + 𝑗]
        auto y = [&] {
            i32 result = 0;
            for (u8 j = 0; j < eta; ++j)
                result += b.get(2 * i * eta + eta + j);
            return result;
        }();

        // 5: 𝑓[𝑖] ← 𝑥 − 𝑦 mod 𝑞
        f[i] = mod(x - y, ML_KEM_Q);
    }

    return f;
}

// 4.2.2 Sampling Algorithms
// Algorithm 7 SampleNTT(𝐵)
Polynomial SampleNTT(ReadonlyBytes B)
{
    Polynomial a {}; // 𝑎 ∈ ℤ256𝑞

    // 1: ctx ← XOF.Init()
    Hash::SHAKE128 hash;

    // 2: ctx ← XOF.Absorb(ctx, 𝐵)
    hash.update(B);

    // 3: 𝑗 ← 0
    u32 j = 0;

    // 5: (ctx, 𝐶) ← XOF.Squeeze(ctx, 3)
    // FIXME: Make the SHAKE* API compatible with this use case.
    // FIXME: Our squeeze (peek) doesn't modify the context, so we need to
    //        squeeze everything at once rather than 3 bytes at a time. The
    //        loop bellow is unbounded so this could fail with a maliciously
    //        crafted input. In practice, according to Appendix B — SampleNTT
    //        Loop Bounds, the loop should exit in less than 280 iteration, so
    //        use this as an upper bound.
    //        The authors of https://cryptojedi.org/papers/terminate-20230516.pdf
    //        offer a dinner at a nice looking restaurant if you find an input that
    //        requires more than 280 iterations.
    auto C_total = hash.peek_with_size(3 * 280);
    u32 squeeze_number = 0;

    // 4: while 𝑗 < 256 do
    while (j < 256) {
        auto C = C_total.bytes().slice(3 * squeeze_number, 3);
        squeeze_number++;
        // 6: 𝑑1 ← 𝐶[0] + 256 ⋅ (𝐶[1] mod 16)
        auto d1 = C[0] + 256u * mod(C[1], 16);
        // 7: 𝑑2 ← ⌊𝐶[1]/16⌋ + 16 ⋅ 𝐶[2]
        auto d2 = floor_div(C[1], 16) + 16u * C[2];

        // 8: if 𝑑1 < 𝑞 then
        if (d1 < ML_KEM_Q) {
            // 9: 𝑎[𝑗] ← 𝑑1
            a[j] = d1;
            // 10: 𝑗 ← 𝑗 + 1
            j = j + 1;
            // 11: end if
        }

        // 12: if 𝑑2 < 𝑞 and 𝑗 < 256 then
        if (d2 < ML_KEM_Q && j < 256) {
            // 13: 𝑎[𝑗] ← 𝑑2
            a[j] = d2;
            // 14: 𝑗 ← 𝑗 + 1
            j = j + 1;
        }
    }

    // 17: return 𝑎
    return a;
}

// Appendix A — Precomputed Values for the NTT
// NOTE: Computing the values live would require using BigInt,
//       and friends, so let's just use the array from the spec.
Array zeta_bit_rev7_mod_q = to_array<u16>({
    // clang-format off
    1, 1729, 2580, 3289, 2642, 630, 1897, 848,
    1062, 1919, 193, 797, 2786, 3260, 569, 1746,
    296, 2447, 1339, 1476, 3046, 56, 2240, 1333,
    1426, 2094, 535, 2882, 2393, 2879, 1974, 821,
    289, 331, 3253, 1756, 1197, 2304, 2277, 2055,
    650, 1977, 2513, 632, 2865, 33, 1320, 1915,
    2319, 1435, 807, 452, 1438, 2868, 1534, 2402,
    2647, 2617, 1481, 648, 2474, 3110, 1227, 910,
    17, 2761, 583, 2649, 1637, 723, 2288, 1100,
    1409, 2662, 3281, 233, 756, 2156, 3015, 3050,
    1703, 1651, 2789, 1789, 1847, 952, 1461, 2687,
    939, 2308, 2437, 2388, 733, 2337, 268, 641,
    1584, 2298, 2037, 3220, 375, 2549, 2090, 1645,
    1063, 319, 2773, 757, 2099, 561, 2466, 2594,
    2804, 1092, 403, 1026, 1143, 2150, 2775, 886,
    1722, 1212, 1874, 1029, 2110, 2935, 885, 2154,
    // clang-format on
});

// Appendix A — Precomputed Values for the NTT
Array zeta_2_bit_rev7_plus_1_mod_q = to_array<i16>({
    // clang-format off
    17, -17, 2761, -2761, 583, -583, 2649, -2649,
    1637, -1637, 723, -723, 2288, -2288, 1100, -1100,
    1409, -1409, 2662, -2662, 3281, -3281, 233, -233,
    756, -756, 2156, -2156, 3015, -3015, 3050, -3050,
    1703, -1703, 1651, -1651, 2789, -2789, 1789, -1789,
    1847, -1847, 952, -952, 1461, -1461, 2687, -2687,
    939, -939, 2308, -2308, 2437, -2437, 2388, -2388,
    733, -733, 2337, -2337, 268, -268, 641, -641,
    1584, -1584, 2298, -2298, 2037, -2037, 3220, -3220,
    375, -375, 2549, -2549, 2090, -2090, 1645, -1645,
    1063, -1063, 319, -319, 2773, -2773, 757, -757,
    2099, -2099, 561, -561, 2466, -2466, 2594, -2594,
    2804, -2804, 1092, -1092, 403, -403, 1026, -1026,
    1143, -1143, 2150, -2150, 2775, -2775, 886, -886,
    1722, -1722, 1212, -1212, 1874, -1874, 1029, -1029,
    2110, -2110, 2935, -2935, 885, -885, 2154, -2154,
    // clang-format on
});

// 4.3 The Number-Theoretic Transform
// Algorithm 9 NTT(𝑓)
Polynomial NTT(ReadonlySpan<u16> f)
{
    // 1: 𝑓^ ← 𝑓
    auto f_hat = Polynomial::from_span(f);

    // 2: 𝑖 ← 1
    u32 i = 1;

    // 3: for (len ← 128; len ≥ 2; len ← len/2)
    for (u32 len = 128; len >= 2; len = len / 2) {
        // 4: for (start ← 0; start < 256; start ← start + 2 ⋅ len)
        for (u32 start = 0; start < 256; start += 2 * len) {
            // 5: zeta ← 𝜁BitRev7(𝑖) mod 𝑞
            auto zeta = zeta_bit_rev7_mod_q[i];
            // 6: 𝑖 ← 𝑖 + 1
            i = i + 1;
            // 7: for (𝑗 ← start; 𝑗 < start + len; 𝑗++)
            for (u32 j = start; j < start + len; ++j) {
                // ▷ steps 8-10 done modulo 𝑞
                // 8: 𝑡 ← zeta ⋅ 𝑓^[𝑗 + len]
                auto t = mod(zeta * f_hat[j + len], ML_KEM_Q);
                // 9: 𝑓^[𝑗 + len] ← 𝑓^[𝑗] − 𝑡
                f_hat[j + len] = mod(f_hat[j] - t, ML_KEM_Q);
                // 10: 𝑓^[𝑗] ← 𝑓^[𝑗] + 𝑡
                f_hat[j] = mod(f_hat[j] + t, ML_KEM_Q);
            }
        }
    }

    return f_hat;
}

// 4.3 The Number-Theoretic Transform
// Algorithm 10 NTT-1(𝑓^)
Polynomial NTT_inverse(ReadonlySpan<u16> f_hat)
{
    // 1: 𝑓 ← 𝑓̂
    auto f = Polynomial::from_span(f_hat);

    // 2: 𝑖 ← 127
    u32 i = 127;
    // 3: for (len ← 2; len ≤ 128; len ← 2 ⋅ len)
    for (u32 len = 2; len <= 128; len = 2 * len) {
        // 4: for (start ← 0; start < 256; start ← start + 2 ⋅ len)
        for (u32 start = 0; start < 256; start += 2 * len) {
            // 5: zeta ← 𝜁BitRev7(𝑖) mod 𝑞
            i32 zeta = zeta_bit_rev7_mod_q[i];
            // 6: 𝑖 ← 𝑖 − 1
            i = i - 1;
            // 7: for (𝑗 ← start; 𝑗 < start + len; 𝑗++)
            for (u32 j = start; j < start + len; ++j) {
                // 8: 𝑡 ← 𝑓[𝑗]
                i32 t = f[j];
                // ▷ steps 9-10 done modulo 𝑞
                // 9: 𝑓[𝑗] ← 𝑡 + 𝑓[𝑗 + len]
                f[j] = mod(t + f[j + len], ML_KEM_Q);
                // 10: 𝑓[𝑗 + len] ← zeta ⋅ (𝑓[𝑗 + len] − 𝑡)
                f[j + len] = mod(zeta * (f[j + len] - t), ML_KEM_Q);
            }
        }
    }

    // 14: 𝑓 ← 𝑓 ⋅ 3303 mod 𝑞
    for (auto& val : f)
        val = mod(val * 3303, ML_KEM_Q);

    // 15: return 𝑓
    return f;
}

// 4.2.1 Conversion and Compression Algorithms
// Compression and decompression.
Polynomial Compress(u8 d, Polynomial const& x)
{
    // 𝑥 ⟼ ⌈(2𝑑/𝑞) ⋅ 𝑥⌋ mod 2𝑑
    Polynomial result = x;
    for (auto& value : result)
        value = mod(round_to<u16>(static_cast<double>(value) * (1u << d) / ML_KEM_Q), (1u << d));
    return result;
}

// 4.2.1 Conversion and Compression Algorithms
// Compression and decompression.
Polynomial Decompress(u8 d, Polynomial const& y)
{
    VERIFY(d < 12);
    // 𝑦 ⟼ ⌈(𝑞/2𝑑) ⋅ 𝑦⌋
    Polynomial result = y;
    // From the definition of ⌈𝑥⌋ (2.3 Mathematical Symbols):
    // "The rounding of 𝑥 to the nearest integer. If 𝑥 = 𝑦 + 1/2
    // for some 𝑦 ∈ ℤ, then ⌈𝑥⌋ = 𝑦 + 1."
    for (auto& value : result)
        value = static_cast<u16>((static_cast<double>(value) * ML_KEM_Q + (1u << (d - 1))) / (1u << d));
    return result;
}

// 2.4.5 Arithmetic With Polynomials and NTT Representations
// "The algebraic operations of addition and scalar multiplication
// in 𝑅𝑞 and 𝑇𝑞 are done coordinate-wise. For example, if 𝑎 ∈ ℤ𝑞
// and 𝑓 ∈ 𝑅𝑞, the 𝑖-th coefficient of the polynomial 𝑎 ⋅ 𝑓 ∈ 𝑅𝑞 is
// equal to 𝑎 ⋅ 𝑓𝑖 mod 𝑞."
Polynomial polynomial_add_NTT(Polynomial const& v1, ReadonlySpan<u16> v2)
{
    VERIFY(v1.size() == v2.size());
    Polynomial result {};

    for (u32 i = 0; i < v1.size(); ++i)
        result[i] = mod(v1[i] + v2[i], ML_KEM_Q);

    return result;
}

// 4.3.1 Multiplication in the NTT Domain
// Algorithm 12 BaseCaseMultiply(𝑎0, 𝑎1, 𝑏0, 𝑏1, 𝛾)
struct C0C1 {
    u16 c0 {};
    u16 c1 {};
};

C0C1 BaseCaseMultiply(i64 a0, i64 a1, i64 b0, i64 b1, i16 gamma)
{
    // ▷ steps 1-2 done modulo 𝑞
    // 1: 𝑐0 ← 𝑎0 ⋅ 𝑏0 + 𝑎1 ⋅ 𝑏1 ⋅ 𝛾
    u16 c0 = mod(a0 * b0 + a1 * b1 * gamma, ML_KEM_Q);
    // 2: 𝑐1 ← 𝑎0 ⋅ 𝑏1 + 𝑎1 ⋅ 𝑏0
    u16 c1 = mod(a0 * b1 + a1 * b0, ML_KEM_Q);
    return { c0, c1 };
}

// 4.3.1 Multiplication in the NTT Domain
// Algorithm 11 MultiplyNTTs(𝑓, 𝑔)
Polynomial MultiplyNTTs(Polynomial const& f, Polynomial const& g)
{
    Polynomial h {};
    // 1: for (𝑖 ← 0; 𝑖 < 128; 𝑖++)
    for (u8 i = 0; i < 128; ++i) {
        // 2: (ℎ[2𝑖], ℎ[2𝑖 + 1]) ← BaseCaseMultiply(𝑓[2𝑖], 𝑓[2𝑖 + 1], 𝑔[2𝑖], 𝑔[2𝑖 + 1], 𝜁2BitRev7(𝑖)+1)
        auto [c0, c1] = BaseCaseMultiply(f[2 * i], f[2 * i + 1], g[2 * i], g[2 * i + 1], zeta_2_bit_rev7_plus_1_mod_q[i]);
        h[2 * i] = c0;
        h[2 * i + 1] = c1;
    }

    return h;
}

// 2.4.7 Arithmetic With Matrices and Vectors
Polynomial vector_product_NTT(u32 k, ReadonlySpan<Polynomial> u, ReadonlySpan<Polynomial> v)
{
    VERIFY(u.size() == v.size());
    VERIFY(u.size() == k);

    // This implements equation (2.14).

    Polynomial result {};

    for (u32 j = 0; j < k; ++j)
        result = polynomial_add_NTT(result, MultiplyNTTs(u[j], v[j]));

    return result;
}

void transpose(u32 k, Vector<Polynomial>& A)
{
    VERIFY(A.size() == k * k);
    for (u32 i = 0; i < k; ++i) {
        for (u32 j = 0; j < i; ++j) {
            for (u32 n = 0; n < ML_KEM_N; ++n)
                swap(A[i * k + j][n], A[j * k + i][n]);
        }
    }
}

// 5.2 K-PKE Encryption
// Algorithm 14 K-PKE.Encrypt(ekPKE, 𝑚, 𝑟)
ByteBuffer K_PKE_Encrypt(ParameterSet const& params, ReadonlyBytes encapsulation_key, ReadonlyBytes m, ReadonlyBytes r)
{
    u32 N = 0;

    // ▷ run ByteDecode12 𝑘 times to decode 𝐭 ∈ (ℤ256𝑞)𝑘
    // 2: 𝐭 ← ByteDecode12(ekPKE[0 ∶ 384𝑘])
    Vector<Polynomial> t;
    t.ensure_capacity(params.k);
    for (u8 i = 0; i < params.k; ++i) {
        t.append(ByteDecode(12, encapsulation_key.slice(i * 384, 384)));
    }

    // 3: 𝜌 ← ekPKE[384𝑘 ∶ 384𝑘 + 32]
    auto rho = encapsulation_key.slice_from_end(32);

    // ▷ re-generate matrix 𝐀 ∈ (ℤ256𝑞)𝑘×𝑘 sampled in Alg. 13
    Vector<Polynomial> A;
    A.resize(params.k * params.k);
    Array<u8, 34> base;
    AK::TypedTransfer<u8>::copy(base.data(), rho.data(), rho.size());
    for (u8 i = 0; i < params.k; ++i) {
        for (u8 j = 0; j < params.k; ++j) {
            // ▷ 𝑗 and 𝑖 are bytes 33 and 34 of the input
            auto input = base;
            input[32] = j;
            input[33] = i;
            // 6: 𝐀[𝑖, 𝑗] ← SampleNTT(𝜌‖𝑗‖𝑖)
            A[i * params.k + j] = SampleNTT(input);
        }
    }

    // ▷ generate 𝐲 ∈ (ℤ256𝑞)𝑘
    Vector<Polynomial> y;
    y.resize(params.k);
    for (u8 i = 0; i < params.k; i++) {
        // 10: 𝐲[𝑖] ← SamplePolyCBD𝜂1(PRF𝜂1 (𝑟, 𝑁 ))
        y[i] = SamplePolyCBD(params.eta_1, PRF(params.eta_1, r, N));
        N++;
    }

    // ▷ generate 𝐞𝟏 ∈ (ℤ256𝑞)𝑘
    Vector<Polynomial> e1;
    e1.resize(params.k);
    for (u8 i = 0; i < params.k; ++i) {
        // 14: 𝐞𝟏[𝑖] ← SamplePolyCBD𝜂2(PRF𝜂2 (𝑟, 𝑁 ))
        e1[i] = SamplePolyCBD(params.eta_2, PRF(params.eta_2, r, N));
        N++;
    }

    // ▷ sample 𝑒2 ∈ ℤ256 from CBD
    // 17: 𝑒2 ← SamplePolyCBD𝜂2(PRF𝜂2 (𝑟, 𝑁 ))
    auto e2 = SamplePolyCBD(params.eta_2, PRF(params.eta_2, r, N));

    // ▷ run NTT 𝑘 times
    // 18: 𝐲^ ← NTT(𝐲)
    auto y_hat = y;
    for (u8 i = 0; i < params.k; ++i) {
        y_hat[i] = NTT(y[i]);
    }

    // ▷ run NTT−1 𝑘 times
    // 19: 𝐮 ← NTT−1(𝐀̂⊺ ∘ 𝐲) + 𝐞𝟏
    transpose(params.k, A);
    auto u = y;
    for (u8 i = 0; i < params.k; ++i) {
        auto product = vector_product_NTT(params.k, A.span().slice(i * params.k, params.k), y_hat);
        u[i] = polynomial_add_NTT(NTT_inverse(product), e1[i]);
    }

    // 20: 𝜇 ← Decompress1(ByteDecode1(𝑚))
    auto mu = Decompress(1, ByteDecode(1, m));

    // ▷ encode plaintext 𝑚 into polynomial 𝑣
    Polynomial v {};
    // 21: 𝑣 ← NTT−1(𝐭^⊺ ∘ 𝐲^) + 𝑒2 + 𝜇
    v = polynomial_add_NTT(polynomial_add_NTT(NTT_inverse(vector_product_NTT(params.k, t, y_hat)), e2), mu);

    // ▷ run ByteEncode𝑑𝑢 and Compress𝑑𝑢 𝑘 times
    // 22: 𝑐1 ← ByteEncode𝑑𝑢(Compress𝑑𝑢(𝐮))
    ByteBuffer c1 {};
    c1.ensure_capacity(2 * params.k * ML_KEM_N);
    for (u8 i = 0; i < params.k; ++i)
        c1.append(ByteEncode(params.d_u, Compress(params.d_u, u[i])));

    // 23: 𝑐2 ← ByteEncode𝑑𝑣(Compress𝑑𝑣(𝑣))
    auto c2 = ByteEncode(params.d_v, Compress(params.d_v, v));

    // 24: return 𝑐 ← (𝑐1‖𝑐2)
    c1.append(c2);
    return c1;
}

ParameterSet const& parameters_from_variation(Variation variation)
{
    switch (variation) {
    case Variation::ML_KEM_512:
        return PARAMS_512;
    case Variation::ML_KEM_768:
        return PARAMS_768;
    case Variation::ML_KEM_1024:
        return PARAMS_1024;
    }
    VERIFY_NOT_REACHED();
}

}

namespace Detail {

// 6.2 Internal Encapsulation
// Algorithm 17 ML-KEM.Encaps_internal(ek, 𝑚)
Result encapsulate_internal(Variation variation, ReadonlyBytes encapsulation_key, ReadonlyBytes m)
{
    auto const& params = parameters_from_variation(variation);

    ByteBuffer concatenated;
    concatenated.append(m);
    concatenated.append(H(encapsulation_key).bytes());
    auto [K, r] = G(concatenated); // derive shared secret key 𝐾 and randomness 𝑟

    auto c = K_PKE_Encrypt(params, encapsulation_key, m, r); // encrypt 𝑚 using K-PKE with randomness 𝑟
    return { K, c };
}

}

// 7.2 ML-KEM Encapsulation
// Algorithm 20 ML-KEM.Encaps(ek)
ErrorOr<Result> encapsulate(Variation variation, ReadonlyBytes encapsulation_key)
{
    auto const& params = parameters_from_variation(variation);

    if (!encapsulation_key_check(params, encapsulation_key))
        return Error::from_string_literal("ML-KEM: Invalid encapsulation key");

    // FIXME: Ensure the randomness source is of good quality.
    //        This is covered in Section 3.3. Return an error
    //        if randomness isn't good enough.
    Array<u8, 32> m;
    fill_with_random(m);

    return Detail::encapsulate_internal(variation, encapsulation_key, m);
}

} // Crypto::KEM::MLKEM
