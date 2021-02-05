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

#include "ClientConnection.h"
#include "DNSRequest.h"
#include "LookupServer.h"
#include <AK/IPv4Address.h>

namespace LookupServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(AK::NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<LookupClientEndpoint, LookupServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

OwnPtr<Messages::LookupServer::LookupNameResponse> ClientConnection::handle(const Messages::LookupServer::LookupName& message)
{
    auto addresses = LookupServer::the().lookup(message.name(), T_A);
    if (addresses.is_empty())
        return make<Messages::LookupServer::LookupNameResponse>(1, Vector<String>());
    return make<Messages::LookupServer::LookupNameResponse>(0, move(addresses));
}

OwnPtr<Messages::LookupServer::LookupAddressResponse> ClientConnection::handle(const Messages::LookupServer::LookupAddress& message)
{
    auto optional_address = IPv4Address::from_string(message.address());
    if (!optional_address.has_value())
        return make<Messages::LookupServer::LookupAddressResponse>(1, String());
    auto& address = optional_address.value();
    auto name = String::formatted("{}.{}.{}.{}.in-addr.arpa",
        address[3],
        address[2],
        address[1],
        address[0]);
    auto hosts = LookupServer::the().lookup(name, T_PTR);
    if (hosts.is_empty())
        return make<Messages::LookupServer::LookupAddressResponse>(1, String());
    return make<Messages::LookupServer::LookupAddressResponse>(0, hosts[0]);
}
}
