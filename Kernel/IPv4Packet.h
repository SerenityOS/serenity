#pragma once

#include <AK/Types.h>
#include <Kernel/IPv4Address.h>

struct IPv4Protocol {
enum {
    ICMP = 1,
};
};

class [[gnu::packed]] IPv4Packet {
public:
    byte version() const { return (m_version_and_ihl >> 4) & 0xf; }
    void set_version(byte version) { m_version_and_ihl = (m_version_and_ihl & 0x0f) | (version << 4); }

    byte internet_header_length() const { return m_version_and_ihl & 0xf; }
    void set_internet_header_length(byte ihl) { m_version_and_ihl = (m_version_and_ihl & 0xf0) | (ihl & 0x0f); }

    word length() const { return ntohs(m_length); }
    void set_length(word length) { m_length = htons(length); }

    word ident() const { return ntohs(m_ident); }
    void set_ident(word ident) { m_ident = htons(ident); }

    byte ttl() const { return m_ttl; }
    void set_ttl(byte ttl) { m_ttl = ttl; }

    byte protocol() const { return m_protocol; }
    void set_protocol(byte protocol) { m_protocol = protocol; }

    word checksum() const { return ntohs(m_checksum); }
    void set_checksum(word checksum) { m_checksum = htons(checksum); }

    const IPv4Address& source() const { return m_source; }
    void set_source(const IPv4Address& address) { m_source = address; }

    const IPv4Address& destination() const { return m_destination; }
    void set_destination(const IPv4Address& address) { m_destination = address; }

    void* payload() { return m_payload; }
    const void* payload() const { return m_payload; }

private:
    byte m_version_and_ihl;
    byte m_dscp_and_ecn;
    word m_length;
    word m_ident;
    word m_flags_and_fragment;
    byte m_ttl;
    byte m_protocol;
    word m_checksum;
    IPv4Address m_source;
    IPv4Address m_destination;
    byte m_payload[0];
};

static_assert(sizeof(IPv4Packet) == 20);
