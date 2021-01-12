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

template<typename ClientEndpoint, typename ServerEndpoint>
class ServerConnection : public IPC::Connection<ClientEndpoint, ServerEndpoint> {
public:
    ServerConnection(ClientEndpoint& local_endpoint, const StringView& address)
        : Connection<ClientEndpoint, ServerEndpoint>(local_endpoint, Core::LocalSocket::construct())
    {
        // We want to rate-limit our clients
        this->socket().set_blocking(true);

        if (!this->socket().connect(Core::SocketAddress::local(address))) {
            perror("connect");
            ASSERT_NOT_REACHED();
        }

        ASSERT(this->socket().is_connected());

        this->initialize_peer_info();
    }

    virtual void handshake() = 0;

    pid_t server_pid() const { return this->peer_pid(); }
    void set_server_pid(pid_t pid) { this->set_peer_pid(pid); }

    void set_my_client_id(int id) { m_my_client_id = id; }
    int my_client_id() const { return m_my_client_id; }

    virtual void die() override
    {
        // Override this function if you don't want your app to exit if it loses the connection.
        exit(0);
    }

private:
    int m_my_client_id { -1 };
};

}
