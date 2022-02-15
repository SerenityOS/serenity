/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace Crypto {
namespace Hash {

template<size_t DigestS>
struct Digest {
    static_assert(DigestS % 8 == 0);
    constexpr static size_t Size = DigestS / 8;
    u8 data[Size];

    [[nodiscard]] ALWAYS_INLINE const u8* immutable_data() const { return data; }
    [[nodiscard]] ALWAYS_INLINE size_t data_length() const { return Size; }

    [[nodiscard]] ALWAYS_INLINE ReadonlyBytes bytes() const { return { immutable_data(), data_length() }; }
};

template<size_t BlockS, size_t DigestS, typename DigestT = Digest<DigestS>>
class HashFunction {
public:
    static_assert(BlockS % 8 == 0);
    static constexpr auto BlockSize = BlockS / 8;

    static_assert(DigestS % 8 == 0);
    static constexpr auto DigestSize = DigestS / 8;

    using DigestType = DigestT;

    constexpr static size_t block_size() { return BlockSize; }
    constexpr static size_t digest_size() { return DigestSize; }

    virtual void update(const u8*, size_t) = 0;

    void update(Bytes buffer) { update(buffer.data(), buffer.size()); }
    void update(ReadonlyBytes buffer) { update(buffer.data(), buffer.size()); }
    void update(const ByteBuffer& buffer) { update(buffer.data(), buffer.size()); }
    void update(StringView string) { update((const u8*)string.characters_without_null_termination(), string.length()); }

    virtual DigestType peek() = 0;
    virtual DigestType digest() = 0;

    virtual void reset() = 0;

#ifndef KERNEL
    virtual String class_name() const = 0;
#endif

protected:
    virtual ~HashFunction() = default;
};
}
}
