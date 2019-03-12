#pragma once

#include <Kernel/MACAddress.h>
#include <Kernel/IPv4Packet.h>
#include <Kernel/NetworkOrdered.h>

struct ICMPType {
enum {
    EchoReply = 0,
    EchoRequest = 8,
};
};

class [[gnu::packed]] ICMPHeader {
public:
    ICMPHeader() { }
    ~ICMPHeader() { }

    byte type() const { return m_type; }
    void set_type(byte b) { m_type = b; }

    byte code() const { return m_code; }
    void set_code(byte b) { m_code = b; }

    word checksum() const { return ntohs(m_checksum); }
    void set_checksum(word w) { m_checksum = htons(w); }

    const void* payload() const { return &m_payload[0]; }
    void* payload() { return &m_payload[0]; }

private:
    byte m_type { 0 };
    byte m_code { 0 };
    word m_checksum { 0 };
    dword m_rest_of_header { 0 };
    byte m_payload[0];
};

static_assert(sizeof(ICMPHeader) == 8);

struct [[gnu::packed]] IPv4ICMPPacket {
    IPv4Packet ipv4_packet;
    ICMPHeader icmp_header;
};

struct [[gnu::packed]] ICMPEchoPacket {
    ICMPHeader header;
    NetworkOrdered<word> identifier;
    NetworkOrdered<word> sequence_number;
    byte payload[];
};
