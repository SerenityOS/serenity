#pragma once

#include <AK/NetworkOrdered.h>
#include <Kernel/Net/MACAddress.h>

class [[gnu::packed]] EthernetFrameHeader
{
public:
    EthernetFrameHeader() {}
    ~EthernetFrameHeader() {}

    MACAddress destination() const { return m_destination; }
    void set_destination(const MACAddress& address) { m_destination = address; }

    MACAddress source() const { return m_source; }
    void set_source(const MACAddress& address) { m_source = address; }

    word ether_type() const { return m_ether_type; }
    void set_ether_type(word ether_type) { m_ether_type = ether_type; }

    const void* payload() const { return &m_payload[0]; }
    void* payload() { return &m_payload[0]; }

private:
    MACAddress m_destination;
    MACAddress m_source;
    NetworkOrdered<word> m_ether_type;
    dword m_payload[0];
};

static_assert(sizeof(EthernetFrameHeader) == 14);
