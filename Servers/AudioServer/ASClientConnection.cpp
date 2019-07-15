#include "ASClientConnection.h"
#include "ASMixer.h"

#include <LibCore/CEventLoop.h>
#include <LibAudio/ASAPI.h>
#include <LibAudio/ABuffer.h>
#include <SharedBuffer.h>

#include <errno.h>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>

ASClientConnection::ASClientConnection(int fd, int client_id, ASMixer& mixer)
    : m_socket(fd)
    , m_notifier(CNotifier(fd, CNotifier::Read))
    , m_client_id(client_id)
    , m_mixer(mixer)
{
    m_notifier.on_ready_to_read = [this] { drain_client(); };
    ASAPI_ServerMessage message;
    message.type = ASAPI_ServerMessage::Type::Greeting;
    message.greeting.server_pid = getpid();
    message.greeting.your_client_id = m_client_id;
    post_message(message);
    dbg() << "********** S: Created new ASClientConnection " << fd << client_id << " and said hello";
}

ASClientConnection::~ASClientConnection()
{
    dbg() << "********** S: Destroyed ASClientConnection " << m_socket.fd() << m_client_id;
}

void ASClientConnection::post_message(const ASAPI_ServerMessage& message, const ByteBuffer& extra_data)
{
    if (!extra_data.is_empty())
        const_cast<ASAPI_ServerMessage&>(message).extra_size = extra_data.size();

    struct iovec iov[2];
    int iov_count = 1;

    iov[0].iov_base = const_cast<ASAPI_ServerMessage*>(&message);
    iov[0].iov_len = sizeof(message);

    if (!extra_data.is_empty()) {
        iov[1].iov_base = const_cast<u8*>(extra_data.data());
        iov[1].iov_len = extra_data.size();
        ++iov_count;
    }

    int nwritten = writev(m_socket.fd(), iov, iov_count);
    if (nwritten < 0) {
        switch (errno) {
        case EPIPE:
            dbgprintf("WSClientConnection::post_message: Disconnected from peer.\n");
            delete_later();
            return;
            break;
        case EAGAIN:
            dbgprintf("WSClientConnection::post_message: Client buffer overflowed.\n");
            did_misbehave();
            return;
            break;
        default:
            perror("WSClientConnection::post_message writev");
            ASSERT_NOT_REACHED();
        }
    }

    ASSERT(nwritten == (int)(sizeof(message) + extra_data.size()));
}

void ASClientConnection::event(CEvent& event)
{
    if (event.type() == ASEvent::WM_ClientDisconnected) {
        int client_id = static_cast<const ASClientDisconnectedNotification&>(event).client_id();
        dbgprintf("ASClientConnection: Client disconnected: %d\n", client_id);
        delete this;
        return;
    }

    CObject::event(event);
}
 
void ASClientConnection::drain_client()
{
    unsigned messages_received = 0;
    for (;;) {
        ASAPI_ClientMessage message;
        // FIXME: Don't go one message at a time, that's so much context switching, oof.
        ssize_t nread = recv(m_socket.fd(), &message, sizeof(ASAPI_ClientMessage), MSG_DONTWAIT);
        if (nread == 0 || (nread == -1 && errno == EAGAIN)) {
            if (!messages_received) {
                // TODO: is delete_later() sufficient?
                CEventLoop::current().post_event(*this, make<ASClientDisconnectedNotification>(m_client_id));
            }
            break;
        }
        if (nread < 0) {
            perror("recv");
            ASSERT_NOT_REACHED();
        }
        ByteBuffer extra_data;
        if (message.extra_size) {
            if (message.extra_size >= 32768) {
                dbgprintf("message.extra_size is way too large\n");
                return did_misbehave();
            }
            extra_data = ByteBuffer::create_uninitialized(message.extra_size);
            // FIXME: We should allow this to time out. Maybe use a socket timeout?
            int extra_nread = read(m_socket.fd(), extra_data.data(), extra_data.size());
            if (extra_nread != (int)message.extra_size) {
                dbgprintf("extra_nread(%d) != extra_size(%d)\n", extra_nread, extra_data.size());
                if (extra_nread < 0)
                    perror("read");
                return did_misbehave();
            }
        }
        if (!handle_message(message, move(extra_data)))
            return;
        ++messages_received;
    }
}

bool ASClientConnection::handle_message(const ASAPI_ClientMessage& message, const ByteBuffer&)
{
    switch (message.type) {
    case ASAPI_ClientMessage::Type::Greeting:
        m_pid = message.greeting.client_pid;
        break;
    case ASAPI_ClientMessage::Type::PlayBuffer: {
        // ### ensure that the size is that of a Vector<ASample>
        Vector<ASample> samples;

        {
            const auto& shared_buf = SharedBuffer::create_from_shared_buffer_id(message.play_buffer.buffer_id);
            if (!shared_buf) {
                did_misbehave();
                return false;
            }

            if (shared_buf->size() / sizeof(ASample) > 441000) {
                did_misbehave();
                return false;
            }
            samples.resize(shared_buf->size() / sizeof(ASample));
            memcpy(samples.data(), shared_buf->data(), shared_buf->size());
        }

        // we no longer need the buffer, so acknowledge that it's playing
        // TODO: rate limit playback here somehow
        ASAPI_ServerMessage reply;
        reply.type = ASAPI_ServerMessage::Type::PlayingBuffer;
        reply.playing_buffer.buffer_id = message.play_buffer.buffer_id;
        post_message(reply);

        m_mixer.queue(*this, adopt(*new ABuffer(samples)));
        break;
    }
    case ASAPI_ClientMessage::Type::Invalid:
    default:
        dbgprintf("ASClientConnection: Unexpected message ID %d\n", int(message.type));
        did_misbehave();
    }

    return true;
}

void ASClientConnection::did_misbehave()
{
    dbgprintf("ASClientConnection{%p} (id=%d, pid=%d) misbehaved, disconnecting.\n", this, m_client_id, m_pid);
    delete_later();
    m_notifier.set_enabled(false);
}
