/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WebDriver/Client.h>
#include <WebDriver/WebContentConnection.h>

namespace WebDriver {

WebContentConnection::WebContentConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<Client> client, unsigned session_id)
    : IPC::ConnectionFromClient<WebDriverClientEndpoint, WebDriverServerEndpoint>(*this, move(socket), 1)
    , m_client(move(client))
    , m_session_id(session_id)
{
}

void WebContentConnection::die()
{
    dbgln_if(WEBDRIVER_DEBUG, "Session {} was closed remotely. Shutting down...", m_session_id);
    m_client->close_session(m_session_id);
}

}
