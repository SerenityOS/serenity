#include "ASEventLoop.h"
#include "ASClientConnection.h"
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

ASEventLoop::ASEventLoop()
{
    unlink("/tmp/asportal");
    m_server_sock.listen("/tmp/asportal");
    m_server_sock.on_ready_to_accept = [this] {
        auto* client_socket = m_server_sock.accept();
        if (!client_socket) {
            dbg() << "AudioServer: accept failed.";
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::Server::new_connection_for_client<ASClientConnection>(*client_socket, client_id, m_mixer);
    };
}
