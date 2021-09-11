/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSAnswer.h"
#include "DNSName.h"
#include "DNSPacket.h"
#include <AK/IPv4Address.h>
#include <LibCore/UDPServer.h>
#include <netinet/in.h>

namespace LookupServer {

class MulticastDNS : public Core::UDPServer {
    C_OBJECT(MulticastDNS)
public:
    Vector<DNSAnswer> lookup(const DNSName&, DNSRecordType record_type);

private:
    explicit MulticastDNS(Object* parent = nullptr);

    void announce();
    ErrorOr<size_t> emit_packet(const DNSPacket&, const sockaddr_in* destination = nullptr);

    void handle_packet();
    void handle_query(const DNSPacket&);

    Vector<IPv4Address> local_addresses() const;

    DNSName m_hostname;

    static constexpr sockaddr_in mdns_addr {
        AF_INET,
        // htons(5353)
        0xe914,
        // 224.0.0.251
        { 0xfb0000e0 },
        { 0 }
    };
};

}
