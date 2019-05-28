#pragma once

#include <Kernel/Net/IPv4.h>

struct TCPFlags {
    enum : word
    {
        FIN = 0x01,
        SYN = 0x02,
        RST = 0x04,
        PUSH = 0x08,
        ACK = 0x10,
        URG = 0x20
    };
};

class [[gnu::packed]] TCPPacket
{
public:
    TCPPacket() {}
    ~TCPPacket() {}

    size_t header_size() const { return data_offset() * sizeof(dword); }

    word source_port() const { return m_source_port; }
    void set_source_port(word port) { m_source_port = port; }

    word destination_port() const { return m_destination_port; }
    void set_destination_port(word port) { m_destination_port = port; }

    dword sequence_number() const { return m_sequence_number; }
    void set_sequence_number(dword number) { m_sequence_number = number; }

    dword ack_number() const { return m_ack_number; }
    void set_ack_number(dword number) { m_ack_number = number; }

    word flags() const { return m_flags_and_data_offset & 0x1ff; }
    void set_flags(word flags) { m_flags_and_data_offset = (m_flags_and_data_offset & ~0x1ff) | (flags & 0x1ff); }

    bool has_syn() const { return flags() & TCPFlags::SYN; }
    bool has_ack() const { return flags() & TCPFlags::ACK; }
    bool has_fin() const { return flags() & TCPFlags::FIN; }

    byte data_offset() const { return (m_flags_and_data_offset & 0xf000) >> 12; }
    void set_data_offset(word data_offset) { m_flags_and_data_offset = (m_flags_and_data_offset & ~0xf000) | data_offset << 12; }

    word window_size() const { return m_window_size; }
    void set_window_size(word window_size) { m_window_size = window_size; }

    word checksum() const { return m_checksum; }
    void set_checksum(word checksum) { m_checksum = checksum; }

    word urgent() const { return m_urgent; }
    void set_urgent(word urgent) { m_urgent = urgent; }

    const void* payload() const { return ((const byte*)this) + header_size(); }
    void* payload() { return ((byte*)this) + header_size(); }

private:
    NetworkOrdered<word> m_source_port;
    NetworkOrdered<word> m_destination_port;
    NetworkOrdered<dword> m_sequence_number;
    NetworkOrdered<dword> m_ack_number;

    NetworkOrdered<word> m_flags_and_data_offset;
    NetworkOrdered<word> m_window_size;
    NetworkOrdered<word> m_checksum;
    NetworkOrdered<word> m_urgent;
};

static_assert(sizeof(TCPPacket) == 20);
