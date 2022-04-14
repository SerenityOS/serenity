/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <LibCore/UDPServer.h>
#include <LibDNS/Answer.h>
#include <LibDNS/Name.h>
#include <LibDNS/Packet.h>
#include <netinet/in.h>

namespace LookupServer {

using namespace DNS;

class MulticastDNS : public Core::UDPServer {
    C_OBJECT(MulticastDNS)
public:
    Vector<Answer> lookup(Name const&, RecordType record_type);

private:
    explicit MulticastDNS(Object* parent = nullptr);

    void announce();
    ErrorOr<size_t> emit_packet(Packet const&, sockaddr_in const* destination = nullptr);

    void handle_packet();
    void handle_query(Packet const&);

    Vector<IPv4Address> local_addresses() const;

    Name m_hostname;

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
