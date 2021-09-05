/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Net/IPv4.h>

namespace Kernel {

struct TCPFlags {
    enum : u16 {
        FIN = 0x01,
        SYN = 0x02,
        RST = 0x04,
        PUSH = 0x08,
        ACK = 0x10,
        URG = 0x20
    };
};

class [[gnu::packed]] TCPOptionMSS {
public:
    TCPOptionMSS(u16 value)
        : m_value(value)
    {
    }

    u16 value() const { return m_value; }

private:
    u8 m_option_kind { 0x02 };
    u8 m_option_length { sizeof(TCPOptionMSS) };
    NetworkOrdered<u16> m_value;
};

static_assert(AssertSize<TCPOptionMSS, 4>());

class [[gnu::packed]] TCPPacket {
public:
    TCPPacket() = default;
    ~TCPPacket() = default;

    size_t header_size() const { return data_offset() * sizeof(u32); }

    u16 source_port() const { return m_source_port; }
    void set_source_port(u16 port) { m_source_port = port; }

    u16 destination_port() const { return m_destination_port; }
    void set_destination_port(u16 port) { m_destination_port = port; }

    u32 sequence_number() const { return m_sequence_number; }
    void set_sequence_number(u32 number) { m_sequence_number = number; }

    u32 ack_number() const { return m_ack_number; }
    void set_ack_number(u32 number) { m_ack_number = number; }

    u16 flags() const { return m_flags_and_data_offset & 0x1ff; }
    void set_flags(u16 flags) { m_flags_and_data_offset = (m_flags_and_data_offset & ~0x1ff) | (flags & 0x1ff); }

    bool has_syn() const { return flags() & TCPFlags::SYN; }
    bool has_ack() const { return flags() & TCPFlags::ACK; }
    bool has_fin() const { return flags() & TCPFlags::FIN; }
    bool has_rst() const { return flags() & TCPFlags::RST; }

    u8 data_offset() const { return (m_flags_and_data_offset & 0xf000) >> 12; }
    void set_data_offset(u16 data_offset) { m_flags_and_data_offset = (m_flags_and_data_offset & ~0xf000) | data_offset << 12; }

    u16 window_size() const { return m_window_size; }
    void set_window_size(u16 window_size) { m_window_size = window_size; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 checksum) { m_checksum = checksum; }

    u16 urgent() const { return m_urgent; }
    void set_urgent(u16 urgent) { m_urgent = urgent; }

    const void* payload() const { return ((const u8*)this) + header_size(); }
    void* payload() { return ((u8*)this) + header_size(); }

private:
    NetworkOrdered<u16> m_source_port;
    NetworkOrdered<u16> m_destination_port;
    NetworkOrdered<u32> m_sequence_number;
    NetworkOrdered<u32> m_ack_number;

    NetworkOrdered<u16> m_flags_and_data_offset;
    NetworkOrdered<u16> m_window_size;
    NetworkOrdered<u16> m_checksum;
    NetworkOrdered<u16> m_urgent;
};

static_assert(AssertSize<TCPPacket, 20>());

}
