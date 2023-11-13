/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DNSServer.h"
#include "LookupServer.h"
#include <AK/IPv4Address.h>
#include <LibDNS/Packet.h>

namespace LookupServer {

using namespace DNS;

DNSServer::DNSServer(Core::EventReceiver* parent)
    : Core::UDPServer(parent)
{
    bind(IPv4Address(), 53);
    on_ready_to_receive = [this]() {
        auto result = handle_client();
        if (result.is_error()) {
            dbgln("DNSServer: Failed to handle client: {}", result.error());
        }
    };
}

ErrorOr<void> DNSServer::handle_client()
{
    sockaddr_in client_address;
    auto buffer = TRY(receive(1024, client_address));
    auto request = TRY(Packet::from_raw_packet(buffer));

    if (!request.is_query()) {
        dbgln("It's not a request");
        return {};
    }

    LookupServer& lookup_server = LookupServer::the();

    Packet response;
    response.set_is_response();
    response.set_id(request.id());

    for (auto& question : request.questions()) {
        if (question.class_code() != RecordClass::IN)
            continue;
        response.add_question(question);
        auto answers = TRY(lookup_server.lookup(question.name(), question.record_type()));
        for (auto& answer : answers) {
            response.add_answer(answer);
        }
    }

    if (response.answer_count() == 0)
        response.set_code(Packet::Code::NXDOMAIN);
    else
        response.set_code(Packet::Code::NOERROR);

    buffer = TRY(response.to_byte_buffer());

    TRY(send(buffer, client_address));
    return {};
}

}
