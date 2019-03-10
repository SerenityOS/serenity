#pragma once

#include <Kernel/MACAddress.h>

class [[gnu::packed]] EthernetFrameHeader {
public:
    EthernetFrameHeader() { }
    ~EthernetFrameHeader() { }

    MACAddress destination() const { return m_destination; }
    void set_destination(const MACAddress& address) { m_destination = address; }

    MACAddress source() const { return m_source; }
    void set_source(const MACAddress& address) { m_source = address; }

    word ether_type() const { return (m_ether_type & 0xff) << 16 | ((m_ether_type >> 16) & 0xff); }
    void set_ether_type(word ether_type) { m_ether_type = (ether_type & 0xff) << 16 | ((ether_type >> 16) & 0xff); }

    const void* raw() const { return this; }
    void* raw() { return this; }

private:
    MACAddress m_destination;
    MACAddress m_source;
    word m_ether_type { 0 };
};

static_assert(sizeof(EthernetFrameHeader) == 14);

