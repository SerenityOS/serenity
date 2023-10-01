/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Forward.h>

namespace AK {

// Ported from the SipHash reference implementation, released to the public domain:
// https://github.com/veorq/SipHash/blob/eee7d0d84dc7731df2359b243aa5e75d85f6eaef/siphash.c
// The standard is SipHash-2-4, but we use 1-3 for a little more speed.
// Cryptography should use 4-8 for (relative) conservative security,
// though SipHash itself is NOT a cryptographically secure hash algorithm.
template<size_t message_block_rounds, size_t finalization_rounds>
u64 sip_hash_bytes(ReadonlyBytes input);
unsigned standard_sip_hash(u64 input);
unsigned secure_sip_hash(u64 input);

inline unsigned standard_sip_ptr_hash(void const* ptr)
{
    return standard_sip_hash(bit_cast<FlatPtr>(ptr));
}

// This function intentionally doesn't just allow C++ trivial types,
// it allows you to hash any types based on the entirety of their bits (as determined by sizeof).
// However, if there's contained types with padding, this may lead to a varying hash, so be careful!
template<typename TrivialType>
inline unsigned standard_sip_hash_trivial(TrivialType const& value)
{
    auto u64_output = sip_hash_bytes<1, 3>({ bit_cast<u8 const*>(&value), sizeof(value) });
    return u64_output ^ (u64_output << 32);
}

}

#ifdef USING_AK_GLOBALLY
using AK::secure_sip_hash;
using AK::sip_hash_bytes;
using AK::standard_sip_hash;
using AK::standard_sip_hash_trivial;
using AK::standard_sip_ptr_hash;
#endif
