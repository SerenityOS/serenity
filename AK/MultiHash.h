/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SipHash.h>
#include <AK/Traits.h>

#pragma once

namespace AK {

// This template exists so that we can avoid re-hashing integers.
// In the general case, we pass the value through its trait-defined hash.
template<typename T>
u64 hash_with_trait_if_necessary(T value)
{
    return Traits<T>::hash(value);
}

template<Integral I>
constexpr u64 hash_with_trait_if_necessary(I value)
{
    return value;
}

template<FloatingPoint F>
constexpr u64 hash_with_trait_if_necessary(F value)
{
    return bit_cast<u64>(static_cast<double>(value));
}

template<typename... ArgumentTs>
u32 multi_hash(ArgumentTs... values)
{
    Array<u64, sizeof...(ArgumentTs)> hashes = { hash_with_trait_if_necessary(values)... };
    auto u64_output = sip_hash_bytes<1, 3>({ hashes.data(), sizeof(u64) * hashes.size() });
    return u64_output ^ (u64_output << 32);
}

}

#ifdef USING_AK_GLOBALLY
using AK::multi_hash;
#endif
