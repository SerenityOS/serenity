#include "Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/TCPServer.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    if (pledge("stdio accept rpath inet unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop loop;

    auto server = Core::TCPServer::construct();

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        ASSERT(client_socket);
        auto client = WebServer::Client::construct(client_socket.release_nonnull(), server);
        client->start();
    };

    server->listen({}, 8000);

    if (unveil("/www", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (pledge("stdio accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return loop.exec();
}
