#include "AClientConnection.h"
#include <unistd.h>
#include <stdio.h>

AClientConnection::AClientConnection()
{
    m_connection.on_connected = [this] {
        m_notifier = make<CNotifier>(m_connection.fd(), CNotifier::Read);
        m_notifier->on_ready_to_read = [this] { printf("AudioServer said something to us"); };
        m_connection.write("Hello, friends");
    };

    int retries = 1000;
    while (retries) {
        if (m_connection.connect(CSocketAddress::local("/tmp/asportal"))) {
            break;
        }

#ifdef ACLIENT_DEBUG
        dbgprintf("AClientConnection: connect failed: %d, %s\n", errno, strerror(errno));
#endif
        sleep(1);
        --retries;
    }
}

