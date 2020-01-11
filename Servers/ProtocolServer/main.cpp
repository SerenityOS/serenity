#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalServer.h>
#include <LibIPC/IClientConnection.h>
#include <ProtocolServer/HttpProtocol.h>
#include <ProtocolServer/PSClientConnection.h>

int main(int, char**)
{
    if (pledge("stdio inet shared_buffer unix rpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    CEventLoop event_loop;
    if (pledge("stdio inet shared_buffer unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    (void)*new HttpProtocol;
    auto server = CLocalServer::construct();
    bool ok = server->take_over_from_system_server();
    ASSERT(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbg() << "ProtocolServer: accept failed.";
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        new_client_connection<PSClientConnection>(*client_socket, client_id);
    };
    return event_loop.exec();
}
