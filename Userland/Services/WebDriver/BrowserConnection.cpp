/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserConnection.h"
#include "Client.h"

namespace WebDriver {

BrowserConnection::BrowserConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<Client> client, unsigned session_id)
    : IPC::ConnectionFromClient<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint>(*this, move(socket), 1)
    , m_client(move(client))
    , m_session_id(session_id)
{
}

void BrowserConnection::die()
{
    dbgln_if(WEBDRIVER_DEBUG, "Session {} was closed remotely. Shutting down...", m_session_id);
    m_client->close_session(m_session_id);
}

}
