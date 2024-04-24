/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Alexander Narsudinov <a.narsudinov@gmail.com>
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
    ErrorOr<Vector<Answer>> lookup(Name const&, RecordType record_type);

private:
    explicit MulticastDNS(Core::EventReceiver* parent = nullptr);

    void announce();
    ErrorOr<size_t> emit_packet(Packet const&, sockaddr_in const* destination = nullptr);

    ErrorOr<void> handle_packet();
    void handle_query(Packet const&);

    Vector<IPv4Address> local_addresses() const;

    Name m_hostname;

    static constexpr sockaddr_in mdns_addr {
#if defined(AK_OS_BSD_GENERIC) || defined(AK_OS_GNU_HURD)
        .sin_len = sizeof(struct sockaddr_in),
#endif
        .sin_family = AF_INET,
        // htons(5353)
        .sin_port = 0xe914,
        // 224.0.0.251
        .sin_addr = { 0xfb0000e0 },
        .sin_zero = { 0 }
    };
};

}
