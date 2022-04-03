/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Applications/Browser/WebDriverSessionClientEndpoint.h>
#include <Applications/Browser/WebDriverSessionServerEndpoint.h>
#include <LibGUI/Application.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibIPC/Encoder.h>

namespace WebDriver {

class ConnectionFromClient
    : public IPC::ConnectionFromClient<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint> {
    C_OBJECT_ABSTRACT(ConnectionFromClient)
public:
    ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionFromClient<WebDriverSessionClientEndpoint, WebDriverSessionServerEndpoint>(*this, move(socket), 1)
    {
    }

    virtual void die() override { }
};
}
