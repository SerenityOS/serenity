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

#include <AK/String.h>
#include <AK/Assertions.h>
#include <AK/IPv4Address.h>
#include <AK/NetworkOrdered.h>
#include <AK/Types.h>

namespace Kernel {

enum class IPv4Protocol : u16 {
    ICMP = 1,
    TCP = 6,
    UDP = 17,
};

NetworkOrdered<u16> internet_checksum(const void*, size_t);

class [[gnu::packed]] IPv4Packet
{
public:
    u8 version() const { return (m_version_and_ihl >> 4) & 0xf; }
    void set_version(u8 version) { m_version_and_ihl = (m_version_and_ihl & 0x0f) | (version << 4); }

    u8 internet_header_length() const { return m_version_and_ihl & 0xf; }
    void set_internet_header_length(u8 ihl) { m_version_and_ihl = (m_version_and_ihl & 0xf0) | (ihl & 0x0f); }

    u16 length() const { return m_length; }
    void set_length(u16 length) { m_length = length; }

    u16 ident() const { return m_ident; }
    void set_ident(u16 ident) { m_ident = ident; }

    u8 ttl() const { return m_ttl; }
    void set_ttl(u8 ttl) { m_ttl = ttl; }

    u8 protocol() const { return m_protocol; }
    void set_protocol(u8 protocol) { m_protocol = protocol; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 checksum) { m_checksum = checksum; }

    const IPv4Address& source() const { return m_source; }
    void set_source(const IPv4Address& address) { m_source = address; }

    const IPv4Address& destination() const { return m_destination; }
    void set_destination(const IPv4Address& address) { m_destination = address; }

    void* payload() { return this + 1; }
    const void* payload() const { return this + 1; }

    u16 payload_size() const { return m_length - sizeof(IPv4Packet); }

    NetworkOrdered<u16> compute_checksum() const
    {
        ASSERT(!m_checksum);
        return internet_checksum(this, sizeof(IPv4Packet));
    }

private:
    u8 m_version_and_ihl { 0 };
    u8 m_dscp_and_ecn { 0 };
    NetworkOrdered<u16> m_length;
    NetworkOrdered<u16> m_ident;
    NetworkOrdered<u16> m_flags_and_fragment;
    u8 m_ttl { 0 };
    NetworkOrdered<u8> m_protocol;
    NetworkOrdered<u16> m_checksum;
    IPv4Address m_source;
    IPv4Address m_destination;
};

static_assert(sizeof(IPv4Packet) == 20);

inline NetworkOrdered<u16> internet_checksum(const void* ptr, size_t count)
{
    u32 checksum = 0;
    auto* w = (const u16*)ptr;
    while (count > 1) {
        checksum += convert_between_host_and_network(*w++);
        if (checksum & 0x80000000)
            checksum = (checksum & 0xffff) | (checksum >> 16);
        count -= 2;
    }
    while (checksum >> 16)
        checksum = (checksum & 0xffff) + (checksum >> 16);
    return ~checksum & 0xffff;
}

}
