#include "Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/TCPServer.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    Core::EventLoop loop;

    auto server = Core::TCPServer::construct();

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        ASSERT(client_socket);
        auto client = WebServer::Client::construct(client_socket.release_nonnull(), server);
        client->start();
    };

    server->listen({}, 8000);
    return loop.exec();
}
