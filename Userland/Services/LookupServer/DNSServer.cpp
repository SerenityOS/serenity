/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
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

#include "DNSServer.h"
#include "DNSPacket.h"
#include "LookupServer.h"
#include <AK/IPv4Address.h>

namespace LookupServer {

DNSServer::DNSServer(Object* parent)
    : Core::UDPServer(parent)
{
    bind(IPv4Address(), 53);
    on_ready_to_receive = [this]() {
        handle_client();
    };
}

void DNSServer::handle_client()
{
    sockaddr_in client_address;
    auto buffer = receive(1024, client_address);
    auto optional_request = DNSPacket::from_raw_packet(buffer.data(), buffer.size());
    if (!optional_request.has_value()) {
        dbgln("Got an invalid DNS packet");
        return;
    }
    auto& request = optional_request.value();

    if (!request.is_query()) {
        dbgln("It's not a request");
        return;
    }

    LookupServer& lookup_server = LookupServer::the();

    DNSPacket response;
    response.set_is_response();
    response.set_id(request.id());

    for (auto& question : request.questions()) {
        if (question.class_code() != C_IN)
            continue;
        response.add_question(question);
        auto answers = lookup_server.lookup(question.name(), question.record_type());
        for (auto& answer : answers) {
            response.add_answer(answer);
        }
    }

    if (response.answer_count() == 0)
        response.set_code(DNSPacket::Code::NXDOMAIN);
    else
        response.set_code(DNSPacket::Code::NOERROR);

    buffer = response.to_byte_buffer();
    sendto(fd(), buffer.data(), buffer.size(), 0, (const sockaddr*)&client_address, sizeof(client_address));
}

}
