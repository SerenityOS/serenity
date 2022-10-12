/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <Applications/Browser/WebDriverSessionClientEndpoint.h>
#include <Applications/Browser/WebDriverSessionServerEndpoint.h>
#include <LibGUI/Application.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibIPC/Encoder.h>

namespace WebDriver {

class Client;

class BrowserConnection
    : public IPC::ConnectionFromClient<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint> {
    C_OBJECT_ABSTRACT(BrowserConnection)
public:
    BrowserConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, NonnullRefPtr<Client> client, unsigned session_id);

    virtual void die() override;

private:
    NonnullRefPtr<Client> m_client;
    unsigned m_session_id { 0 };
};

}
