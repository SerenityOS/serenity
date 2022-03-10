/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "SessionExitInhibitor.h"

namespace SessionServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

void ConnectionFromClient::for_each_client(Function<void(ConnectionFromClient&)> callback)
{
    for (auto& it : s_connections) {
        callback(*it.value);
    }
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<SessionExitInhibitionClientEndpoint, SessionExitInhibitionServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ConnectionFromClient::~ConnectionFromClient()
{
}

void ConnectionFromClient::die()
{
    SessionExitInhibitor::the().allow_exit(client_id());
    s_connections.remove(client_id());
}

Messages::SessionExitInhibitionServer::IsExitInhibitedResponse ConnectionFromClient::is_exit_inhibited()
{
    return Messages::SessionExitInhibitionServer::IsExitInhibitedResponse(SessionExitInhibitor::the().is_exit_inhibited());
}

void ConnectionFromClient::inhibit_exit()
{
    SessionExitInhibitor::the().inhibit_exit(client_id());
}

void ConnectionFromClient::allow_exit()
{
    SessionExitInhibitor::the().allow_exit(client_id());
}

void ConnectionFromClient::on_inhibited_exit_prevented()
{
    async_on_inhibited_exit_prevented();
}

void ConnectionFromClient::report_inhibited_exit_prevention()
{
    SessionExitInhibitor::the().on_inhibited_exit_prevented();
}

}
