/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/IP/IPv4.h>

namespace Kernel {

struct TCPFlags {
    enum : u16 {
        FIN = 0x01,
        SYN = 0x02,
        RST = 0x04,
        PSH = 0x08,
        ACK = 0x10,
        URG = 0x20
    };
};

enum class TCPOptionKind : u8 {
    End = 0,
    Nop = 1,
    MSS = 2,
    WindowScale = 3,
    SACKPermitted = 4,
    SACK = 5,
    Timestamp = 6,
};

class [[gnu::packed]] TCPOption {
public:
    TCPOptionKind kind() const { return m_kind; }
    u8 length() const { return m_length; }

protected:
    TCPOption(TCPOptionKind kind, u8 length)
        : m_kind(kind)
        , m_length(length) {};

private:
    TCPOptionKind m_kind { TCPOptionKind::End };
    u8 m_length { sizeof(TCPOption) };
};

class [[gnu::packed]] TCPOptionMSS : public TCPOption {
public:
    TCPOptionMSS(u16 value)
        : TCPOption(TCPOptionKind::MSS, sizeof(TCPOptionMSS))
        , m_value(value)
    {
    }

    u16 value() const { return m_value; }

private:
    NetworkOrdered<u16> m_value;
};

class [[gnu::packed]] TCPOptionWindowScale : public TCPOption {
public:
    TCPOptionWindowScale(u8 value)
        : TCPOption(TCPOptionKind::WindowScale, sizeof(TCPOptionWindowScale))
        , m_value(value)
    {
    }

    u8 value() const { return m_value; }

private:
    NetworkOrdered<u8> m_value;
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

    void const* payload() const { return ((u8 const*)this) + header_size(); }
    void* payload() { return ((u8*)this) + header_size(); }

    template<typename Callback>
    void for_each_option(Callback callback) const
    {
        auto const* next_option = (u8 const*)this + sizeof(TCPPacket);
        auto const* options_end = payload();
        while (next_option < options_end) {
            if ((size_t)options_end - (size_t)next_option < sizeof(TCPOption))
                return; // Not enough space left for another option
            auto const* option = (TCPOption const*)next_option;
            if (option->kind() == TCPOptionKind::End)
                return;
            if (option->kind() == TCPOptionKind::Nop) {
                next_option += 1;
                continue;
            }
            if (option->length() < sizeof(TCPOption))
                return; // minimal option length
            callback(*option);
            next_option += option->length();
        }
    }

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
