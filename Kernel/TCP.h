#pragma once

#include <Kernel/IPv4.h>

class [[gnu::packed]] TCPPacket {
public:
    TCPPacket() { }
    ~TCPPacket() { }

    word source_port() const { return m_source_port; }
    void set_source_port(word port) { m_source_port = port; }

    word destination_port() const { return m_destination_port; }
    void set_destination_port(word port) { m_destination_port = port; }

    dword sequence_number() const { return m_sequence_number; }
    void set_sequence_number(dword number) { m_sequence_number = number; }

    dword ack_number() const { return m_ack_number; }
    void set_ack_number(dword number) { m_ack_number = number; }

    word flags() const { return m_flags; }
    void set_flags(word flags) { m_flags = flags; }

    word window_size() const { return m_window_size; }
    void set_window_size(word window_size) { m_window_size = window_size; }

    word checksum() const { return m_checksum; }
    void set_checksum(word checksum) { m_checksum = checksum; }

    word urgent() const { return m_urgent; }
    void set_urgent(word urgent) { m_urgent = urgent; }

    const void* payload() const { return this + 1; }
    void* payload() { return this + 1; }

private:
    NetworkOrdered<word> m_source_port;
    NetworkOrdered<word> m_destination_port;
    NetworkOrdered<dword> m_sequence_number;
    NetworkOrdered<dword> m_ack_number;

    NetworkOrdered<word> m_flags;
    NetworkOrdered<word> m_window_size;
    NetworkOrdered<word> m_checksum;
    NetworkOrdered<word> m_urgent;
};

static_assert(sizeof(UDPPacket) == 8);
