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
    return make<Messages::LaunchServer::GreetResponse>();
}

OwnPtr<Messages::LaunchServer::OpenURLResponse> ClientConnection::handle(const Messages::LaunchServer::OpenURL& request)
{
    if (!m_allowlist.is_empty()) {
        bool allowed = false;
        for (auto& allowed_handler : m_allowlist) {
            if (allowed_handler.handler_name == request.handler_name()
                && (allowed_handler.any_url || allowed_handler.urls.contains_slow(request.url()))) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            // You are not on the list, go home!
            did_misbehave(String::formatted("Client requested a combination of handler/URL that was not on the list: '{}' with '{}'", request.handler_name(), request.url()).characters());
            return {};
        }
    }

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

OwnPtr<Messages::LaunchServer::AddAllowedURLResponse> ClientConnection::handle(const Messages::LaunchServer::AddAllowedURL& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return {};
    }

    if (!request.url().is_valid()) {
        did_misbehave("Got request to allow invalid URL");
        return {};
    }

    m_allowlist.empend(String(), false, Vector<URL> { request.url() });

    return make<Messages::LaunchServer::AddAllowedURLResponse>();
}

OwnPtr<Messages::LaunchServer::AddAllowedHandlerWithAnyURLResponse> ClientConnection::handle(const Messages::LaunchServer::AddAllowedHandlerWithAnyURL& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return {};
    }

    if (request.handler_name().is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return {};
    }

    m_allowlist.empend(request.handler_name(), true, Vector<URL>());

    return make<Messages::LaunchServer::AddAllowedHandlerWithAnyURLResponse>();
}

OwnPtr<Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLsResponse> ClientConnection::handle(const Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLs& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return {};
    }

    if (request.handler_name().is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return {};
    }

    if (request.urls().is_empty()) {
        did_misbehave("Got request to allow empty URL list");
        return {};
    }

    m_allowlist.empend(request.handler_name(), false, request.urls());

    return make<Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLsResponse>();
}

OwnPtr<Messages::LaunchServer::SealAllowlistResponse> ClientConnection::handle(const Messages::LaunchServer::SealAllowlist&)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got more than one request to seal the allowed handlers list");
        return {};
    }

    return make<Messages::LaunchServer::SealAllowlistResponse>();
}

}
