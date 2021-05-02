/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "DNSPacket.h"
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

Messages::LookupServer::LookupNameResponse ClientConnection::handle(const Messages::LookupServer::LookupName& message)
{
    auto answers = LookupServer::the().lookup(message.name(), T_A);
    if (answers.is_empty())
        return { 1, Vector<String>() };
    Vector<String> addresses;
    for (auto& answer : answers) {
        addresses.append(answer.record_data());
    }
    return { 0, move(addresses) };
}

Messages::LookupServer::LookupAddressResponse ClientConnection::handle(const Messages::LookupServer::LookupAddress& message)
{
    if (message.address().length() != 4)
        return { 1, String() };
    IPv4Address address { (const u8*)message.address().characters() };
    auto name = String::formatted("{}.{}.{}.{}.in-addr.arpa",
        address[3],
        address[2],
        address[1],
        address[0]);
    auto answers = LookupServer::the().lookup(name, T_PTR);
    if (answers.is_empty())
        return { 1, String() };
    return { 0, answers[0].record_data() };
}
}
