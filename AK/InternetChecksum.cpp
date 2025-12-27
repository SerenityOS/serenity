/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/InternetChecksum.h>

namespace AK {

InternetChecksum::InternetChecksum(ReadonlyBytes bytes)
{
    update(bytes);
}

void InternetChecksum::update(ReadonlyBytes bytes)
{
    VERIFY(!m_uneven_payload);
    for (size_t i = 0; i < bytes.size() / sizeof(u16); ++i)
        m_state += ByteReader::load16(bytes.offset_pointer(i * sizeof(u16)));
    if (bytes.size() % 2 == 1) {
        m_state += bytes.last();
        m_uneven_payload = true;
    }
}

NetworkOrdered<u16> InternetChecksum::digest()
{
    u32 output = m_state;
    // While there are any bits above the bottom 16...
    while (output >> 16)
        // Drop the top bits, and add the carries to the sum.
        output = (output & 0xFFFF) + (output >> 16);

    // Return the one's complement (this is already network-ordered, so we
    // bypass the constructor of that).
    return bit_cast<NetworkOrdered<u16>>(static_cast<u16>(~output));
}

}
