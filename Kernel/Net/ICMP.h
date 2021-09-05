/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MACAddress.h>
#include <Kernel/Net/IPv4.h>

struct ICMPType {
    enum {
        EchoReply = 0,
        EchoRequest = 8,
    };
};

class [[gnu::packed]] ICMPHeader {
public:
    ICMPHeader() = default;
    ~ICMPHeader() = default;

    u8 type() const { return m_type; }
    void set_type(u8 b) { m_type = b; }

    u8 code() const { return m_code; }
    void set_code(u8 b) { m_code = b; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 w) { m_checksum = w; }

    const void* payload() const { return this + 1; }
    void* payload() { return this + 1; }

private:
    u8 m_type { 0 };
    u8 m_code { 0 };
    NetworkOrdered<u16> m_checksum { 0 };
    // NOTE: The rest of the header is 4 bytes
};

static_assert(AssertSize<ICMPHeader, 4>());

struct [[gnu::packed]] ICMPEchoPacket {
    ICMPHeader header;
    NetworkOrdered<u16> identifier;
    NetworkOrdered<u16> sequence_number;
    void* payload() { return this + 1; }
    const void* payload() const { return this + 1; }
};
