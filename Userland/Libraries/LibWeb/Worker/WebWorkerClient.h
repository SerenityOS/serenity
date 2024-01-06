/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibWeb/Worker/WebWorkerClientEndpoint.h>
#include <LibWeb/Worker/WebWorkerServerEndpoint.h>
#include <LibWebView/SocketPair.h>

namespace Web::HTML {

class WebWorkerClient final
    : public IPC::ConnectionToServer<WebWorkerClientEndpoint, WebWorkerServerEndpoint>
    , public WebWorkerClientEndpoint {
    IPC_CLIENT_CONNECTION(WebWorkerClient, "/tmp/session/%sid/portal/webworker"sv);

public:
    explicit WebWorkerClient(NonnullOwnPtr<Core::LocalSocket>);

    WebView::SocketPair dup_sockets();

private:
    virtual void die() override;
};

}
