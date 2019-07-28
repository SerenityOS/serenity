#include "ASClientConnection.h"
#include "ASMixer.h"

#include <LibAudio/ABuffer.h>
#include <LibAudio/ASAPI.h>
#include <LibCore/CEventLoop.h>
#include <SharedBuffer.h>

#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

ASClientConnection::ASClientConnection(CLocalSocket& client_socket, int client_id, ASMixer& mixer)
    : Connection(client_socket, client_id)
    , m_mixer(mixer)
{
}

ASClientConnection::~ASClientConnection()
{
}

void ASClientConnection::send_greeting()
{
    ASAPI_ServerMessage message;
    message.type = ASAPI_ServerMessage::Type::Greeting;
    message.greeting.server_pid = getpid();
    message.greeting.your_client_id = client_id();
    post_message(message);
}

bool ASClientConnection::handle_message(const ASAPI_ClientMessage& message, const ByteBuffer&&)
{
    switch (message.type) {
    case ASAPI_ClientMessage::Type::Greeting:
        set_client_pid(message.greeting.client_pid);
        break;
    case ASAPI_ClientMessage::Type::PlayBuffer: {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.play_buffer.buffer_id);
        if (!shared_buffer) {
            did_misbehave();
            return false;
        }

        // we no longer need the buffer, so acknowledge that it's playing
        ASAPI_ServerMessage reply;
        reply.type = ASAPI_ServerMessage::Type::PlayingBuffer;
        reply.playing_buffer.buffer_id = message.play_buffer.buffer_id;
        post_message(reply);

        m_mixer.queue(*this, ABuffer::create_with_shared_buffer(*shared_buffer));
        break;
    }
    case ASAPI_ClientMessage::Type::EnqueueBuffer: {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.play_buffer.buffer_id);
        if (!shared_buffer) {
            did_misbehave();
            return false;
        }

        static const int max_in_queue = 2;

        ASAPI_ServerMessage reply;
        reply.type = ASAPI_ServerMessage::Type::EnqueueBufferResponse;
        reply.playing_buffer.buffer_id = message.play_buffer.buffer_id;
        if (m_buffer_queue.size() >= max_in_queue) {
            reply.success = false;
        } else {
            m_buffer_queue.enqueue(ABuffer::create_with_shared_buffer(*shared_buffer));
        }
        post_message(reply);

        if (m_playing_queued_buffer_id == -1)
            play_next_in_queue();

        break;
    }
    case ASAPI_ClientMessage::Type::Invalid:
    default:
        dbgprintf("ASClientConnection: Unexpected message ID %d\n", int(message.type));
        did_misbehave();
    }

    return true;
}

void ASClientConnection::did_finish_playing_buffer(Badge<ASMixer>, int buffer_id)
{
    if (m_playing_queued_buffer_id == buffer_id)
        play_next_in_queue();

    ASAPI_ServerMessage reply;
    reply.type = ASAPI_ServerMessage::Type::FinishedPlayingBuffer;
    reply.playing_buffer.buffer_id = buffer_id;
    post_message(reply);
}

void ASClientConnection::play_next_in_queue()
{
    dbg() << "Playing next in queue (" << m_buffer_queue.size() << " queued)";
    auto buffer = m_buffer_queue.dequeue();
    m_playing_queued_buffer_id = buffer->shared_buffer_id();
    m_mixer.queue(*this, move(buffer));
}
