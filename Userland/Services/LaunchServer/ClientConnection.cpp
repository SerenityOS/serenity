/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

void ClientConnection::handle(const Messages::LaunchServer::Greet&)
{
}

Messages::LaunchServer::OpenURLResponse ClientConnection::handle(const Messages::LaunchServer::OpenURL& request)
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
            return nullptr;
        }
    }

    URL url(request.url());
    return Launcher::the().open_url(url, request.handler_name());
}

Messages::LaunchServer::GetHandlersForURLResponse ClientConnection::handle(const Messages::LaunchServer::GetHandlersForURL& request)
{
    URL url(request.url());
    return Launcher::the().handlers_for_url(url);
}

Messages::LaunchServer::GetHandlersWithDetailsForURLResponse ClientConnection::handle(const Messages::LaunchServer::GetHandlersWithDetailsForURL& request)
{
    URL url(request.url());
    return Launcher::the().handlers_with_details_for_url(url);
}

void ClientConnection::handle(const Messages::LaunchServer::AddAllowedURL& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (!request.url().is_valid()) {
        did_misbehave("Got request to allow invalid URL");
        return;
    }

    m_allowlist.empend(String(), false, Vector<URL> { request.url() });
}

void ClientConnection::handle(const Messages::LaunchServer::AddAllowedHandlerWithAnyURL& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (request.handler_name().is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return;
    }

    m_allowlist.empend(request.handler_name(), true, Vector<URL>());
}

void ClientConnection::handle(const Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLs& request)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (request.handler_name().is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return;
    }

    if (request.urls().is_empty()) {
        did_misbehave("Got request to allow empty URL list");
        return;
    }

    m_allowlist.empend(request.handler_name(), false, request.urls());
}

void ClientConnection::handle(const Messages::LaunchServer::SealAllowlist&)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got more than one request to seal the allowed handlers list");
        return;
    }
}

}
