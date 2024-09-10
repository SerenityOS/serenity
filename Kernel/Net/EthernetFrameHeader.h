/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/MACAddress.h>

class [[gnu::packed]] EthernetFrameHeader {
public:
    EthernetFrameHeader() = default;
    ~EthernetFrameHeader() = default;

    MACAddress destination() const { return m_destination; }
    void set_destination(MACAddress const& address) { m_destination = address; }

    MACAddress source() const { return m_source; }
    void set_source(MACAddress const& address) { m_source = address; }

    u16 ether_type() const { return m_ether_type; }
    void set_ether_type(u16 ether_type) { m_ether_type = ether_type; }

    void const* payload() const { return &m_payload[0]; }
    void* payload() { return &m_payload[0]; }

private:
    MACAddress m_destination;
    MACAddress m_source;
    NetworkOrdered<u16> m_ether_type;
    u8 m_payload[0];
};

static_assert(sizeof(EthernetFrameHeader) == 14);
