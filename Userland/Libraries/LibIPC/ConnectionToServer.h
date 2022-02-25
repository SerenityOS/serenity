/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Stream.h>
#include <LibIPC/Connection.h>

namespace IPC {

#define IPC_CLIENT_CONNECTION(klass, socket_path)                                                      \
    C_OBJECT_ABSTRACT(klass)                                                                           \
public:                                                                                                \
    template<typename Klass = klass, class... Args>                                                    \
    static ErrorOr<NonnullRefPtr<klass>> try_create(Args&&... args)                                    \
    {                                                                                                  \
        auto socket = TRY(Core::Stream::LocalSocket::connect(socket_path));                            \
        /* We want to rate-limit our clients */                                                        \
        TRY(socket->set_blocking(true));                                                               \
                                                                                                       \
        return adopt_nonnull_ref_or_enomem(new (nothrow) Klass(move(socket), forward<Args>(args)...)); \
    }

template<typename ClientEndpoint, typename ServerEndpoint>
class ConnectionToServer : public IPC::Connection<ClientEndpoint, ServerEndpoint>
    , public ClientEndpoint::Stub
    , public ServerEndpoint::template Proxy<ClientEndpoint> {
public:
    using ClientStub = typename ClientEndpoint::Stub;
    using IPCProxy = typename ServerEndpoint::template Proxy<ClientEndpoint>;

    ConnectionToServer(ClientStub& local_endpoint, NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : Connection<ClientEndpoint, ServerEndpoint>(local_endpoint, move(socket))
        , ServerEndpoint::template Proxy<ClientEndpoint>(*this, {})
    {
    }

    virtual void die() override
    {
        // Override this function if you don't want your app to exit if it loses the connection.
        exit(0);
    }
};

}
