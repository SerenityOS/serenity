#pragma once

#include <AK/AKString.h>
#include <AK/Assertions.h>
#include <AK/IPv4Address.h>
#include <AK/NetworkOrdered.h>
#include <AK/Types.h>

enum class IPv4Protocol : word {
    ICMP = 1,
    TCP = 6,
    UDP = 17,
};

NetworkOrdered<word> internet_checksum(const void*, size_t);

class [[gnu::packed]] IPv4Packet
{
public:
    byte version() const { return (m_version_and_ihl >> 4) & 0xf; }
    void set_version(byte version) { m_version_and_ihl = (m_version_and_ihl & 0x0f) | (version << 4); }

    byte internet_header_length() const { return m_version_and_ihl & 0xf; }
    void set_internet_header_length(byte ihl) { m_version_and_ihl = (m_version_and_ihl & 0xf0) | (ihl & 0x0f); }

    word length() const { return m_length; }
    void set_length(word length) { m_length = length; }

    word ident() const { return m_ident; }
    void set_ident(word ident) { m_ident = ident; }

    byte ttl() const { return m_ttl; }
    void set_ttl(byte ttl) { m_ttl = ttl; }

    byte protocol() const { return m_protocol; }
    void set_protocol(byte protocol) { m_protocol = protocol; }

    word checksum() const { return m_checksum; }
    void set_checksum(word checksum) { m_checksum = checksum; }

    const IPv4Address& source() const { return m_source; }
    void set_source(const IPv4Address& address) { m_source = address; }

    const IPv4Address& destination() const { return m_destination; }
    void set_destination(const IPv4Address& address) { m_destination = address; }

    void* payload() { return this + 1; }
    const void* payload() const { return this + 1; }

    word payload_size() const { return m_length - sizeof(IPv4Packet); }

    NetworkOrdered<word> compute_checksum() const
    {
        ASSERT(!m_checksum);
        return internet_checksum(this, sizeof(IPv4Packet));
    }

private:
    byte m_version_and_ihl { 0 };
    byte m_dscp_and_ecn { 0 };
    NetworkOrdered<word> m_length;
    NetworkOrdered<word> m_ident;
    NetworkOrdered<word> m_flags_and_fragment;
    byte m_ttl { 0 };
    NetworkOrdered<byte> m_protocol;
    NetworkOrdered<word> m_checksum;
    IPv4Address m_source;
    IPv4Address m_destination;
};

static_assert(sizeof(IPv4Packet) == 20);

inline NetworkOrdered<word> internet_checksum(const void* ptr, size_t count)
{
    dword checksum = 0;
    auto* w = (const word*)ptr;
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
