/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/MACAddress.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/IP/IPv4.h>

namespace Kernel {

struct ARPOperation {
    enum : u16 {
        Request = 1,
        Response = 2,
    };
};

struct ARPHardwareType {
    enum : u16 {
        Ethernet = 1,
    };
};

class [[gnu::packed]] ARPPacket {
public:
    u16 hardware_type() const { return m_hardware_type; }
    void set_hardware_type(u16 w) { m_hardware_type = w; }

    u16 protocol_type() const { return m_protocol_type; }
    void set_protocol_type(u16 w) { m_protocol_type = w; }

    u16 operation() const { return m_operation; }
    void set_operation(u16 w) { m_operation = w; }

    MACAddress const& sender_hardware_address() const { return m_sender_hardware_address; }
    void set_sender_hardware_address(MACAddress const& address) { m_sender_hardware_address = address; }

    IPv4Address const& sender_protocol_address() const { return m_sender_protocol_address; }
    void set_sender_protocol_address(IPv4Address const& address) { m_sender_protocol_address = address; }

    MACAddress const& target_hardware_address() const { return m_target_hardware_address; }
    void set_target_hardware_address(MACAddress const& address) { m_target_hardware_address = address; }

    IPv4Address const& target_protocol_address() const { return m_target_protocol_address; }
    void set_target_protocol_address(IPv4Address const& address) { m_target_protocol_address = address; }

private:
    NetworkOrdered<u16> m_hardware_type { ARPHardwareType::Ethernet };
    NetworkOrdered<u16> m_protocol_type { EtherType::IPv4 };
    u8 m_hardware_address_length { sizeof(MACAddress) };
    u8 m_protocol_address_length { sizeof(IPv4Address) };
    NetworkOrdered<u16> m_operation;
    MACAddress m_sender_hardware_address;
    IPv4Address m_sender_protocol_address;
    MACAddress m_target_hardware_address;
    IPv4Address m_target_protocol_address;
};

static_assert(sizeof(ARPPacket) == 28);

}
