/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/IP/IPv4.h>

namespace Kernel {

class [[gnu::packed]] UDPPacket {
public:
    UDPPacket() = default;
    ~UDPPacket() = default;

    u16 source_port() const { return m_source_port; }
    void set_source_port(u16 port) { m_source_port = port; }

    u16 destination_port() const { return m_destination_port; }
    void set_destination_port(u16 port) { m_destination_port = port; }

    u16 length() const { return m_length; }
    void set_length(u16 length) { m_length = length; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 checksum) { m_checksum = checksum; }

    void const* payload() const { return &m_payload[0]; }
    void* payload() { return &m_payload[0]; }

private:
    NetworkOrdered<u16> m_source_port;
    NetworkOrdered<u16> m_destination_port;
    NetworkOrdered<u16> m_length;
    NetworkOrdered<u16> m_checksum;
    u8 m_payload[0];
};

static_assert(sizeof(UDPPacket) == 8);

}
