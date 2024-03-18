/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "Launcher.h"
#include <AK/HashMap.h>
#include <LaunchServer/LaunchClientEndpoint.h>
#include <LibURL/URL.h>

namespace LaunchServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;
ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ConnectionFromClient<LaunchClientEndpoint, LaunchServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

Messages::LaunchServer::OpenUrlResponse ConnectionFromClient::open_url(URL::URL const& url, ByteString const& handler_name)
{
    if (!m_allowlist.is_empty()) {
        bool allowed = false;
        auto request_url_without_fragment = url;
        request_url_without_fragment.set_fragment({});
        for (auto& allowed_handler : m_allowlist) {
            if (allowed_handler.handler_name == handler_name
                && (allowed_handler.any_url || allowed_handler.urls.contains_slow(request_url_without_fragment))) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            // You are not on the list, go home!
            did_misbehave(ByteString::formatted("Client requested a combination of handler/URL that was not on the list: '{}' with '{}'", handler_name, url).characters());
            return nullptr;
        }
    }

    return Launcher::the().open_url(url, handler_name);
}

Messages::LaunchServer::GetHandlersForUrlResponse ConnectionFromClient::get_handlers_for_url(URL::URL const& url)
{
    return Launcher::the().handlers_for_url(url);
}

Messages::LaunchServer::GetHandlersWithDetailsForUrlResponse ConnectionFromClient::get_handlers_with_details_for_url(URL::URL const& url)
{
    return Launcher::the().handlers_with_details_for_url(url);
}

void ConnectionFromClient::add_allowed_url(URL::URL const& url)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (!url.is_valid()) {
        did_misbehave("Got request to allow invalid URL");
        return;
    }

    m_allowlist.empend(ByteString(), false, Vector<URL::URL> { url });
}

void ConnectionFromClient::add_allowed_handler_with_any_url(ByteString const& handler_name)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (handler_name.is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return;
    }

    m_allowlist.empend(handler_name, true, Vector<URL::URL>());
}

void ConnectionFromClient::add_allowed_handler_with_only_specific_urls(ByteString const& handler_name, Vector<URL::URL> const& urls)
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got request to add more allowed handlers after list was sealed");
        return;
    }

    if (handler_name.is_empty()) {
        did_misbehave("Got request to allow empty handler name");
        return;
    }

    if (urls.is_empty()) {
        did_misbehave("Got request to allow empty URL list");
        return;
    }

    m_allowlist.empend(handler_name, false, urls);
}

void ConnectionFromClient::seal_allowlist()
{
    if (m_allowlist_is_sealed) {
        did_misbehave("Got more than one request to seal the allowed handlers list");
        return;
    }

    m_allowlist_is_sealed = true;
}

}
