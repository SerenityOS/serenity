/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Crypto::Checksum {

#if defined(__aarch64__) && defined(__ARM_FEATURE_CRC32)

void CRC32::update(ReadonlyBytes span)
{
    // FIXME: Does this require runtime checking on rpi?
    //        (Maybe the instruction is present on the rpi4 but not on the rpi3?)

    u8 const* data = span.data();
    size_t size = span.size();

    while (size > 0 && (reinterpret_cast<FlatPtr>(data) & 7) != 0) {
        m_state = __builtin_arm_crc32b(m_state, *data);
        ++data;
        --size;
    }

    auto* data64 = reinterpret_cast<u64 const*>(data);
    while (size >= 8) {
        m_state = __builtin_arm_crc32d(m_state, *data64);
        ++data64;
        size -= 8;
    }

    data = reinterpret_cast<u8 const*>(data64);
    while (size > 0) {
        m_state = __builtin_arm_crc32b(m_state, *data);
        ++data;
        --size;
    }
};

    // FIXME: On Intel, use _mm_crc32_u8 / _mm_crc32_u64 if available (SSE 4.2).

#else

static constexpr auto generate_table()
{
    Array<u32, 256> data {};
    for (auto i = 0u; i < data.size(); i++) {
        u32 value = i;

        for (auto j = 0; j < 8; j++) {
            if (value & 1) {
                value = 0xEDB88320 ^ (value >> 1);
            } else {
                value = value >> 1;
            }
        }

        data[i] = value;
    }
    return data;
}

static constexpr auto table = generate_table();

void CRC32::update(ReadonlyBytes data)
{
    for (size_t i = 0; i < data.size(); i++) {
        m_state = table[(m_state ^ data.at(i)) & 0xFF] ^ (m_state >> 8);
    }
};

#endif

u32 CRC32::digest()
{
    return ~m_state;
}

}
