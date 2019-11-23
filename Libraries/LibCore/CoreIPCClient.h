#pragma once

#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CSyscallUtils.h>
#include <LibIPC/IMessage.h>
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

//#define CIPC_DEBUG

namespace IPC {
namespace Client {

    class Event : public CEvent {
    public:
        enum Type {
            Invalid = 2000,
            PostProcess,
        };
        Event() {}
        explicit Event(Type type)
            : CEvent(type)
        {
        }
    };

    class PostProcessEvent : public Event {
    public:
        explicit PostProcessEvent(int client_id)
            : Event(PostProcess)
            , m_client_id(client_id)
        {
        }

        int client_id() const { return m_client_id; }

    private:
        int m_client_id { 0 };
    };

    template<typename ServerMessage, typename ClientMessage>
    class Connection : public CObject {
    public:
        Connection(const StringView& address)
            : m_connection(CLocalSocket::construct(this))
            , m_notifier(CNotifier::construct(m_connection->fd(), CNotifier::Read, this))
        {
            // We want to rate-limit our clients
            m_connection->set_blocking(true);
            m_notifier->on_ready_to_read = [this] {
                drain_messages_from_server();
                CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
            };

            int retries = 100000;
            while (retries) {
                if (m_connection->connect(CSocketAddress::local(address))) {
                    break;
                }

                dbgprintf("Client::Connection: connect failed: %d, %s\n", errno, strerror(errno));
                usleep(10000);
                --retries;
            }
            ASSERT(m_connection->is_connected());
        }

        virtual void handshake() = 0;

        virtual void event(CEvent& event) override
        {
            if (event.type() == Event::PostProcess) {
                postprocess_bundles(m_unprocessed_bundles);
            } else {
                CObject::event(event);
            }
        }

        void set_server_pid(pid_t pid) { m_server_pid = pid; }
        pid_t server_pid() const { return m_server_pid; }
        void set_my_client_id(int id) { m_my_client_id = id; }
        int my_client_id() const { return m_my_client_id; }

        template<typename MessageType>
        bool wait_for_specific_event(MessageType type, ServerMessage& event)
        {
            // Double check we don't already have the event waiting for us.
            // Otherwise we might end up blocked for a while for no reason.
            for (ssize_t i = 0; i < m_unprocessed_bundles.size(); ++i) {
                if (m_unprocessed_bundles[i].message.type == type) {
                    event = move(m_unprocessed_bundles[i].message);
                    m_unprocessed_bundles.remove(i);
                    CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
                    return true;
                }
            }
            for (;;) {
                fd_set rfds;
                FD_ZERO(&rfds);
                FD_SET(m_connection->fd(), &rfds);
                int rc = CSyscallUtils::safe_syscall(select, m_connection->fd() + 1, &rfds, nullptr, nullptr, nullptr);
                if (rc < 0) {
                    perror("select");
                }
                ASSERT(rc > 0);
                ASSERT(FD_ISSET(m_connection->fd(), &rfds));
                bool success = drain_messages_from_server();
                if (!success)
                    return false;
                for (ssize_t i = 0; i < m_unprocessed_bundles.size(); ++i) {
                    if (m_unprocessed_bundles[i].message.type == type) {
                        event = move(m_unprocessed_bundles[i].message);
                        m_unprocessed_bundles.remove(i);
                        CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
                        return true;
                    }
                }
            }
        }

        bool post_message_to_server(const ClientMessage& message, const ByteBuffer&& extra_data = {})
        {
#if defined(CIPC_DEBUG)
            dbg() << "C: -> S " << int(message.type) << " extra " << extra_data.size();
#endif
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

            int nwritten;

            for (;;) {
                nwritten = writev(m_connection->fd(), iov, iov_count);
                if (nwritten < 0) {
                    if (errno == EAGAIN) {
                        sched_yield();
                        continue;
                    }
                    perror("writev");
                    ASSERT_NOT_REACHED();
                }
                break;
            }
            ASSERT((size_t)nwritten == sizeof(message) + extra_data.size());

            return true;
        }

        template<typename MessageType>
        ServerMessage sync_request(const ClientMessage& request, MessageType response_type)
        {
            bool success = post_message_to_server(request);
            ASSERT(success);

            ServerMessage response;
            success = wait_for_specific_event(response_type, response);
            ASSERT(success);
            return response;
        }

        template<typename RequestType, typename... Args>
        typename RequestType::ResponseType send_sync(Args&&... args)
        {
            bool success = post_message_to_server(RequestType(forward<Args>(args)...));
            ASSERT(success);

            ServerMessage response;
            success = wait_for_specific_event(RequestType::ResponseType::message_type(), response);
            ASSERT(success);
            return response;
        }

    protected:
        struct IncomingMessageBundle {
            ServerMessage message;
            ByteBuffer extra_data;
        };

        virtual void postprocess_bundles(Vector<IncomingMessageBundle>& new_bundles)
        {
            dbg() << "Client::Connection: "
                  << " warning: discarding " << new_bundles.size() << " unprocessed bundles; this may not be what you want";
            new_bundles.clear();
        }

    private:
        bool drain_messages_from_server()
        {
            for (;;) {
                ServerMessage message;
                ssize_t nread = recv(m_connection->fd(), &message, sizeof(ServerMessage), MSG_DONTWAIT);
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
                    int extra_nread = read(m_connection->fd(), extra_data.data(), extra_data.size());
                    if (extra_nread < 0) {
                        perror("read");
                        ASSERT_NOT_REACHED();
                    }
                    ASSERT((size_t)extra_nread == message.extra_size);
                }
#if defined(CIPC_DEBUG)
                dbg() << "C: <- S " << int(message.type) << " extra " << extra_data.size();
#endif
                m_unprocessed_bundles.append({ move(message), move(extra_data) });
            }
        }

        RefPtr<CLocalSocket> m_connection;
        RefPtr<CNotifier> m_notifier;
        Vector<IncomingMessageBundle> m_unprocessed_bundles;
        int m_server_pid { -1 };
        int m_my_client_id { -1 };
    };

    template<typename LocalEndpoint, typename PeerEndpoint>
    class ConnectionNG : public CObject {
    public:
        ConnectionNG(LocalEndpoint& local_endpoint, const StringView& address)
            : m_local_endpoint(local_endpoint)
            , m_connection(CLocalSocket::construct(this))
            , m_notifier(CNotifier::construct(m_connection->fd(), CNotifier::Read, this))
        {
            // We want to rate-limit our clients
            m_connection->set_blocking(true);
            m_notifier->on_ready_to_read = [this] {
                drain_messages_from_server();
                CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
            };

            int retries = 100000;
            while (retries) {
                if (m_connection->connect(CSocketAddress::local(address))) {
                    break;
                }

                dbgprintf("Client::Connection: connect failed: %d, %s\n", errno, strerror(errno));
                usleep(10000);
                --retries;
            }
            ASSERT(m_connection->is_connected());
        }

        virtual void handshake() = 0;

        virtual void event(CEvent& event) override
        {
            if (event.type() == Event::PostProcess) {
                postprocess_messages(m_unprocessed_messages);
            } else {
                CObject::event(event);
            }
        }

        void set_server_pid(pid_t pid) { m_server_pid = pid; }
        pid_t server_pid() const { return m_server_pid; }
        void set_my_client_id(int id) { m_my_client_id = id; }
        int my_client_id() const { return m_my_client_id; }

        template<typename MessageType>
        OwnPtr<MessageType> wait_for_specific_message()
        {
            // Double check we don't already have the event waiting for us.
            // Otherwise we might end up blocked for a while for no reason.
            for (ssize_t i = 0; i < m_unprocessed_messages.size(); ++i) {
                if (m_unprocessed_messages[i]->id() == MessageType::static_message_id()) {
                    auto message = move(m_unprocessed_messages[i]);
                    m_unprocessed_messages.remove(i);
                    CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
                    return message;
                }
            }
            for (;;) {
                fd_set rfds;
                FD_ZERO(&rfds);
                FD_SET(m_connection->fd(), &rfds);
                int rc = CSyscallUtils::safe_syscall(select, m_connection->fd() + 1, &rfds, nullptr, nullptr, nullptr);
                if (rc < 0) {
                    perror("select");
                }
                ASSERT(rc > 0);
                ASSERT(FD_ISSET(m_connection->fd(), &rfds));
                if (!drain_messages_from_server())
                    return nullptr;
                for (ssize_t i = 0; i < m_unprocessed_messages.size(); ++i) {
                    if (m_unprocessed_messages[i]->id() == MessageType::static_message_id()) {
                        auto message = move(m_unprocessed_messages[i]);
                        m_unprocessed_messages.remove(i);
                        CEventLoop::current().post_event(*this, make<PostProcessEvent>(m_connection->fd()));
                        return message;
                    }
                }
            }
        }

        bool post_message_to_server(const IMessage& message)
        {
            auto buffer = message.encode();
            int nwritten = write(m_connection->fd(), buffer.data(), (size_t)buffer.size());
            if (nwritten < 0) {
                perror("write");
                ASSERT_NOT_REACHED();
                return false;
            }
            ASSERT(nwritten == buffer.size());
            return true;
        }

        template<typename RequestType, typename... Args>
        OwnPtr<typename RequestType::ResponseType> send_sync(Args&&... args)
        {
            bool success = post_message_to_server(RequestType(forward<Args>(args)...));
            ASSERT(success);
            auto response = wait_for_specific_message<typename RequestType::ResponseType>();
            ASSERT(response);
            return response;
        }

    protected:
        virtual void postprocess_messages(Vector<OwnPtr<IMessage>>& new_bundles)
        {
            new_bundles.clear();
        }

    private:
        bool drain_messages_from_server()
        {
            Vector<u8> bytes;
            for (;;) {
                u8 buffer[4096];
                ssize_t nread = recv(m_connection->fd(), buffer, sizeof(buffer), MSG_DONTWAIT);
                if (nread < 0) {
                    if (errno == EAGAIN)
                        break;
                    perror("read");
                    exit(1);
                    return false;
                }
                if (nread == 0) {
                    dbg() << "EOF on IPC fd";
                    // FIXME: Dying is definitely not always appropriate!
                    exit(1);
                    return false;
                }
                bytes.append(buffer, nread);
            }

            size_t decoded_bytes = 0;
            for (size_t index = 0; index < (size_t)bytes.size(); index += decoded_bytes) {
                auto remaining_bytes = ByteBuffer::wrap(bytes.data() + index, bytes.size() - index);
                if (auto message = LocalEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                    m_local_endpoint.handle(*message);
                } else if (auto message = PeerEndpoint::decode_message(remaining_bytes, decoded_bytes)) {
                    m_unprocessed_messages.append(move(message));
                } else {
                    ASSERT_NOT_REACHED();
                }
                ASSERT(decoded_bytes);
            }
            return true;
        }

        LocalEndpoint& m_local_endpoint;
        RefPtr<CLocalSocket> m_connection;
        RefPtr<CNotifier> m_notifier;
        Vector<OwnPtr<IMessage>> m_unprocessed_messages;
        int m_server_pid { -1 };
        int m_my_client_id { -1 };
    };

} // Client
} // IPC
