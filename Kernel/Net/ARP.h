/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MACAddress.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/IPv4.h>

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

    u8 hardware_address_length() const { return m_hardware_address_length; }
    void set_hardware_address_length(u8 b) { m_hardware_address_length = b; }

    u8 protocol_address_length() const { return m_protocol_address_length; }
    void set_protocol_address_length(u8 b) { m_protocol_address_length = b; }

    u16 operation() const { return m_operation; }
    void set_operation(u16 w) { m_operation = w; }

    const MACAddress& sender_hardware_address() const { return m_sender_hardware_address; }
    void set_sender_hardware_address(const MACAddress& address) { m_sender_hardware_address = address; }

    const IPv4Address& sender_protocol_address() const { return m_sender_protocol_address; }
    void set_sender_protocol_address(const IPv4Address& address) { m_sender_protocol_address = address; }

    const MACAddress& target_hardware_address() const { return m_target_hardware_address; }
    void set_target_hardware_address(const MACAddress& address) { m_target_hardware_address = address; }

    const IPv4Address& target_protocol_address() const { return m_target_protocol_address; }
    void set_target_protocol_address(const IPv4Address& address) { m_target_protocol_address = address; }

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
