/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/Connection.h>

namespace IPC {

template<typename ClientEndpoint, typename ServerEndpoint>
class ServerConnection : public IPC::Connection<ClientEndpoint, ServerEndpoint>, public ClientEndpoint::Stub {
public:
    using ClientStub = typename ClientEndpoint::Stub;
    using ServerProxy = typename ServerEndpoint::Proxy;

    ServerConnection(ClientStub& local_endpoint, const StringView& address)
        : Connection<ClientEndpoint, ServerEndpoint>(local_endpoint, Core::LocalSocket::construct())
    {
        // We want to rate-limit our clients
        this->socket().set_blocking(true);

        if (!this->socket().connect(Core::SocketAddress::local(address))) {
            perror("connect");
            VERIFY_NOT_REACHED();
        }

        VERIFY(this->socket().is_connected());
    }

    virtual void handshake() = 0;

    virtual void die() override
    {
        // Override this function if you don't want your app to exit if it loses the connection.
        exit(0);
    }
};

}
