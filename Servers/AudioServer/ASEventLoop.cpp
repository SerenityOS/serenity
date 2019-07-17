#include "ASEventLoop.h"
#include "ASClientConnection.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>

ASEventLoop::ASEventLoop()
{
    unlink("/tmp/asportal");

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/asportal");
    int rc = bind(m_server_sock.fd(), (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = listen(m_server_sock.fd(), 5);
    ASSERT(rc == 0);

    m_server_notifier = make<CNotifier>(m_server_sock.fd(), CNotifier::Read);
    m_server_notifier->on_ready_to_read = [this] { drain_server(); };
}

void ASEventLoop::drain_server()
{
    sockaddr_un address;
    socklen_t address_size = sizeof(address);
    int client_fd = accept(m_server_sock.fd(), (sockaddr*)&address, &address_size);
    if (client_fd < 0) {
        dbgprintf("AudioServer: accept() failed: %s\n", strerror(errno));
    } else {
        dbgprintf("AudioServer: accept()ed client %d\n", client_fd);
        static int s_next_client_id = 0;
        IPC::Server::new_connection_for_client<ASClientConnection>(client_fd, s_next_client_id++, m_mixer);
    }
}

