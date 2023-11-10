/*
 * Copyright (c) 2023, Michiel Visser <opensource@webmichiel.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <AK/UFixedBigInt.h>
#include <AK/UFixedBigIntDivision.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/Curves/SECP384r1.h>

namespace Crypto::Curves {

struct JacobianPoint {
    u384 x { 0u };
    u384 y { 0u };
    u384 z { 0u };
};

static constexpr u384 calculate_modular_inverse_mod_r(u384 value)
{
    // Calculate the modular multiplicative inverse of value mod 2^384 using the extended euclidean algorithm
    u768 old_r = value;
    u768 r = static_cast<u768>(1u) << 384u;
    u768 old_s = 1u;
    u768 s = 0u;

    while (!r.is_zero_constant_time()) {
        u768 quotient = old_r / r;
        u768 temp = r;
        r = old_r - quotient * r;
        old_r = temp;

        temp = s;
        s = old_s - quotient * s;
        old_s = temp;
    }

    return old_s.low();
}

static constexpr u384 calculate_r2_mod(u384 modulus)
{
    // Calculate the value of R^2 mod modulus, where R = 2^384
    u1536 r = static_cast<u1536>(1u) << 384u;
    u1536 r2 = r * r;
    u1536 result = r2 % static_cast<u1536>(modulus);
    return result.low().low();
}

// SECP384r1 curve parameters
static constexpr u384 PRIME { { 0x00000000ffffffffull, 0xffffffff00000000ull, 0xfffffffffffffffeull, 0xffffffffffffffffull, 0xffffffffffffffffull, 0xffffffffffffffffull } };
static constexpr u384 A { { 0x00000000fffffffcull, 0xffffffff00000000ull, 0xfffffffffffffffeull, 0xffffffffffffffffull, 0xffffffffffffffffull, 0xffffffffffffffffull } };
static constexpr u384 B { { 0x2a85c8edd3ec2aefull, 0xc656398d8a2ed19dull, 0x0314088f5013875aull, 0x181d9c6efe814112ull, 0x988e056be3f82d19ull, 0xb3312fa7e23ee7e4ull } };
static constexpr u384 ORDER { { 0xecec196accc52973ull, 0x581a0db248b0a77aull, 0xc7634d81f4372ddfull, 0xffffffffffffffffull, 0xffffffffffffffffull, 0xffffffffffffffffull } };

// Verify that A = -3 mod p, which is required for some optimizations
static_assert(A == PRIME - 3);

// Precomputed helper values for reduction and Montgomery multiplication
static constexpr u384 REDUCE_PRIME = u384 { 0 } - PRIME;
static constexpr u384 REDUCE_ORDER = u384 { 0 } - ORDER;
static constexpr u384 PRIME_INVERSE_MOD_R = u384 { 0 } - calculate_modular_inverse_mod_r(PRIME);
static constexpr u384 ORDER_INVERSE_MOD_R = u384 { 0 } - calculate_modular_inverse_mod_r(ORDER);
static constexpr u384 R2_MOD_PRIME = calculate_r2_mod(PRIME);
static constexpr u384 R2_MOD_ORDER = calculate_r2_mod(ORDER);

static u384 import_big_endian(ReadonlyBytes data)
{
    VERIFY(data.size() == 48);

    u64 f = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(0 * sizeof(u64))));
    u64 e = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(1 * sizeof(u64))));
    u64 d = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(2 * sizeof(u64))));
    u64 c = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(3 * sizeof(u64))));
    u64 b = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(4 * sizeof(u64))));
    u64 a = AK::convert_between_host_and_big_endian(ByteReader::load64(data.offset_pointer(5 * sizeof(u64))));

    return u384 { { a, b, c, d, e, f } };
}

static void export_big_endian(u384 const& value, Bytes data)
{
    auto span = value.span();

    u64 a = AK::convert_between_host_and_big_endian(span[0]);
    u64 b = AK::convert_between_host_and_big_endian(span[1]);
    u64 c = AK::convert_between_host_and_big_endian(span[2]);
    u64 d = AK::convert_between_host_and_big_endian(span[3]);
    u64 e = AK::convert_between_host_and_big_endian(span[4]);
    u64 f = AK::convert_between_host_and_big_endian(span[5]);

    ByteReader::store(data.offset_pointer(5 * sizeof(u64)), a);
    ByteReader::store(data.offset_pointer(4 * sizeof(u64)), b);
    ByteReader::store(data.offset_pointer(3 * sizeof(u64)), c);
    ByteReader::store(data.offset_pointer(2 * sizeof(u64)), d);
    ByteReader::store(data.offset_pointer(1 * sizeof(u64)), e);
    ByteReader::store(data.offset_pointer(0 * sizeof(u64)), f);
}

static constexpr u384 select(u384 const& left, u384 const& right, bool condition)
{
    // If condition = 0 return left else right
    u384 mask = (u384)condition - 1;

    return (left & mask) | (right & ~mask);
}

static constexpr u768 multiply(u384 const& left, u384 const& right)
{
    return left.wide_multiply(right);
}

static constexpr u384 modular_reduce(u384 const& value)
{
    // Add -prime % 2^384
    bool carry = false;
    u384 other = value.addc(REDUCE_PRIME, carry);

    // Check for overflow
    return select(value, other, carry);
}

static constexpr u384 modular_reduce_order(u384 const& value)
{
    // Add -order % 2^384
    bool carry = false;
    u384 other = value.addc(REDUCE_ORDER, carry);

    // Check for overflow
    return select(value, other, carry);
}

static constexpr u384 modular_add(u384 const& left, u384 const& right, bool carry_in = false)
{
    bool carry = carry_in;
    u384 output = left.addc(right, carry);

    // If there is a carry, subtract p by adding 2^384 - p
    u384 addend = select(0u, REDUCE_PRIME, carry);
    carry = false;
    output = output.addc(addend, carry);

    // If there is still a carry, subtract p by adding 2^384 - p
    addend = select(0u, REDUCE_PRIME, carry);
    return output + addend;
}

static constexpr u384 modular_sub(u384 const& left, u384 const& right)
{
    bool borrow = false;
    u384 output = left.subc(right, borrow);

    // If there is a borrow, add p by subtracting 2^384 - p
    u384 sub = select(0u, REDUCE_PRIME, borrow);
    borrow = false;
    output = output.subc(sub, borrow);

    // If there is still a borrow, add p by subtracting 2^384 - p
    sub = select(0u, REDUCE_PRIME, borrow);
    return output - sub;
}

static constexpr u384 modular_multiply(u384 const& left, u384 const& right)
{
    // Modular multiplication using the Montgomery method: https://en.wikipedia.org/wiki/Montgomery_modular_multiplication
    // This requires that the inputs to this function are in Montgomery form.

    // T = left * right
    u768 mult = multiply(left, right);

    // m = ((T mod R) * curve_p')
    u768 m = multiply(mult.low(), PRIME_INVERSE_MOD_R);

    // mp = (m mod R) * curve_p
    u768 mp = multiply(m.low(), PRIME);

    // t = (T + mp)
    bool carry = false;
    mult.low().addc(mp.low(), carry);

    // output = t / R
    return modular_add(mult.high(), mp.high(), carry);
}

static constexpr u384 modular_square(u384 const& value)
{
    return modular_multiply(value, value);
}

static constexpr u384 to_montgomery(u384 const& value)
{
    return modular_multiply(value, R2_MOD_PRIME);
}

static constexpr u384 from_montgomery(u384 const& value)
{
    return modular_multiply(value, 1u);
}

static constexpr u384 modular_inverse(u384 const& value)
{
    // Modular inverse modulo the curve prime can be computed using Fermat's little theorem: a^(p-2) mod p = a^-1 mod p.
    // Calculating a^(p-2) mod p can be done using the square-and-multiply exponentiation method, as p-2 is constant.
    u384 base = value;
    u384 result = to_montgomery(1u);
    u384 prime_minus_2 = PRIME - 2u;

    for (size_t i = 0; i < 384; i++) {
        if ((prime_minus_2 & 1u) == 1u) {
            result = modular_multiply(result, base);
        }
        base = modular_square(base);
        prime_minus_2 >>= 1u;
    }

    return result;
}

static constexpr u384 modular_add_order(u384 const& left, u384 const& right, bool carry_in = false)
{
    bool carry = carry_in;
    u384 output = left.addc(right, carry);

    // If there is a carry, subtract n by adding 2^384 - n
    u384 addend = select(0u, REDUCE_ORDER, carry);
    carry = false;
    output = output.addc(addend, carry);

    // If there is still a carry, subtract n by adding 2^384 - n
    addend = select(0u, REDUCE_ORDER, carry);
    return output + addend;
}

static constexpr u384 modular_multiply_order(u384 const& left, u384 const& right)
{
    // Modular multiplication using the Montgomery method: https://en.wikipedia.org/wiki/Montgomery_modular_multiplication
    // This requires that the inputs to this function are in Montgomery form.

    // T = left * right
    u768 mult = multiply(left, right);

    // m = ((T mod R) * curve_n')
    u768 m = multiply(mult.low(), ORDER_INVERSE_MOD_R);

    // mp = (m mod R) * curve_n
    u768 mp = multiply(m.low(), ORDER);

    // t = (T + mp)
    bool carry = false;
    mult.low().addc(mp.low(), carry);

    // output = t / R
    return modular_add_order(mult.high(), mp.high(), carry);
}

static constexpr u384 modular_square_order(u384 const& value)
{
    return modular_multiply_order(value, value);
}

static constexpr u384 to_montgomery_order(u384 const& value)
{
    return modular_multiply_order(value, R2_MOD_ORDER);
}

static constexpr u384 from_montgomery_order(u384 const& value)
{
    return modular_multiply_order(value, 1u);
}

static constexpr u384 modular_inverse_order(u384 const& value)
{
    // Modular inverse modulo the curve order can be computed using Fermat's little theorem: a^(n-2) mod n = a^-1 mod n.
    // Calculating a^(n-2) mod n can be done using the square-and-multiply exponentiation method, as n-2 is constant.
    u384 base = value;
    u384 result = to_montgomery_order(1u);
    u384 order_minus_2 = ORDER - 2u;

    for (size_t i = 0; i < 384; i++) {
        if ((order_minus_2 & 1u) == 1u) {
            result = modular_multiply_order(result, base);
        }
        base = modular_square_order(base);
        order_minus_2 >>= 1u;
    }

    return result;
}

static void point_double(JacobianPoint& output_point, JacobianPoint const& point)
{
    // Based on "Point Doubling" from http://point-at-infinity.org/ecc/Prime_Curve_Jacobian_Coordinates.html

    // if (Y == 0)
    //   return POINT_AT_INFINITY
    if (point.y.is_zero_constant_time()) {
        VERIFY_NOT_REACHED();
    }

    u384 temp;

    // Y2 = Y^2
    u384 y2 = modular_square(point.y);

    // S = 4*X*Y2
    u384 s = modular_multiply(point.x, y2);
    s = modular_add(s, s);
    s = modular_add(s, s);

    // M = 3*X^2 + a*Z^4 = 3*(X + Z^2)*(X - Z^2)
    // This specific equation from https://github.com/earlephilhower/bearssl-esp8266/blob/6105635531027f5b298aa656d44be2289b2d434f/src/ec/ec_p256_m64.c#L811-L816
    // This simplification only works because a = -3 mod p
    temp = modular_square(point.z);
    u384 m = modular_add(point.x, temp);
    temp = modular_sub(point.x, temp);
    m = modular_multiply(m, temp);
    temp = modular_add(m, m);
    m = modular_add(m, temp);

    // X' = M^2 - 2*S
    u384 xp = modular_square(m);
    xp = modular_sub(xp, s);
    xp = modular_sub(xp, s);

    // Y' = M*(S - X') - 8*Y2^2
    u384 yp = modular_sub(s, xp);
    yp = modular_multiply(yp, m);
    temp = modular_square(y2);
    temp = modular_add(temp, temp);
    temp = modular_add(temp, temp);
    temp = modular_add(temp, temp);
    yp = modular_sub(yp, temp);

    // Z' = 2*Y*Z
    u384 zp = modular_multiply(point.y, point.z);
    zp = modular_add(zp, zp);

    // return (X', Y', Z')
    output_point.x = xp;
    output_point.y = yp;
    output_point.z = zp;
}

static void point_add(JacobianPoint& output_point, JacobianPoint const& point_a, JacobianPoint const& point_b)
{
    // Based on "Point Addition" from  http://point-at-infinity.org/ecc/Prime_Curve_Jacobian_Coordinates.html
    if (point_a.x.is_zero_constant_time() && point_a.y.is_zero_constant_time() && point_a.z.is_zero_constant_time()) {
        output_point.x = point_b.x;
        output_point.y = point_b.y;
        output_point.z = point_b.z;
        return;
    }

    u384 temp;

    temp = modular_square(point_b.z);
    // U1 = X1*Z2^2
    u384 u1 = modular_multiply(point_a.x, temp);
    // S1 = Y1*Z2^3
    u384 s1 = modular_multiply(point_a.y, temp);
    s1 = modular_multiply(s1, point_b.z);

    temp = modular_square(point_a.z);
    // U2 = X2*Z1^2
    u384 u2 = modular_multiply(point_b.x, temp);
    // S2 = Y2*Z1^3
    u384 s2 = modular_multiply(point_b.y, temp);
    s2 = modular_multiply(s2, point_a.z);

    // if (U1 == U2)
    //   if (S1 != S2)
    //     return POINT_AT_INFINITY
    //   else
    //     return POINT_DOUBLE(X1, Y1, Z1)
    if (u1.is_equal_to_constant_time(u2)) {
        if (s1.is_equal_to_constant_time(s2)) {
            point_double(output_point, point_a);
            return;
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    // H = U2 - U1
    u384 h = modular_sub(u2, u1);
    u384 h2 = modular_square(h);
    u384 h3 = modular_multiply(h2, h);
    // R = S2 - S1
    u384 r = modular_sub(s2, s1);
    // X3 = R^2 - H^3 - 2*U1*H^2
    u384 x3 = modular_square(r);
    x3 = modular_sub(x3, h3);
    temp = modular_multiply(u1, h2);
    temp = modular_add(temp, temp);
    x3 = modular_sub(x3, temp);
    // Y3 = R*(U1*H^2 - X3) - S1*H^3
    u384 y3 = modular_multiply(u1, h2);
    y3 = modular_sub(y3, x3);
    y3 = modular_multiply(y3, r);
    temp = modular_multiply(s1, h3);
    y3 = modular_sub(y3, temp);
    // Z3 = H*Z1*Z2
    u384 z3 = modular_multiply(h, point_a.z);
    z3 = modular_multiply(z3, point_b.z);
    // return (X3, Y3, Z3)
    output_point.x = x3;
    output_point.y = y3;
    output_point.z = z3;
}

static void convert_jacobian_to_affine(JacobianPoint& point)
{
    u384 temp;
    // X' = X/Z^2
    temp = modular_square(point.z);
    temp = modular_inverse(temp);
    point.x = modular_multiply(point.x, temp);
    // Y' = Y/Z^3
    temp = modular_square(point.z);
    temp = modular_multiply(temp, point.z);
    temp = modular_inverse(temp);
    point.y = modular_multiply(point.y, temp);
    // Z' = 1
    point.z = to_montgomery(1u);
}

static bool is_point_on_curve(JacobianPoint const& point)
{
    // This check requires the point to be in Montgomery form, with Z=1
    u384 temp, temp2;

    // Calulcate Y^2 - X^3 - a*X - b = Y^2 - X^3 + 3*X - b
    temp = modular_square(point.y);
    temp2 = modular_square(point.x);
    temp2 = modular_multiply(temp2, point.x);
    temp = modular_sub(temp, temp2);
    temp = modular_add(temp, point.x);
    temp = modular_add(temp, point.x);
    temp = modular_add(temp, point.x);
    temp = modular_sub(temp, to_montgomery(B));
    temp = modular_reduce(temp);

    return temp.is_zero_constant_time() && point.z.is_equal_to_constant_time(to_montgomery(1u));
}

ErrorOr<ByteBuffer> SECP384r1::generate_private_key()
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(48));
    fill_with_random(buffer);
    return buffer;
}

ErrorOr<ByteBuffer> SECP384r1::generate_public_key(ReadonlyBytes a)
{
    // clang-format off
    u8 generator_bytes[97] {
        0x04,
        0xAA, 0x87, 0xCA, 0x22, 0xBE, 0x8B, 0x05, 0x37, 0x8E, 0xB1, 0xC7, 0x1E, 0xF3, 0x20, 0xAD, 0x74,
        0x6E, 0x1D, 0x3B, 0x62, 0x8B, 0xA7, 0x9B, 0x98, 0x59, 0xF7, 0x41, 0xE0, 0x82, 0x54, 0x2A, 0x38,
        0x55, 0x02, 0xF2, 0x5D, 0xBF, 0x55, 0x29, 0x6C, 0x3A, 0x54, 0x5E, 0x38, 0x72, 0x76, 0x0A, 0xB7,
        0x36, 0x17, 0xDE, 0x4A, 0x96, 0x26, 0x2C, 0x6F, 0x5D, 0x9E, 0x98, 0xBF, 0x92, 0x92, 0xDC, 0x29,
        0xF8, 0xF4, 0x1D, 0xBD, 0x28, 0x9A, 0x14, 0x7C, 0xE9, 0xDA, 0x31, 0x13, 0xB5, 0xF0, 0xB8, 0xC0,
        0x0A, 0x60, 0xB1, 0xCE, 0x1D, 0x7E, 0x81, 0x9D, 0x7A, 0x43, 0x1D, 0x7C, 0x90, 0xEA, 0x0E, 0x5F,
    };
    // clang-format on
    return compute_coordinate(a, { generator_bytes, 97 });
}

ErrorOr<ByteBuffer> SECP384r1::compute_coordinate(ReadonlyBytes scalar_bytes, ReadonlyBytes point_bytes)
{
    VERIFY(scalar_bytes.size() == 48);

    u384 scalar = import_big_endian(scalar_bytes);
    // FIXME: This will slightly bias the distribution of client secrets
    scalar = modular_reduce_order(scalar);
    if (scalar.is_zero_constant_time())
        return Error::from_string_literal("SECP384r1: scalar is zero");

    // Make sure the point is uncompressed
    if (point_bytes.size() != 97 || point_bytes[0] != 0x04)
        return Error::from_string_literal("SECP384r1: point is not uncompressed format");

    JacobianPoint point {
        import_big_endian(point_bytes.slice(1, 48)),
        import_big_endian(point_bytes.slice(49, 48)),
        1u,
    };

    // Convert the input point into Montgomery form
    point.x = to_montgomery(point.x);
    point.y = to_montgomery(point.y);
    point.z = to_montgomery(point.z);

    // Check that the point is on the curve
    if (!is_point_on_curve(point))
        return Error::from_string_literal("SECP384r1: point is not on the curve");

    JacobianPoint result;
    JacobianPoint temp_result;

    // Calculate the scalar times point multiplication in constant time
    for (auto i = 0; i < 384; i++) {
        point_add(temp_result, result, point);

        auto condition = (scalar & 1u) == 1u;
        result.x = select(result.x, temp_result.x, condition);
        result.y = select(result.y, temp_result.y, condition);
        result.z = select(result.z, temp_result.z, condition);

        point_double(point, point);
        scalar >>= 1u;
    }

    // Convert from Jacobian coordinates back to Affine coordinates
    convert_jacobian_to_affine(result);

    // Make sure the resulting point is on the curve
    VERIFY(is_point_on_curve(result));

    // Convert the result back from Montgomery form
    result.x = from_montgomery(result.x);
    result.y = from_montgomery(result.y);
    // Final modular reduction on the coordinates
    result.x = modular_reduce(result.x);
    result.y = modular_reduce(result.y);

    // Export the values into an output buffer
    auto buf = TRY(ByteBuffer::create_uninitialized(97));
    buf[0] = 0x04;
    export_big_endian(result.x, buf.bytes().slice(1, 48));
    export_big_endian(result.y, buf.bytes().slice(49, 48));
    return buf;
}

ErrorOr<ByteBuffer> SECP384r1::derive_premaster_key(ReadonlyBytes shared_point)
{
    VERIFY(shared_point.size() == 97);
    VERIFY(shared_point[0] == 0x04);

    ByteBuffer premaster_key = TRY(ByteBuffer::create_uninitialized(48));
    premaster_key.overwrite(0, shared_point.data() + 1, 48);
    return premaster_key;
}

ErrorOr<bool> SECP384r1::verify(ReadonlyBytes hash, ReadonlyBytes pubkey, ReadonlyBytes signature)
{
    Crypto::ASN1::Decoder asn1_decoder(signature);
    TRY(asn1_decoder.enter());

    auto r_bigint = TRY(asn1_decoder.read<Crypto::UnsignedBigInteger>(Crypto::ASN1::Class::Universal, Crypto::ASN1::Kind::Integer));
    auto s_bigint = TRY(asn1_decoder.read<Crypto::UnsignedBigInteger>(Crypto::ASN1::Class::Universal, Crypto::ASN1::Kind::Integer));

    u384 r = 0u;
    u384 s = 0u;
    for (size_t i = 0; i < 12; i++) {
        u384 rr = r_bigint.words()[i];
        u384 ss = s_bigint.words()[i];
        r |= (rr << (i * 32));
        s |= (ss << (i * 32));
    }

    // z is the hash
    u384 z = import_big_endian(hash.slice(0, 48));

    u384 r_mo = to_montgomery_order(r);
    u384 s_mo = to_montgomery_order(s);
    u384 z_mo = to_montgomery_order(z);

    u384 s_inv = modular_inverse_order(s_mo);

    u384 u1 = modular_multiply_order(z_mo, s_inv);
    u384 u2 = modular_multiply_order(r_mo, s_inv);

    u1 = from_montgomery_order(u1);
    u2 = from_montgomery_order(u2);

    auto u1_buf = TRY(ByteBuffer::create_uninitialized(48));
    export_big_endian(u1, u1_buf.bytes());
    auto u2_buf = TRY(ByteBuffer::create_uninitialized(48));
    export_big_endian(u2, u2_buf.bytes());

    auto p1 = TRY(generate_public_key(u1_buf));
    auto p2 = TRY(compute_coordinate(u2_buf, pubkey));

    JacobianPoint point1 {
        import_big_endian(TRY(p1.slice(1, 48))),
        import_big_endian(TRY(p1.slice(49, 48))),
        1u,
    };

    // Convert the input point into Montgomery form
    point1.x = to_montgomery(point1.x);
    point1.y = to_montgomery(point1.y);
    point1.z = to_montgomery(point1.z);

    VERIFY(is_point_on_curve(point1));

    JacobianPoint point2 {
        import_big_endian(TRY(p2.slice(1, 48))),
        import_big_endian(TRY(p2.slice(49, 48))),
        1u,
    };

    // Convert the input point into Montgomery form
    point2.x = to_montgomery(point2.x);
    point2.y = to_montgomery(point2.y);
    point2.z = to_montgomery(point2.z);

    VERIFY(is_point_on_curve(point2));

    JacobianPoint result;
    point_add(result, point1, point2);

    // Convert from Jacobian coordinates back to Affine coordinates
    convert_jacobian_to_affine(result);

    // Make sure the resulting point is on the curve
    VERIFY(is_point_on_curve(result));

    // Convert the result back from Montgomery form
    result.x = from_montgomery(result.x);
    result.y = from_montgomery(result.y);
    // Final modular reduction on the coordinates
    result.x = modular_reduce(result.x);
    result.y = modular_reduce(result.y);

    return r.is_equal_to_constant_time(result.x);
}

}
