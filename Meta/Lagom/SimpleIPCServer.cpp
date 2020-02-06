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

#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibCore/oreIPCServer.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include "SimpleEndpoint.h"

class SimpleIPCServer final :
    public IPC::Server::ConnectionNG<SimpleEndpoint>,
    public SimpleEndpoint {

    C_OBJECT(SimpleIPCServer)
public:
    SimpleIPCServer(Core::LocalSocket& socket, int client_id)
        : ConnectionNG(*this, socket, client_id)
    {
    }

    virtual OwnPtr<Simple::ComputeSumResponse> handle(const Simple::ComputeSum& message)
    {
        return make<Simple::ComputeSumResponse>(message.a() + message.b() + message.c());
    }
};

int main(int, char**)
{
    Core::EventLoop event_loop;

    unlink("/tmp/simple-ipc");
    auto server = Core::LocalServer::construct();
    server->listen("/tmp/simple-ipc");
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        ASSERT(client_socket);
        static int next_client_id = 0;
        IPC::Server::new_connection_ng_for_client<SimpleIPCServer>(*client_socket, ++next_client_id);
    };

    return event_loop.exec();
}
