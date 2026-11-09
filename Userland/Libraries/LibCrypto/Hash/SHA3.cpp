/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SHA3.h"
#include <AK/Bitmap.h>

namespace Crypto::Hash::SHA3 {

namespace {

constexpr void XOR(Bytes input1, ReadonlyBytes input2)
{
    VERIFY(input1.size() >= input2.size());
    for (u32 i = 0; i < input2.size(); i++)
        input1[i] ^= input2[i];
}

// Defined in "3.1 State" for b=1600.
constexpr u32 KECCAK_L = 6;
constexpr u32 KECCAK_W = 64;

// 3.1.2 Converting Strings to State Arrays
constexpr bool state_get(State const& state, u8 x, u8 y, u8 z)
{
    // A[x, y, z] = S [w(5y + x) + z]
    auto bit_index = KECCAK_W * (5 * y + x) + z;
    Bitmap bitmap { const_cast<u8*>(state.data()), state.size() * 8 };
    return bitmap.get(bit_index);
}

constexpr void state_set(State& state, u8 x, u8 y, u8 z, bool value)
{
    // A[x, y, z] = S [w(5y + x) + z]
    auto bit_index = KECCAK_W * (5 * y + x) + z;
    Bitmap bitmap { state.data(), state.size() * 8 };
    bitmap.set(bit_index, value);
}

// 3.2.1 Specification of θ
constexpr State theta(State const& A)
{
    // 1. For all pairs (x, z) such that 0 ≤ x < 5 and 0 ≤ z < w, let
    //        C[x, z] = A[x, 0, z] ⊕ A[x, 1, z] ⊕ A[x, 2, z] ⊕ A[x, 3, z] ⊕ A[x, 4, z]
    State C {};
    for (u8 x = 0; x < 5; x++) {
        for (u8 z = 0; z < KECCAK_W; ++z) {
            bool value = false;
            for (u8 y = 0; y < 5; y++)
                value ^= state_get(A, x, y, z);
            state_set(C, x, 0, z, value);
        }
    }

    // 2. For all pairs (x, z) such that 0 ≤ x < 5 and 0 ≤ z < w let
    //        D[x, z] = C[(x - 1) mod 5, z] ⊕ C[(x+1) mod 5, (z – 1) mod w].
    State D {};
    for (u8 x = 0; x < 5; x++) {
        for (u8 z = 0; z < KECCAK_W; ++z) {
            bool value = state_get(C, mod((x - 1), 5), 0, z);
            value ^= state_get(C, mod((x + 1), 5), 0, mod((z - 1), KECCAK_W));
            state_set(D, x, 0, z, value);
        }
    }

    State A_prime {};
    // 3. For all triples (x, y, z) such that 0 ≤ x < 5, 0 ≤ y < 5, and 0 ≤ z < w, let
    //        A′[x, y, z] = A[x, y, z] ⊕ D[x, z].
    for (u8 x = 0; x < 5; x++) {
        for (u8 y = 0; y < 5; y++) {
            for (u8 z = 0; z < KECCAK_W; ++z) {
                bool value = state_get(A, x, y, z);
                value ^= state_get(D, x, 0, z);
                state_set(A_prime, x, y, z, value);
            }
        }
    }

    return A_prime;
}

// 3.2.2 Specification of ρ
constexpr State rho(State const& A)
{
    State A_prime {};
    // 1. For all z such that 0 ≤ z < w, let A′ [0, 0, z] = A[0, 0, z].
    for (u8 z = 0; z < KECCAK_W; ++z)
        state_set(A_prime, 0, 0, z, state_get(A, 0, 0, z));

    // 2. Let (x, y) = (1, 0).
    u32 x = 1;
    u32 y = 0;

    // 3. For t from 0 to 23:
    for (u32 t = 0; t <= 23; ++t) {
        // a. for all z such that 0 ≤ z < w, let A′[x, y, z] = A[x, y, (z – (t + 1)(t + 2)/2) mod w];
        for (u8 z = 0; z < KECCAK_W; ++z) {
            bool value = state_get(A, x, y, mod(z - (t + 1) * (t + 2) / 2, KECCAK_W));
            state_set(A_prime, x, y, z, value);
        }

        // b. let (x, y) = (y, (2x + 3y) mod 5)
        auto old_x = x;
        x = y;
        y = mod(2 * old_x + 3 * y, 5);
    }

    // 4. Return A′.
    return A_prime;
}

// 3.2.3 Specification of π
constexpr State pi(State const& A)
{
    State A_prime {};
    // 1. For all triples (x, y, z) such that 0 ≤ x < 5, 0 ≤ y < 5, and 0 ≤ z < w, let
    //        A′[x, y, z]= A[(x + 3y) mod 5, x, z].
    for (u8 x = 0; x < 5; x++) {
        for (u8 y = 0; y < 5; y++) {
            for (u8 z = 0; z < KECCAK_W; ++z) {
                bool value = state_get(A, mod(x + 3 * y, 5), x, z);
                state_set(A_prime, x, y, z, value);
            }
        }
    }

    // 2. Return A′.
    return A_prime;
}

// 3.2.4 Specification of χ
constexpr State chi(State const& A)
{
    State A_prime {};
    // For all triples (x, y, z) such that 0 ≤ x < 5, 0 ≤ y < 5, and 0 ≤ z < w, let
    //     A′ [x, y, z] = A[x, y, z] ⊕ ((A[(x+1) mod 5, y, z] ⊕ 1) ⋅ A[(x+2) mod 5, y, z]).
    for (u8 x = 0; x < 5; x++) {
        for (u8 y = 0; y < 5; y++) {
            for (u8 z = 0; z < KECCAK_W; ++z) {
                bool value = state_get(A, x, y, z);
                value ^= (state_get(A, mod(x + 1, 5), y, z) ^ 1) & state_get(A, mod(x + 2, 5), y, z);
                state_set(A_prime, x, y, z, value);
            }
        }
    }

    // 2. Return A′.
    return A_prime;
}

// 3.2.5 Specification of ι
constexpr bool rc(u32 t)
{
    // 1. If t mod 255 = 0, return 1.
    if (t % 255 == 0)
        return 1;

    // 2. Let R = 10000000.
    Vector<bool, 9> R {};
    R.resize(9);
    R[0] = 1;

    // 3. For i from 1 to t mod 255, let:
    for (u32 i = 1; i <= t % 255; ++i) {
        // a. R = 0 || R;
        R.prepend(0);
        // b. R[0] = R[0] ⊕ R[8];
        R[0] ^= R[8];
        // c. R[4] = R[4] ⊕ R[8];
        R[4] ^= R[8];
        // d. R[5] = R[5] ⊕ R[8];
        R[5] ^= R[8];
        // e. R[6] = R[6] ⊕ R[8];
        R[6] ^= R[8];
        // f. R =Trunc8[R].
        R.resize(8);
    }

    // 4. Return R[0].
    return R[0];
}

// 3.2.5 Specification of ι
constexpr State iota(State const& A, u32 ir)
{
    // 1. For all triples (x, y, z) such that 0 ≤ x < 5, 0 ≤ y < 5, and 0 ≤ z < w, let A′[x, y, z] = A[x, y, z].
    auto A_prime = A;

    // 2. Let RC = 0w.
    Array<bool, KECCAK_W> RC {};

    // 3. For j from 0 to l, let RC[2^j – 1] = rc(j + 7ir).
    for (u8 j = 0; j <= KECCAK_L; ++j)
        RC[(1 << j) - 1] = rc(j + 7 * ir);

    // 4. For all z such that 0 ≤ z < w, let A′ [0, 0, z] = A′ [0, 0, z] ⊕ RC[z].
    for (u8 z = 0; z < KECCAK_W; ++z)
        state_set(A_prime, 0, 0, z, state_get(A_prime, 0, 0, z) ^ RC[z]);

    // 5. Return A′.
    return A_prime;
}

// 3.3 KECCAK-p[b, nr]
constexpr State Rnd(State const& state, u32 ir)
{
    // Rnd(A, ir) = ι(χ(π(ρ(θ(A)))), ir)
    return iota(chi(pi(rho(theta(state)))), ir);
}

// Algorithm 7: KECCAK-p[b, nr](S)
constexpr State keccak_p(State const& state)
{
    // 1. Convert S into a state array, A, as described in Sec. 3.1.2
    // NOTE: This is a noop, we just need to access A using state_* functions.
    auto A = state;

    // 2. For ir from 12 + 2l – nr to 12 + 2l – 1, let A = Rnd(A, ir).
    for (u32 ir = 12 + 2 * KECCAK_L - KECCAK_Nr; ir <= 12 + 2 * KECCAK_L - 1; ++ir)
        A = Rnd(A, ir);

    // 3. Convert A into a string S′ of length b, as described in Sec. 3.1.3.
    // NOTE: Again, this is a noop.
    // 4. Return S′.
    return A;
}

constexpr void update_state(Variation variation, State& state, ReadonlyBytes bytes)
{
    auto r = rate_from_variation(variation);
    VERIFY(bytes.size() == r);

    // 6. [...], let S = f (S ⊕ (Pi || 0c)).
    // NOTE: XORing with the zero is a noop, our XOR returns out early when the second
    //       argument is smaller than the first.
    XOR(state, bytes);
    state = keccak_p(state);
}

constexpr void update_state_with_padding(Variation variation, State& state, auto input_buffer)
{
    auto r = rate_from_variation(variation);
    VERIFY(input_buffer.size() < r);

    // NOTE: The padding added here covers both the padding from Algorithm 8 (pad10*1) and the one
    //       inherent to the function, specified in 6.1 SHA-3 Hash Functions.

    // 1. Let P = N || pad(r, len(N)).
    input_buffer.append(0b00000110u);
    input_buffer.resize(r);
    input_buffer.last() |= 0b10000000u;

    update_state(variation, state, input_buffer);
}

}

// The SHA3Impl class mainly implements "Algorithm 8: SPONGE[f, pad, r](N, d)".
// This algorithm expects the whole input to be provided at once, but we don't
// want this, so the individual steps of the algorithm are scattered between
// update and peek.

template<Variation V>
void SHA3Impl<V>::update(u8 const* input, size_t length)
{
    auto bytes = ReadonlyBytes { input, length };
    while (!bytes.is_empty()) {
        auto to_read = min(bytes.size(), m_input_buffer.capacity() - m_input_buffer.size());
        m_input_buffer.extend(bytes.trim(to_read));
        bytes = bytes.slice(to_read);
        if (m_input_buffer.size() == m_input_buffer.capacity()) {
            update_state(V, m_state, m_input_buffer);
            m_input_buffer.clear();
        }
    }
}

template<Variation V>
SHA3Impl<V>::HashBuffer SHA3Impl<V>::peek_impl(u32 d) const
{
    auto S = m_state;

    // 1. Let P = N || pad(r, len(N)).
    update_state_with_padding(V, S, m_input_buffer);

    // 7. Let Z be the empty string.
    HashBuffer Z {};
    Z.resize(d);

    u32 bytes_squeezed = 0;
    while (true) {
        // 8. Let Z = Z || Trunc r (S).
        for (u32 i = 0; i < rate_from_variation(V) && bytes_squeezed < d; ++i, ++bytes_squeezed)
            Z[bytes_squeezed] = S[i];
        // 9. If d ≤ |Z|, then return Trunc d (Z); else continue.
        if (d <= bytes_squeezed)
            return Z;
        // 10. Let S = f(S), and continue with Step 8.
        S = keccak_p(S);
    }
    VERIFY_NOT_REACHED();
}

template<Variation V>
SHA3Impl<V>::F::DigestType SHA3Impl<V>::peek()
{
    auto hash = peek_impl(F::DigestSize);
    typename F::DigestType Z;
    hash.bytes().copy_to({ Z.data, F::DigestSize });
    return Z;
}

template<Variation V>
void SHA3Impl<V>::reset()
{
    m_state = {};
    m_input_buffer = {};
}

template class SHA3Impl<Variation::SHA3_224>;
template class SHA3Impl<Variation::SHA3_256>;
template class SHA3Impl<Variation::SHA3_384>;
template class SHA3Impl<Variation::SHA3_512>;

} // Crypto::Hash::SHA3
