/*
 * Copyright (c) 2022, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <LibCrypto/Curves/X25519.h>

namespace Crypto::Curves {

void X25519::import_state(u32* state, ReadonlyBytes data)
{
    for (auto i = 0; i < X25519::WORDS; i++) {
        u32 value = ByteReader::load32(data.offset_pointer(sizeof(u32) * i));
        state[i] = AK::convert_between_host_and_little_endian(value);
    }
}

ErrorOr<ByteBuffer> X25519::export_state(u32* data)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(X25519::BYTES));

    for (auto i = 0; i < X25519::WORDS; i++) {
        u32 value = AK::convert_between_host_and_little_endian(data[i]);
        ByteReader::store(buffer.offset_pointer(sizeof(u32) * i), value);
    }

    return buffer;
}

void X25519::select(u32* state, u32* a, u32* b, u32 condition)
{
    // If B < (2^255 - 19) then R = B, else R = A
    u32 mask = condition - 1;

    for (auto i = 0; i < X25519::WORDS; i++) {
        state[i] = (a[i] & mask) | (b[i] & ~mask);
    }
}

void X25519::set(u32* state, u32 value)
{
    state[0] = value;

    for (auto i = 1; i < X25519::WORDS; i++) {
        state[i] = 0;
    }
}

void X25519::copy(u32* state, u32* value)
{
    for (auto i = 0; i < X25519::WORDS; i++) {
        state[i] = value[i];
    }
}

void X25519::conditional_swap(u32* first, u32* second, u32 condition)
{
    u32 mask = ~condition + 1;
    for (auto i = 0; i < X25519::WORDS; i++) {
        u32 temp = mask & (first[i] ^ second[i]);
        first[i] ^= temp;
        second[i] ^= temp;
    }
}

void X25519::modular_multiply_single(u32* state, u32* first, u32 second)
{
    // Compute R = (A * B) mod p
    u64 temp = 0;
    u32 output[X25519::WORDS];

    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += (u64)first[i] * second;
        output[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    // Reduce bit 256 (2^256 = 38 mod p)
    temp *= 38;
    // Reduce bit 255 (2^255 = 19 mod p)
    temp += (output[7] >> 31) * 19;
    // Mask the most significant bit
    output[7] &= 0x7FFFFFFF;

    // Fast modular reduction
    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += output[i];
        output[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    modular_reduce(state, output);
}

void X25519::modular_square(u32* state, u32* value)
{
    // Compute R = (A ^ 2) mod p
    modular_multiply(state, value, value);
}

void X25519::modular_multiply(u32* state, u32* first, u32* second)
{
    // Compute R = (A * B) mod p
    u64 temp = 0;
    u64 carry = 0;
    u32 output[X25519::WORDS * 2];

    // Comba's method
    for (auto i = 0; i < 16; i++) {
        if (i < X25519::WORDS) {
            for (auto j = 0; j <= i; j++) {
                temp += (u64)first[j] * second[i - j];
                carry += temp >> 32;
                temp &= 0xFFFFFFFF;
            }
        } else {
            for (auto j = i - 7; j < X25519::WORDS; j++) {
                temp += (u64)first[j] * second[i - j];
                carry += temp >> 32;
                temp &= 0xFFFFFFFF;
            }
        }

        output[i] = temp & 0xFFFFFFFF;
        temp = carry & 0xFFFFFFFF;
        carry >>= 32;
    }

    // Reduce bit 255 (2^255 = 19 mod p)
    temp = (output[7] >> 31) * 19;
    // Mask the most significant bit
    output[7] &= 0x7FFFFFFF;

    // Fast modular reduction 1st pass
    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += output[i];
        temp += (u64)output[i + 8] * 38;
        output[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    // Reduce bit 256 (2^256 = 38 mod p)
    temp *= 38;
    // Reduce bit 255 (2^255 = 19 mod p)
    temp += (output[7] >> 31) * 19;
    // Mask the most significant bit
    output[7] &= 0x7FFFFFFF;

    // Fast modular reduction 2nd pass
    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += output[i];
        output[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    modular_reduce(state, output);
}

void X25519::modular_add(u32* state, u32* first, u32* second)
{
    // R = (A + B) mod p
    u64 temp = 0;
    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += first[i];
        temp += second[i];
        state[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    modular_reduce(state, state);
}

void X25519::modular_subtract(u32* state, u32* first, u32* second)
{
    // R = (A - B) mod p
    i64 temp = -19;
    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += first[i];
        temp -= second[i];
        state[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    // Compute R = A + (2^255 - 19) - B
    state[7] += 0x80000000;

    modular_reduce(state, state);
}

void X25519::modular_reduce(u32* state, u32* data)
{
    // R = A mod p
    u64 temp = 19;
    u32 other[X25519::WORDS];

    for (auto i = 0; i < X25519::WORDS; i++) {
        temp += data[i];
        other[i] = temp & 0xFFFFFFFF;
        temp >>= 32;
    }

    // Compute B = A - (2^255 - 19)
    other[7] -= 0x80000000;

    u32 mask = (other[7] & 0x80000000) >> 31;
    select(state, other, data, mask);
}

void X25519::to_power_of_2n(u32* state, u32* value, u8 n)
{
    // compute R = (A ^ (2^n)) mod p
    modular_square(state, value);
    for (auto i = 1; i < n; i++) {
        modular_square(state, state);
    }
}

void X25519::modular_multiply_inverse(u32* state, u32* value)
{
    // Compute R = A^-1 mod p
    u32 u[X25519::WORDS];
    u32 v[X25519::WORDS];

    // Fermat's little theorem
    modular_square(u, value);
    modular_multiply(u, u, value);
    modular_square(u, u);
    modular_multiply(v, u, value);
    to_power_of_2n(u, v, 3);
    modular_multiply(u, u, v);
    modular_square(u, u);
    modular_multiply(v, u, value);
    to_power_of_2n(u, v, 7);
    modular_multiply(u, u, v);
    modular_square(u, u);
    modular_multiply(v, u, value);
    to_power_of_2n(u, v, 15);
    modular_multiply(u, u, v);
    modular_square(u, u);
    modular_multiply(v, u, value);
    to_power_of_2n(u, v, 31);
    modular_multiply(v, u, v);
    to_power_of_2n(u, v, 62);
    modular_multiply(u, u, v);
    modular_square(u, u);
    modular_multiply(v, u, value);
    to_power_of_2n(u, v, 125);
    modular_multiply(u, u, v);
    modular_square(u, u);
    modular_square(u, u);
    modular_multiply(u, u, value);
    modular_square(u, u);
    modular_square(u, u);
    modular_multiply(u, u, value);
    modular_square(u, u);
    modular_multiply(state, u, value);
}

// https://datatracker.ietf.org/doc/html/rfc7748#section-5
ErrorOr<ByteBuffer> X25519::compute_coordinate(ReadonlyBytes input_k, ReadonlyBytes input_u)
{
    u32 k[X25519::WORDS] {};
    u32 u[X25519::WORDS] {};
    u32 x1[X25519::WORDS] {};
    u32 x2[X25519::WORDS] {};
    u32 z1[X25519::WORDS] {};
    u32 z2[X25519::WORDS] {};
    u32 t1[X25519::WORDS] {};
    u32 t2[X25519::WORDS] {};

    // Copy input to internal state
    import_state(k, input_k);

    // Set the three least significant bits of the first byte and the most significant bit of the last to zero,
    // set the second most significant bit of the last byte to 1
    k[0] &= 0xFFFFFFF8;
    k[7] &= 0x7FFFFFFF;
    k[7] |= 0x40000000;

    // Copy coordinate to internal state
    import_state(u, input_u);
    // mask the most significant bit in the final byte.
    u[7] &= 0x7FFFFFFF;

    // Implementations MUST accept non-canonical values and process them as
    // if they had been reduced modulo the field prime.
    modular_reduce(u, u);

    set(x1, 1);
    set(z1, 0);
    copy(x2, u);
    set(z2, 1);

    // Montgomery ladder
    u32 swap = 0;
    for (auto i = X25519::BITS - 1; i >= 0; i--) {
        u32 b = (k[i / X25519::BYTES] >> (i % X25519::BYTES)) & 1;

        conditional_swap(x1, x2, swap ^ b);
        conditional_swap(z1, z2, swap ^ b);

        swap = b;

        modular_add(t1, x2, z2);
        modular_subtract(x2, x2, z2);
        modular_add(z2, x1, z1);
        modular_subtract(x1, x1, z1);
        modular_multiply(t1, t1, x1);
        modular_multiply(x2, x2, z2);
        modular_square(z2, z2);
        modular_square(x1, x1);
        modular_subtract(t2, z2, x1);
        modular_multiply_single(z1, t2, A24);
        modular_add(z1, z1, x1);
        modular_multiply(z1, z1, t2);
        modular_multiply(x1, x1, z2);
        modular_subtract(z2, t1, x2);
        modular_square(z2, z2);
        modular_multiply(z2, z2, u);
        modular_add(x2, x2, t1);
        modular_square(x2, x2);
    }

    conditional_swap(x1, x2, swap);
    conditional_swap(z1, z2, swap);

    // Retrieve affine representation
    modular_multiply_inverse(u, z1);
    modular_multiply(u, u, x1);

    // Encode state for export
    return export_state(u);
}
}
