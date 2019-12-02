#include "ASEventLoop.h"
#include "ASClientConnection.h"
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

ASEventLoop::ASEventLoop()
    : m_server(CLocalServer::construct())
{
    bool ok = m_server->take_over_from_system_server();
    ASSERT(ok);
    m_server->on_ready_to_accept = [this] {
        auto client_socket = m_server->accept();
        if (!client_socket) {
            dbg() << "AudioServer: accept failed.";
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        new_client_connection<ASClientConnection>(*client_socket, client_id, m_mixer);
    };
}
