/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LaunchServer/LaunchClientEndpoint.h>
#include <LaunchServer/LaunchServerEndpoint.h>
#include <LibIPC/ClientConnection.h>

namespace LaunchServer {

class ClientConnection final : public IPC::ClientConnection<LaunchClientEndpoint, LaunchServerEndpoint> {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual Messages::LaunchServer::OpenUrlResponse open_url(URL const&, String const&) override;
    virtual Messages::LaunchServer::GetHandlersForUrlResponse get_handlers_for_url(URL const&) override;
    virtual Messages::LaunchServer::GetHandlersWithDetailsForUrlResponse get_handlers_with_details_for_url(URL const&) override;
    virtual void add_allowed_url(URL const&) override;
    virtual void add_allowed_handler_with_any_url(String const&) override;
    virtual void add_allowed_handler_with_only_specific_urls(String const&, Vector<URL> const&) override;
    virtual void seal_allowlist() override;

    struct AllowlistEntry {
        String handler_name;
        bool any_url { false };
        Vector<URL> urls;
    };

    Vector<AllowlistEntry> m_allowlist;
    bool m_allowlist_is_sealed { false };
};
}
