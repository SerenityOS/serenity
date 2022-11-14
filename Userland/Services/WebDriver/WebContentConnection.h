/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Stream.h>
#include <LibIPC/ConnectionFromClient.h>
#include <WebContent/WebDriverClientEndpoint.h>
#include <WebContent/WebDriverServerEndpoint.h>

namespace WebDriver {

class Client;

class WebContentConnection
    : public IPC::ConnectionFromClient<WebDriverClientEndpoint, WebDriverServerEndpoint> {
    C_OBJECT_ABSTRACT(WebContentConnection)
public:
    WebContentConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<Client> client, unsigned session_id);

    virtual void die() override;

private:
    NonnullRefPtr<Client> m_client;
    unsigned m_session_id { 0 };
};

}
