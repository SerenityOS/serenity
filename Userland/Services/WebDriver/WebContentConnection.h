/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionFromClient.h>
#include <WebContent/WebDriverClientEndpoint.h>
#include <WebContent/WebDriverServerEndpoint.h>

namespace WebDriver {

class Client;

class WebContentConnection
    : public IPC::ConnectionFromClient<WebDriverClientEndpoint, WebDriverServerEndpoint> {
    C_OBJECT_ABSTRACT(WebContentConnection)
public:
    WebContentConnection(NonnullOwnPtr<Core::LocalSocket> socket);

    Function<void()> on_close;

    virtual void die() override;
};

}
