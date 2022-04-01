/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/Connection.h>
#include <LibIPC/Stub.h>

namespace IPC {

template<typename T, class... Args>
NonnullRefPtr<T> new_client_connection(Args&&... args)
{
    return T::construct(forward<Args>(args)...) /* arghs */;
}

template<typename ClientEndpoint, typename ServerEndpoint>
class ConnectionFromClient : public Connection<ServerEndpoint, ClientEndpoint>
    , public ServerEndpoint::Stub
    , public ClientEndpoint::template Proxy<ServerEndpoint> {
public:
    using ServerStub = typename ServerEndpoint::Stub;
    using IPCProxy = typename ClientEndpoint::template Proxy<ServerEndpoint>;

    ConnectionFromClient(ServerStub& stub, NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
        : IPC::Connection<ServerEndpoint, ClientEndpoint>(stub, move(socket))
        , ClientEndpoint::template Proxy<ServerEndpoint>(*this, {})
        , m_client_id(client_id)
    {
        VERIFY(this->socket().is_open());
        this->socket().on_ready_to_read = [this] {
            // FIXME: Do something about errors.
            (void)this->drain_messages_from_peer();
        };
    }

    virtual ~ConnectionFromClient() override = default;

    void did_misbehave()
    {
        dbgln("{} (id={}) misbehaved, disconnecting.", *this, m_client_id);
        this->shutdown();
    }

    void did_misbehave(char const* message)
    {
        dbgln("{} (id={}) misbehaved ({}), disconnecting.", *this, m_client_id, message);
        this->shutdown();
    }

    void shutdown_with_error(Error const& error)
    {
        dbgln("{} (id={}) had error ({}), disconnecting.", *this, m_client_id, error);
        this->shutdown();
    }

    int client_id() const { return m_client_id; }

    virtual void die() override = 0;

private:
    int m_client_id { -1 };
};

}

template<typename ClientEndpoint, typename ServerEndpoint>
struct AK::Formatter<IPC::ConnectionFromClient<ClientEndpoint, ServerEndpoint>> : Formatter<Core::Object> {
};
