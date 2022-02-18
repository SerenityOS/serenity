/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCrypto/Hash/HashFunction.h>

#ifndef KERNEL
#    include <AK/String.h>
#endif

namespace Crypto {
namespace Hash {

namespace MD5Constants {

constexpr u32 init_A = 0x67452301;
constexpr u32 init_B = 0xefcdab89;
constexpr u32 init_C = 0x98badcfe;
constexpr u32 init_D = 0x10325476;
constexpr u32 S11 = 7;
constexpr u32 S12 = 12;
constexpr u32 S13 = 17;
constexpr u32 S14 = 22;
constexpr u32 S21 = 5;
constexpr u32 S22 = 9;
constexpr u32 S23 = 14;
constexpr u32 S24 = 20;
constexpr u32 S31 = 4;
constexpr u32 S32 = 11;
constexpr u32 S33 = 16;
constexpr u32 S34 = 23;
constexpr u32 S41 = 6;
constexpr u32 S42 = 10;
constexpr u32 S43 = 15;
constexpr u32 S44 = 21;
constexpr u8 PADDING[] = {
    0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0
};

}

class MD5 final : public HashFunction<512, 128> {
public:
    using HashFunction::update;

    virtual void update(const u8*, size_t) override;
    virtual DigestType digest() override;
    virtual DigestType peek() override;

#ifndef KERNEL
    virtual String class_name() const override
    {
        return "MD5";
    }
#endif

    inline static DigestType hash(const u8* data, size_t length)
    {
        MD5 md5;
        md5.update(data, length);
        return md5.digest();
    }

    inline static DigestType hash(const ByteBuffer& buffer) { return hash(buffer.data(), buffer.size()); }
    inline static DigestType hash(StringView buffer) { return hash((const u8*)buffer.characters_without_null_termination(), buffer.length()); }
    inline virtual void reset() override
    {
        m_A = MD5Constants::init_A;
        m_B = MD5Constants::init_B;
        m_C = MD5Constants::init_C;
        m_D = MD5Constants::init_D;

        m_count[0] = 0;
        m_count[1] = 0;

        __builtin_memset(m_data_buffer, 0, sizeof(m_data_buffer));
    }

private:
    inline void transform(const u8*);

    static void encode(const u32* from, u8* to, size_t length);
    static void decode(const u8* from, u32* to, size_t length);

    u32 m_A { MD5Constants::init_A }, m_B { MD5Constants::init_B }, m_C { MD5Constants::init_C }, m_D { MD5Constants::init_D };
    u32 m_count[2] { 0, 0 };

    u8 m_data_buffer[64] {};
};

}

}
