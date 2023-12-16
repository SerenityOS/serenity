/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "LookupServer.h"
#include <AK/IPv4Address.h>
#include <LibDNS/Packet.h>

namespace LookupServer {

using namespace DNS;

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<LookupClientEndpoint, LookupServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

Messages::LookupServer::LookupNameResponse ConnectionFromClient::lookup_name(ByteString const& name)
{
    auto maybe_answers = LookupServer::the().lookup(name, RecordType::A);
    if (maybe_answers.is_error()) {
        dbgln("LookupServer: Failed to lookup A record: {}", maybe_answers.error());
        return { 1, {} };
    }

    auto answers = maybe_answers.release_value();
    Vector<ByteString> addresses;
    for (auto& answer : answers) {
        addresses.append(answer.record_data());
    }
    return { 0, move(addresses) };
}

Messages::LookupServer::LookupAddressResponse ConnectionFromClient::lookup_address(ByteString const& address)
{
    if (address.length() != 4)
        return { 1, ByteString() };
    IPv4Address ip_address { (u8 const*)address.characters() };
    auto name = ByteString::formatted("{}.{}.{}.{}.in-addr.arpa",
        ip_address[3],
        ip_address[2],
        ip_address[1],
        ip_address[0]);

    auto maybe_answers = LookupServer::the().lookup(name, RecordType::PTR);
    if (maybe_answers.is_error()) {
        dbgln("LookupServer: Failed to lookup PTR record: {}", maybe_answers.error());
        return { 1, ByteString() };
    }

    auto answers = maybe_answers.release_value();
    if (answers.is_empty())
        return { 1, ByteString() };
    return { 0, answers[0].record_data() };
}
}
