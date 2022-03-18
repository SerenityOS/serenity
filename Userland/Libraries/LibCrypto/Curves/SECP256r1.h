/*
 * Copyright (c) 2022, Michiel Visser <opensource@webmichiel.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/UFixedBigInt.h>
#include <LibCrypto/Curves/EllipticCurve.h>

namespace Crypto::Curves {

struct JacobianPoint {
    u256 x { 0u };
    u256 y { 0u };
    u256 z { 0u };
};

class SECP256r1 : public EllipticCurve {
public:
    size_t key_size() override { return 1 + 2 * 32; }
    ErrorOr<ByteBuffer> generate_private_key() override;
    ErrorOr<ByteBuffer> generate_public_key(ReadonlyBytes a) override;
    ErrorOr<ByteBuffer> compute_coordinate(ReadonlyBytes scalar_bytes, ReadonlyBytes point_bytes) override;
    ErrorOr<ByteBuffer> derive_premaster_key(ReadonlyBytes shared_point) override;

private:
    static u256 modular_reduce(u256 const& value);
    static u256 modular_reduce_order(u256 const& value);
    static u256 modular_add(u256 const& left, u256 const& right, bool carry_in = false);
    static u256 modular_sub(u256 const& left, u256 const& right);
    static u256 modular_multiply(u256 const& left, u256 const& right);
    static u256 modular_square(u256 const& value);
    static u256 to_montgomery(u256 const& value);
    static u256 from_montgomery(u256 const& value);
    static u256 modular_inverse(u256 const& value);
    static void point_double(JacobianPoint& output_point, JacobianPoint const& point);
    static void point_add(JacobianPoint& output_point, JacobianPoint const& point_a, JacobianPoint const& point_b);
    static void convert_jacobian_to_affine(JacobianPoint& point);
    static bool is_point_on_curve(JacobianPoint const& point);

    static constexpr u256 REDUCE_PRIME { u128 { 0x0000000000000001ull, 0xffffffff00000000ull }, u128 { 0xffffffffffffffffull, 0x00000000fffffffe } };
    static constexpr u256 REDUCE_ORDER { u128 { 0x0c46353d039cdaafull, 0x4319055258e8617bull }, u128 { 0x0000000000000000ull, 0x00000000ffffffff } };
    static constexpr u256 PRIME_INVERSE_MOD_R { u128 { 0x0000000000000001ull, 0x0000000100000000ull }, u128 { 0x0000000000000000ull, 0xffffffff00000002ull } };
    static constexpr u256 PRIME { u128 { 0xffffffffffffffffull, 0x00000000ffffffffull }, u128 { 0x0000000000000000ull, 0xffffffff00000001ull } };
    static constexpr u256 R2_MOD_PRIME { u128 { 0x0000000000000003ull, 0xfffffffbffffffffull }, u128 { 0xfffffffffffffffeull, 0x00000004fffffffdull } };
    static constexpr u256 ONE { 1u };
    static constexpr u256 B_MONTGOMERY { u128 { 0xd89cdf6229c4bddfull, 0xacf005cd78843090ull }, u128 { 0xe5a220abf7212ed6ull, 0xdc30061d04874834ull } };
};

}
