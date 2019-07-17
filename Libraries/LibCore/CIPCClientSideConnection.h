#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibAudio/ASAPI.h>

#include <unistd.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>

template <typename ServerMessage, typename ClientMessage>
class CIPCClientSideConnection {
public:
    CIPCClientSideConnection()
        : m_notifier(CNotifier(m_connection.fd(), CNotifier::Read))
    {
        // We want to rate-limit our clients
        m_connection.set_blocking(true);
        m_notifier.on_ready_to_read = [this] {
            drain_messages_from_server();
        };
        m_connection.on_connected = [this] {
            dbg() << "IPC: Connected, sending greeting";
            send_greeting();
            dbg() << "IPC: Greeting sent!";
        };

        int retries = 1000;
        while (retries) {
            if (m_connection.connect(CSocketAddress::local("/tmp/asportal"))) {
                break;
            }

            dbgprintf("CIPCClientSideConnection: connect failed: %d, %s\n", errno, strerror(errno));
            sleep(1);
            --retries;
        }
    }

    virtual void send_greeting() = 0;
    void set_server_pid(pid_t pid) { m_server_pid = pid; }
    pid_t server_pid() const { return m_server_pid; }
    void set_my_client_id(int id) { m_my_client_id = id; }
    int my_client_id() const { return m_my_client_id; }

protected:
    template <typename MessageType>
    bool wait_for_specific_event(MessageType type, ServerMessage& event)
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

    bool post_message_to_server(const ClientMessage& message, const ByteBuffer&& extra_data = {})
    {
        if (!extra_data.is_empty())
            const_cast<ClientMessage&>(message).extra_size = extra_data.size();

        struct iovec iov[2];
        int iov_count = 1;
        iov[0].iov_base = const_cast<ClientMessage*>(&message);
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

    template <typename MessageType>
    ServerMessage sync_request(const ClientMessage& request, MessageType response_type)
    {
        bool success = post_message_to_server(request);
        ASSERT(success);

        ServerMessage response;
        success = wait_for_specific_event(response_type, response);
        ASSERT(success);
        return response;
    }

private:
    bool drain_messages_from_server()
    {
        for (;;) {
            ServerMessage message;
            ssize_t nread = recv(m_connection.fd(), &message, sizeof(ServerMessage), MSG_DONTWAIT);
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

    CLocalSocket m_connection;
    CNotifier m_notifier;

    struct IncomingASMessageBundle {
        ServerMessage message;
        ByteBuffer extra_data;
    };
    Vector<IncomingASMessageBundle> m_unprocessed_bundles;
    int m_server_pid;
    int m_my_client_id;
};
