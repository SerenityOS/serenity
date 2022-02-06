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

ClientConnection::ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
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

Messages::LookupServer::LookupNameResponse ClientConnection::lookup_name(String const& name)
{
    auto maybe_answers = LookupServer::the().lookup(name, DNSRecordType::A);
    if (maybe_answers.is_error()) {
        dbgln("LookupServer: Failed to lookup PTR record: {}", maybe_answers.error());
    }

    auto answers = maybe_answers.release_value();
    Vector<String> addresses;
    for (auto& answer : answers) {
        addresses.append(answer.record_data());
    }
    return { 0, move(addresses) };
}

Messages::LookupServer::LookupAddressResponse ClientConnection::lookup_address(String const& address)
{
    if (address.length() != 4)
        return { 1, String() };
    IPv4Address ip_address { (const u8*)address.characters() };
    auto name = String::formatted("{}.{}.{}.{}.in-addr.arpa",
        ip_address[3],
        ip_address[2],
        ip_address[1],
        ip_address[0]);

    auto maybe_answers = LookupServer::the().lookup(name, DNSRecordType::PTR);
    if (maybe_answers.is_error()) {
        dbgln("LookupServer: Failed to lookup PTR record: {}", maybe_answers.error());
    }

    auto answers = maybe_answers.release_value();
    if (answers.is_empty())
        return { 1, String() };
    return { 0, answers[0].record_data() };
}
}
