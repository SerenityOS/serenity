/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/Adler32.h>

namespace Crypto::Checksum {

void Adler32::update(ReadonlyBytes data)
{
    for (size_t i = 0; i < data.size(); i++) {
        m_state_a = (m_state_a + data.at(i)) % 65521;
        m_state_b = (m_state_b + m_state_a) % 65521;
    }
}

u32 Adler32::digest()
{
    return (m_state_b << 16) | m_state_a;
}

}
