/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Span.h>
#include <YAK/Types.h>
#include <LibCrypto/Checksum/CRC32.h>

namespace Crypto::Checksum {

void CRC32::update(ReadonlyBytes data)
{
    for (size_t i = 0; i < data.size(); i++) {
        m_state = table[(m_state ^ data.at(i)) & 0xFF] ^ (m_state >> 8);
    }
};

u32 CRC32::digest()
{
    return ~m_state;
}

}
