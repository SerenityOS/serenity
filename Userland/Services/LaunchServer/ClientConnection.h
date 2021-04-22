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

class ClientConnection final : public IPC::ClientConnection<LaunchClientEndpoint, LaunchServerEndpoint>
    , public LaunchServerEndpoint {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual OwnPtr<Messages::LaunchServer::GreetResponse> handle(const Messages::LaunchServer::Greet&) override;
    virtual OwnPtr<Messages::LaunchServer::OpenURLResponse> handle(const Messages::LaunchServer::OpenURL&) override;
    virtual OwnPtr<Messages::LaunchServer::GetHandlersForURLResponse> handle(const Messages::LaunchServer::GetHandlersForURL&) override;
    virtual OwnPtr<Messages::LaunchServer::GetHandlersWithDetailsForURLResponse> handle(const Messages::LaunchServer::GetHandlersWithDetailsForURL&) override;
    virtual OwnPtr<Messages::LaunchServer::AddAllowedURLResponse> handle(const Messages::LaunchServer::AddAllowedURL&) override;
    virtual OwnPtr<Messages::LaunchServer::AddAllowedHandlerWithAnyURLResponse> handle(const Messages::LaunchServer::AddAllowedHandlerWithAnyURL&) override;
    virtual OwnPtr<Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLsResponse> handle(const Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLs&) override;
    virtual OwnPtr<Messages::LaunchServer::SealAllowlistResponse> handle(const Messages::LaunchServer::SealAllowlist&) override;

    struct AllowlistEntry {
        String handler_name;
        bool any_url { false };
        Vector<URL> urls;
    };

    Vector<AllowlistEntry> m_allowlist;
    bool m_allowlist_is_sealed { false };
};
}
