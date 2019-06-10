#pragma once

#include <AK/NetworkOrdered.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/MACAddress.h>

struct ARPOperation {
    enum : word {
        Request = 1,
        Response = 2,
    };
};

struct ARPHardwareType {
    enum : word {
        Ethernet = 1,
    };
};

class [[gnu::packed]] ARPPacket
{
public:
    word hardware_type() const { return m_hardware_type; }
    void set_hardware_type(word w) { m_hardware_type = w; }

    word protocol_type() const { return m_protocol_type; }
    void set_protocol_type(word w) { m_protocol_type = w; }

    byte hardware_address_length() const { return m_hardware_address_length; }
    void set_hardware_address_length(byte b) { m_hardware_address_length = b; }

    byte protocol_address_length() const { return m_protocol_address_length; }
    void set_protocol_address_length(byte b) { m_protocol_address_length = b; }

    word operation() const { return m_operation; }
    void set_operation(word w) { m_operation = w; }

    const MACAddress& sender_hardware_address() const { return m_sender_hardware_address; }
    void set_sender_hardware_address(const MACAddress& address) { m_sender_hardware_address = address; }

    const IPv4Address& sender_protocol_address() const { return m_sender_protocol_address; }
    void set_sender_protocol_address(const IPv4Address& address) { m_sender_protocol_address = address; }

    const MACAddress& target_hardware_address() const { return m_target_hardware_address; }
    void set_target_hardware_address(const MACAddress& address) { m_target_hardware_address = address; }

    const IPv4Address& target_protocol_address() const { return m_target_protocol_address; }
    void set_target_protocol_address(const IPv4Address& address) { m_target_protocol_address = address; }

private:
    NetworkOrdered<word> m_hardware_type { ARPHardwareType::Ethernet };
    NetworkOrdered<word> m_protocol_type { EtherType::IPv4 };
    byte m_hardware_address_length { sizeof(MACAddress) };
    byte m_protocol_address_length { sizeof(IPv4Address) };
    NetworkOrdered<word> m_operation;
    MACAddress m_sender_hardware_address;
    IPv4Address m_sender_protocol_address;
    MACAddress m_target_hardware_address;
    IPv4Address m_target_protocol_address;
};

static_assert(sizeof(ARPPacket) == 28);
