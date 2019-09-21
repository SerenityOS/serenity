#include <LibCore/CEventLoop.h>
#include <LibCore/CTimer.h>
#include <LibCore/CoreIPCServer.h>
#include <LibCore/CLocalServer.h>
#include <stdio.h>
#include "SimpleEndpoint.h"

class SimpleIPCServer final :
    public IPC::Server::ConnectionNG<SimpleEndpoint>,
    public SimpleEndpoint {

    C_OBJECT(SimpleIPCServer)
public:
    SimpleIPCServer(CLocalSocket& socket, int client_id)
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
    CEventLoop event_loop;

    unlink("/tmp/simple-ipc");
    auto server = CLocalServer::construct();
    server->listen("/tmp/simple-ipc");
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        ASSERT(client_socket);
        static int next_client_id = 0;
        IPC::Server::new_connection_ng_for_client<SimpleIPCServer>(*client_socket, ++next_client_id);
    };

    return event_loop.exec();
}
