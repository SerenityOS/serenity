/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <LibCrypto/Hash/HashFunction.h>

namespace Crypto {
namespace Authentication {

void galois_multiply(u32 (&z)[4], const u32 (&x)[4], const u32 (&y)[4]);

struct GHashDigest {
    constexpr static size_t Size = 16;
    u8 data[Size];

    const u8* immutable_data() const { return data; }
    size_t data_length() { return Size; }
};

class GHash final {
public:
    using TagType = GHashDigest;

    template<size_t N>
    explicit GHash(const char (&key)[N])
        : GHash({ key, N })
    {
    }

    explicit GHash(const ReadonlyBytes& key)
    {
        for (size_t i = 0; i < 16; i += 4)
            m_key[i / 4] = AK::convert_between_host_and_big_endian(*(const u32*)(key.offset(i)));
    }

    constexpr static size_t digest_size() { return TagType::Size; }

    String class_name() const { return "GHash"; }

    TagType process(ReadonlyBytes aad, ReadonlyBytes cipher);

private:
    inline void transform(ReadonlyBytes, ReadonlyBytes);

    u32 m_key[4];
};

}

}
