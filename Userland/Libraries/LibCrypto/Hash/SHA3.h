/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibCrypto/Hash/HashFunction.h>

namespace Crypto::Hash {

// The SHA3 hash functions are described in [FIPS202].
// https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.202.pdf

namespace SHA3 {

enum class Variation : u8 {
    SHA3_224,
    SHA3_256,
    SHA3_384,
    SHA3_512,
    SHAKE128,
    SHAKE256,
};

consteval size_t block_size_from_variation(Variation variation)
{
    // 7 Conformance
    switch (variation) {
    case Variation::SHA3_224:
        return 144;
    case Variation::SHA3_256:
        return 136;
    case Variation::SHA3_384:
        return 104;
    case Variation::SHA3_512:
        return 72;
    case Variation::SHAKE128:
    case Variation::SHAKE256:
        return 0;
    }

    VERIFY_NOT_REACHED();
}

constexpr size_t hash_size_from_variation(Variation variation)
{
    // A.1 Summary
    switch (variation) {
    case Variation::SHA3_224:
        return 224;
    case Variation::SHA3_256:
        return 256;
    case Variation::SHA3_384:
        return 384;
    case Variation::SHA3_512:
        return 512;
    case Variation::SHAKE128:
        return 128;
    case Variation::SHAKE256:
        return 256;
    }

    VERIFY_NOT_REACHED();
}

// 5.2 Specification of KECCAK[c]
// "When restricted to the case b = 1600, the KECCAK family is denoted by KECCAK[c]"
static constexpr u32 KECCAK_B = 1600;
static constexpr u32 KECCAK_Nr = 24;
static constexpr u32 KECCAK_STATE_SIZE = KECCAK_B / 8;
using State = Array<u8, KECCAK_STATE_SIZE>;

constexpr size_t rate_from_variation(Variation variation)
{
    // From 4 SPONGE CONSTRUCTION, Algorithm 8: SPONGE[f, pad, r](N, d)
    // r = b - c
    return (KECCAK_B - hash_size_from_variation(variation) * 2) / 8;
}

template<Variation V>
class SHA3Impl : public HashFunction<block_size_from_variation(V), hash_size_from_variation(V), SHA3Impl<V>> {
public:
    using F = HashFunction<block_size_from_variation(V), hash_size_from_variation(V), SHA3Impl>;
    using F::update;

    virtual void update(u8 const*, size_t) override;
    virtual F::DigestType peek() override;
    virtual void reset() override;

    virtual ByteString class_name() const override { return ByteString::formatted("SHA3-{}", hash_size_from_variation(V)); }

protected:
    using HashBuffer = AK::Detail::ByteBuffer<hash_size_from_variation(V)>;
    HashBuffer peek_impl(u32 d) const;

private:
    State m_state {};
    Vector<u8, rate_from_variation(V)> m_input_buffer {};
};

}

class SHA3_224 : public SHA3::SHA3Impl<SHA3::Variation::SHA3_224> { };
class SHA3_256 : public SHA3::SHA3Impl<SHA3::Variation::SHA3_256> { };
class SHA3_384 : public SHA3::SHA3Impl<SHA3::Variation::SHA3_384> { };
class SHA3_512 : public SHA3::SHA3Impl<SHA3::Variation::SHA3_512> { };

// FIXME: Both SHAKE128 and SHAKE256 inherits from HashFunction but
//        the API doesn't work really well for XOFs. Once we have
//        more users, find a better API to suite them all.
class SHAKE128 : public SHA3::SHA3Impl<SHA3::Variation::SHAKE128> {
public:
    virtual ByteString class_name() const override { return "SHAKE128"sv; }

    auto peek_with_size(u32 d) const { return peek_impl(d); }
};

class SHAKE256 : public SHA3::SHA3Impl<SHA3::Variation::SHAKE256> {
public:
    virtual ByteString class_name() const override { return "SHAKE256"sv; }

    auto peek_with_size(u32 d) const { return peek_impl(d); }
};

} // Crypto::Hash
