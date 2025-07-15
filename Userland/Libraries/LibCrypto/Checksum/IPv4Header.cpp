/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <AK/NumericLimits.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <LibCrypto/Checksum/IPv4Header.h>
#include <arpa/inet.h>

namespace Crypto::Checksum {

void IPv4Header::update(ReadonlyBytes data)
{
    for (size_t i = 0; i < data.size() / sizeof(u16); ++i)
        // Dealing with the byte order isn't technically a part of the checksumming
        // process, because it's expected that you'd already have the packet decoded,
        // but since we're dealing with raw data, the responsibility of decoding (also)
        // falls on us.
        m_state += AK::convert_between_host_and_network_endian(ByteReader::load16(data.offset_pointer(i * sizeof(u16))));
}

u16 IPv4Header::digest()
{
    u32 output = m_state;
    // While there are any bits above the bottom 16...
    while (output >> 16)
        // Drop the top bits, and add the carries to the sum.
        output = (output & 0xFFFF) + (output >> 16);

    // Return the one's complement.
    return htons(~static_cast<u16>(output));
}

}
