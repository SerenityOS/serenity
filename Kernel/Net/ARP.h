/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NetworkOrdered.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/MACAddress.h>

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

class [[gnu::packed]] ARPPacket
{
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
