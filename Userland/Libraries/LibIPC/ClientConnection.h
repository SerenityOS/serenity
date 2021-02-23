/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibIPC/Connection.h>

namespace IPC {

template<typename T, class... Args>
NonnullRefPtr<T> new_client_connection(Args&&... args)
{
    return T::construct(forward<Args>(args)...) /* arghs */;
}

template<typename ClientEndpoint, typename ServerEndpoint>
class ClientConnection : public Connection<ServerEndpoint, ClientEndpoint> {
public:
    ClientConnection(ServerEndpoint& endpoint, NonnullRefPtr<Core::LocalSocket> socket, int client_id)
        : IPC::Connection<ServerEndpoint, ClientEndpoint>(endpoint, move(socket))
        , m_client_id(client_id)
    {
        VERIFY(this->socket().is_connected());
        this->socket().on_ready_to_read = [this] { this->drain_messages_from_peer(); };
    }

    virtual ~ClientConnection() override
    {
    }

    void did_misbehave()
    {
        dbgln("{} (id={}) misbehaved, disconnecting.", *this, m_client_id);
        this->shutdown();
    }

    void did_misbehave(const char* message)
    {
        dbgln("{} (id={}) misbehaved ({}), disconnecting.", *this, m_client_id, message);
        this->shutdown();
    }

    int client_id() const { return m_client_id; }

    virtual void die() = 0;

private:
    int m_client_id { -1 };
};

}

template<>
template<typename ClientEndpoint, typename ServerEndpoint>
struct AK::Formatter<IPC::ClientConnection<ClientEndpoint, ServerEndpoint>> : Formatter<Core::Object> {
};
