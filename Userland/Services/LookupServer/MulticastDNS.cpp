/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Alexander Narsudinov <a.narsudinov@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MulticastDNS.h"
#include <AK/ByteString.h>
#include <AK/IPv4Address.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <limits.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

namespace LookupServer {

MulticastDNS::MulticastDNS(Core::EventReceiver* parent)
    : Core::UDPServer(parent)
    , m_hostname("courage.local")
{
    char buffer[_POSIX_HOST_NAME_MAX];
    if (gethostname(buffer, sizeof(buffer)) < 0) {
        perror("gethostname");
    } else {
        m_hostname = ByteString::formatted("{}.local", buffer);
    }

    u8 zero = 0;
    if (setsockopt(fd(), IPPROTO_IP, IP_MULTICAST_LOOP, &zero, 1) < 0)
        perror("setsockopt(IP_MULTICAST_LOOP)");
    ip_mreq mreq = {
        mdns_addr.sin_addr,
        { htonl(INADDR_ANY) },
    };
    if (setsockopt(fd(), IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq)) < 0)
        perror("setsockopt(IP_ADD_MEMBERSHIP)");

    bind(IPv4Address(), 5353);

    on_ready_to_receive = [this]() {
        if (auto result = handle_packet(); result.is_error()) {
            dbgln("Failed to handle packet: {}", result.error());
        }
    };

    // TODO: Announce on startup. We cannot just call announce() here,
    // because it races with the network interfaces getting configured.
}

ErrorOr<void> MulticastDNS::handle_packet()
{
    auto buffer = TRY(receive(1024));
    auto packet = TRY(Packet::from_raw_packet(buffer));
    if (packet.is_query())
        handle_query(packet);
    return {};
}

void MulticastDNS::handle_query(Packet const& packet)
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
    Packet response;
    response.set_is_response();
    response.set_code(Packet::Code::NOERROR);
    response.set_authoritative_answer(true);
    response.set_recursion_desired(false);
    response.set_recursion_available(false);

    for (auto& address : local_addresses()) {
        auto raw_addr = address.to_in_addr_t();
        Answer answer {
            m_hostname,
            RecordType::A,
            RecordClass::IN,
            120,
            ByteString { (char const*)&raw_addr, sizeof(raw_addr) },
            true,
        };
        response.add_answer(answer);
    }

    if (emit_packet(response).is_error())
        perror("Failed to emit response packet");
}

ErrorOr<size_t> MulticastDNS::emit_packet(Packet const& packet, sockaddr_in const* destination)
{
    auto buffer = TRY(packet.to_byte_buffer());
    if (!destination)
        destination = &mdns_addr;

    return send(buffer, *destination);
}

Vector<IPv4Address> MulticastDNS::local_addresses() const
{
    auto file_or_error = Core::File::open("/sys/kernel/net/adapters"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        dbgln("Failed to open /sys/kernel/net/adapters: {}", file_or_error.error());
        return {};
    }
    auto file_contents_or_error = file_or_error.value()->read_until_eof();
    if (file_or_error.is_error()) {
        dbgln("Cannot read /sys/kernel/net/adapters: {}", file_contents_or_error.error());
        return {};
    }
    auto json_or_error = JsonValue::from_string(file_contents_or_error.value());
    if (json_or_error.is_error()) {
        dbgln("Invalid JSON(?) in /sys/kernel/net/adapters: {}", json_or_error.error());
        return {};
    }

    Vector<IPv4Address> addresses;

    json_or_error.value().as_array().for_each([&addresses](auto& value) {
        auto if_object = value.as_object();
        auto address = if_object.get_byte_string("ipv4_address"sv).value_or({});
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

ErrorOr<Vector<Answer>> MulticastDNS::lookup(Name const& name, RecordType record_type)
{
    Packet request;
    request.set_is_query();
    request.set_recursion_desired(false);
    request.add_question({ name, record_type, RecordClass::IN, false });

    TRY(emit_packet(request));
    Vector<Answer> answers;

    // FIXME: It would be better not to block
    // the main loop while we wait for a response.
    while (true) {
        auto pfd = pollfd { fd(), POLLIN, 0 };
        auto rc = TRY(Core::System::poll({ &pfd, 1 }, 1000));
        if (rc == 0) {
            // Timed out.
            return Vector<Answer> {};
        }
        auto buffer = TRY(receive(1024));
        if (buffer.is_empty())
            return Vector<Answer> {};
        auto packet_or_error = Packet::from_raw_packet(buffer);
        if (packet_or_error.is_error()) {
            dbgln("Got an invalid mDNS packet: {}", packet_or_error.release_error());
            continue;
        }
        auto packet = packet_or_error.release_value();

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
