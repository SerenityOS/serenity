#pragma once

#include <Kernel/Net/IPv4.h>

class [[gnu::packed]] UDPPacket
{
public:
    UDPPacket() {}
    ~UDPPacket() {}

    word source_port() const { return m_source_port; }
    void set_source_port(word port) { m_source_port = port; }

    word destination_port() const { return m_destination_port; }
    void set_destination_port(word port) { m_destination_port = port; }

    word length() const { return m_length; }
    void set_length(word length) { m_length = length; }

    word checksum() const { return m_checksum; }
    void set_checksum(word checksum) { m_checksum = checksum; }

    const void* payload() const { return this + 1; }
    void* payload() { return this + 1; }

private:
    NetworkOrdered<word> m_source_port;
    NetworkOrdered<word> m_destination_port;
    NetworkOrdered<word> m_length;
    NetworkOrdered<word> m_checksum;
};

static_assert(sizeof(UDPPacket) == 8);
