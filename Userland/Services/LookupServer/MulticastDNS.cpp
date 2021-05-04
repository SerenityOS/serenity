/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MulticastDNS.h"
#include "DNSPacket.h"
#include <AK/IPv4Address.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

namespace LookupServer {

MulticastDNS::MulticastDNS(Object* parent)
    : Core::UDPServer(parent)
    , m_hostname("courage.local")
{
    char buffer[HOST_NAME_MAX];
    int rc = gethostname(buffer, sizeof(buffer));
    if (rc < 0) {
        perror("gethostname");
    } else {
        m_hostname = String::formatted("{}.local", buffer);
    }

    u8 zero = 0;
    rc = setsockopt(fd(), IPPROTO_IP, IP_MULTICAST_LOOP, &zero, 1);
    if (rc < 0)
        perror("setsockopt(IP_MULTICAST_LOOP)");
    ip_mreq mreq = {
        mdns_addr.sin_addr,
        { htonl(INADDR_ANY) },
    };
    rc = setsockopt(fd(), IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if (rc < 0)
        perror("setsockopt(IP_ADD_MEMBESHIP)");

    bind(IPv4Address(), 5353);

    on_ready_to_receive = [this]() {
        handle_packet();
    };

    // TODO: Announce on startup. We cannot just call announce() here,
    // because it races with the network interfaces getting configured.
}

void MulticastDNS::handle_packet()
{
    auto buffer = receive(1024);
    auto optional_packet = DNSPacket::from_raw_packet(buffer.data(), buffer.size());
    if (!optional_packet.has_value()) {
        dbgln("Got an invalid mDNS packet");
        return;
    }
    auto& packet = optional_packet.value();

    if (packet.is_query())
        handle_query(packet);
}

void MulticastDNS::handle_query(const DNSPacket& packet)
{
    bool should_reply = false;

    for (auto& question : packet.questions())
        if (question.name() == m_hostname)
            should_reply = true;

    if (!should_reply)
        return;

    announce();
}

void MulticastDNS::announce()
{
    DNSPacket response;
    response.set_is_response();
    response.set_code(DNSPacket::Code::NOERROR);

    for (auto& address : local_addresses()) {
        auto raw_addr = address.to_in_addr_t();
        DNSAnswer answer {
            m_hostname,
            T_A,
            C_IN | 0x8000,
            120,
            String { (const char*)&raw_addr, sizeof(raw_addr) }
        };
        response.add_answer(answer);
    }

    int rc = emit_packet(response);
    if (rc < 0)
        perror("Failed to emit response packet");
}

ssize_t MulticastDNS::emit_packet(const DNSPacket& packet, const sockaddr_in* destination)
{
    auto buffer = packet.to_byte_buffer();
    if (!destination)
        destination = &mdns_addr;
    return sendto(fd(), buffer.data(), buffer.size(), 0, (const sockaddr*)destination, sizeof(*destination));
}

Vector<IPv4Address> MulticastDNS::local_addresses() const
{
    auto file = Core::File::construct("/proc/net/adapters");
    if (!file->open(Core::IODevice::ReadOnly)) {
        dbgln("Failed to open /proc/net/adapters: {}", file->error_string());
        return {};
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());

    Vector<IPv4Address> addresses;

    json.value().as_array().for_each([&addresses](auto& value) {
        auto if_object = value.as_object();
        auto address = if_object.get("ipv4_address").to_string();
        auto ipv4_address = IPv4Address::from_string(address);
        // Skip unconfigured interfaces.
        if (!ipv4_address.has_value())
            return;
        // Skip loopback adapters.
        if (ipv4_address.value()[0] == IN_LOOPBACKNET)
            return;
        addresses.append(ipv4_address.value());
    });

    return addresses;
}

Vector<DNSAnswer> MulticastDNS::lookup(const DNSName& name, unsigned short record_type)
{
    DNSPacket request;
    request.set_is_query();
    request.add_question({ name, record_type, C_IN });

    int rc = emit_packet(request);
    if (rc < 0) {
        perror("failed to emit request packet");
        return {};
    }

    Vector<DNSAnswer> answers;

    // FIXME: It would be better not to block
    // the main loop while we wait for a response.
    while (true) {
        pollfd pfd { fd(), POLLIN, 0 };
        rc = poll(&pfd, 1, 1000);
        if (rc < 0) {
            perror("poll");
        } else if (rc == 0) {
            // Timed out.
            return {};
        }

        auto buffer = receive(1024);
        if (!buffer)
            return {};
        auto optional_packet = DNSPacket::from_raw_packet(buffer.data(), buffer.size());
        if (!optional_packet.has_value()) {
            dbgln("Got an invalid mDNS packet");
            continue;
        }
        auto& packet = optional_packet.value();

        if (packet.is_query())
            continue;

        for (auto& answer : packet.answers())
            if (answer.name() == name && answer.type() == record_type)
                answers.append(answer);
        if (!answers.is_empty())
            return answers;
    }
}

}
