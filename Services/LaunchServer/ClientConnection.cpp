/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
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
#include "Launcher.h"
#include <AK/HashMap.h>
#include <AK/URL.h>
#include <LaunchServer/LaunchClientEndpoint.h>

namespace LaunchServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;
ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<LaunchClientEndpoint, LaunchServerEndpoint>(*this, move(client_socket), client_id)
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

OwnPtr<Messages::LaunchServer::GreetResponse> ClientConnection::handle(const Messages::LaunchServer::Greet&)
{
    return make<Messages::LaunchServer::GreetResponse>(client_id());
}

OwnPtr<Messages::LaunchServer::OpenURLResponse> ClientConnection::handle(const Messages::LaunchServer::OpenURL& request)
{
    URL url(request.url());
    auto result = Launcher::the().open_url(url, request.handler_name());
    return make<Messages::LaunchServer::OpenURLResponse>(result);
}

OwnPtr<Messages::LaunchServer::GetHandlersForURLResponse> ClientConnection::handle(const Messages::LaunchServer::GetHandlersForURL& request)
{
    URL url(request.url());
    auto result = Launcher::the().handlers_for_url(url);
    return make<Messages::LaunchServer::GetHandlersForURLResponse>(result);
}

OwnPtr<Messages::LaunchServer::GetHandlersWithDetailsForURLResponse> ClientConnection::handle(const Messages::LaunchServer::GetHandlersWithDetailsForURL& request)
{
    URL url(request.url());
    auto result = Launcher::the().handlers_with_details_for_url(url);
    return make<Messages::LaunchServer::GetHandlersWithDetailsForURLResponse>(result);
}

}
