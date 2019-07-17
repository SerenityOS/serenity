#include "AClientConnection.h"
#include "ABuffer.h"
#include <SharedBuffer.h>
#include <LibCore/CEventLoop.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>

AClientConnection::AClientConnection()
    : m_notifier(CNotifier(m_connection.fd(), CNotifier::Read))
{
    // We want to rate-limit our clients
    m_connection.set_blocking(true);
    m_notifier.on_ready_to_read = [this] {
        drain_messages_from_server();
    };
    m_connection.on_connected = [this] {
        ASAPI_ClientMessage request;
        request.type = ASAPI_ClientMessage::Type::Greeting;
        request.greeting.client_pid = getpid();
        auto response = sync_request(request, ASAPI_ServerMessage::Type::Greeting);
        m_server_pid = response.greeting.server_pid;
        m_my_client_id = response.greeting.your_client_id;
        dbg() << "**** C: Got greeting from AudioServer: client ID " << m_my_client_id << " PID " << m_server_pid;
    };

    int retries = 1000;
    while (retries) {
        if (m_connection.connect(CSocketAddress::local("/tmp/asportal"))) {
            break;
        }

#ifdef ACLIENT_DEBUG
        dbgprintf("**** C: AClientConnection: connect failed: %d, %s\n", errno, strerror(errno));
#endif
        sleep(1);
        --retries;
    }
}

bool AClientConnection::drain_messages_from_server()
{
    for (;;) {
        ASAPI_ServerMessage message;
        ssize_t nread = recv(m_connection.fd(), &message, sizeof(ASAPI_ServerMessage), MSG_DONTWAIT);
        if (nread < 0) {
            if (errno == EAGAIN) {
                return true;
            }
            perror("read");
            exit(1);
            return false;
        }
        if (nread == 0) {
            dbgprintf("EOF on IPC fd\n");
            exit(1);
            return false;
        }
        ASSERT(nread == sizeof(message));
        ByteBuffer extra_data;
        if (message.extra_size) {
            extra_data = ByteBuffer::create_uninitialized(message.extra_size);
            int extra_nread = read(m_connection.fd(), extra_data.data(), extra_data.size());
            if (extra_nread < 0) {
                perror("read");
                ASSERT_NOT_REACHED();
            }
            ASSERT((size_t)extra_nread == message.extra_size);
        }
        m_unprocessed_bundles.append({ move(message), move(extra_data) });
    }
}

bool AClientConnection::wait_for_specific_event(ASAPI_ServerMessage::Type type, ASAPI_ServerMessage& event)
{
    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(m_connection.fd(), &rfds);
        int rc = select(m_connection.fd() + 1, &rfds, nullptr, nullptr, nullptr);
        if (rc < 0) {
            perror("select");
        }
        ASSERT(rc > 0);
        ASSERT(FD_ISSET(m_connection.fd(), &rfds));
        bool success = drain_messages_from_server();
        if (!success)
            return false;
        for (ssize_t i = 0; i < m_unprocessed_bundles.size(); ++i) {
            if (m_unprocessed_bundles[i].message.type == type) {
                event = move(m_unprocessed_bundles[i].message);
                m_unprocessed_bundles.remove(i);
                return true;
            }
        }
    }
}

bool AClientConnection::post_message_to_server(const ASAPI_ClientMessage& message, const ByteBuffer& extra_data)
{
    if (!extra_data.is_empty())
        const_cast<ASAPI_ClientMessage&>(message).extra_size = extra_data.size();

    struct iovec iov[2];
    int iov_count = 1;
    iov[0].iov_base = const_cast<ASAPI_ClientMessage*>(&message);
    iov[0].iov_len = sizeof(message);

    if (!extra_data.is_empty()) {
        iov[1].iov_base = const_cast<u8*>(extra_data.data());
        iov[1].iov_len = extra_data.size();
        ++iov_count;
    }

    int nwritten = writev(m_connection.fd(), iov, iov_count);
    if (nwritten < 0) {
        perror("writev");
        ASSERT_NOT_REACHED();
    }
    ASSERT((size_t)nwritten == sizeof(message) + extra_data.size());

    return true;
}

ASAPI_ServerMessage AClientConnection::sync_request(const ASAPI_ClientMessage& request, ASAPI_ServerMessage::Type response_type)
{
    bool success = post_message_to_server(request);
    ASSERT(success);

    ASAPI_ServerMessage response;
    success = wait_for_specific_event(response_type, response);
    ASSERT(success);
    return response;
}

void AClientConnection::play(const ABuffer& buffer)
{
    auto shared_buf = SharedBuffer::create(m_server_pid, buffer.size_in_bytes());
    if (!shared_buf) {
        dbg() << "Failed to create a shared buffer!";
        return;
    }

    memcpy(shared_buf->data(), buffer.data(), buffer.size_in_bytes());
    shared_buf->seal();
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::PlayBuffer;
    request.play_buffer.buffer_id = shared_buf->shared_buffer_id();
    sync_request(request, ASAPI_ServerMessage::Type::PlayingBuffer);
}
