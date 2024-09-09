/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2024, sdomi <ja@sdomi.pl>
 * Copyright (c) 2024, Wanda <wanda@phinode.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

// IPv4 "Protocol" field or IPv6 "Next Header" field.
// Included in this enum are only transport layer protocols, not IPv6 extended headers.
// https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
enum class TransportProtocol : u8 {
    ICMP = 1,
    TCP = 6,
    UDP = 17,
    ICMPv6 = 58,
};

class InternetChecksum {
public:
    void add(ReadonlyBytes bytes)
    {
        VERIFY(!m_uneven_payload);
        Span<u16 const> words { bit_cast<u16 const*>(bytes.data()), bytes.size() / 2 };
        for (u16 w : words) {
            m_checksum += AK::convert_between_host_and_network_endian(w);
            if (m_checksum & 0x80000000)
                m_checksum = (m_checksum & 0xffff) | (m_checksum >> 16);
        }
        if (bytes.size() % 2 == 1) {
            m_checksum += (bytes.last()) << 8;
            m_uneven_payload = true;
        }
    }
    NetworkOrdered<u16> finish()
    {
        while (m_checksum >> 16)
            m_checksum = (m_checksum & 0xffff) + (m_checksum >> 16);
        return ~m_checksum & 0xffff;
    }

private:
    u32 m_checksum { 0 };
    bool m_uneven_payload { false };
};

}
